import json
import os
import urllib.request
import urllib.error
import openai
import firebase_admin
from firebase_admin import auth, credentials

# -------------------------------
# DEFERRED INIT FIREBASE ADMIN
# -------------------------------
def init_firebase_once():
    if not firebase_admin._apps:
        cred = credentials.Certificate(
            json.loads(os.environ["FIREBASE_SERVICE_ACCOUNT"])
        )
        firebase_admin.initialize_app(cred)

def init_openai_once():
    openai.api_key = os.environ["OPEN_AI_KEY"]
# -------------------------------
# SAFE HTTP CALL HELPER
# -------------------------------
def post_json(url, body, headers, timeout=10):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")

    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode())

    except urllib.error.HTTPError as e:
        # ⭐ IMPORTANT → print real Firebase error body
        error_body = e.read().decode()
        print("HTTP ERROR:", e.code, error_body)
        raise Exception(f"HTTP {e.code}: {error_body}")

    except Exception as e:
        print("NETWORK ERROR:", str(e))
        raise


# -------------------------------
# MAIN HANDLER
# -------------------------------
def lambda_handler(event, context):

    try:
        # -------------------------------
        # 0️⃣ INIT GLOBALS SAFELY
        # -------------------------------
        init_firebase_once()
        init_openai_once()

        # -------------------------------
        # 1️⃣ AUTH HEADER CHECK
        # -------------------------------
        headers = event.get("headers", {}) or {}
        headers = {k.lower(): v for k, v in headers.items()}

        auth_header = headers.get("authorization", "")

        if not auth_header.startswith("Bearer "):
            return {
                "statusCode": 401,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({"success": False, "error": "Missing Bearer token"})
            }

        id_token = auth_header.split("Bearer ")[1]

        # -------------------------------
        # 2️⃣ VERIFY FIREBASE USER TOKEN
        # -------------------------------
        try:
            decoded = auth.verify_id_token(id_token)
            user_uid = decoded["uid"]
            print("Authenticated UID:", user_uid)

        except Exception as e:
            print("Firebase token invalid:", str(e))
            return {
                "statusCode": 401,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({"success": False, "error": "Invalid Firebase token"})
            }

        # -------------------------------
        # 3️⃣ CALL CONSUME ATOM (SECURE)
        # -------------------------------
        consume_url = os.environ.get("CONSUME_ATOM_URL")
        consume_secret = os.environ.get("CONSUME_ATOM_SECRET")

        if not consume_url:
            return {
                "statusCode": 500,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({"success": False, "error": "CONSUME_ATOM_URL missing"})
            }

        # ⭐ ALWAYS USE SHARED SECRET FOR BACKEND CALL
        if not consume_secret:
            return {
                "statusCode": 500,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({"success": False, "error": "CONSUME_ATOM_SECRET missing"})
            }

        atom_payload = {
            "userId": user_uid,
            "deductionId": context.aws_request_id
        }

        atom_headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {consume_secret}"
        }

        status, atom_response = post_json(
            consume_url,
            atom_payload,
            atom_headers
        )

        result = atom_response  # Firebase directly returns {success: true...}

        if not result or not result.get("success"):
            return {
                "statusCode": 402,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({
                    "success": False,
                    "error": "Atom limit exceeded",
                    "details": result.get("message", "Unknown") if result else "Unknown"
                })
            }

        remaining_atoms = result.get("remainingAtoms")

        print("Remaining atoms:", remaining_atoms)

        # -------------------------------
        # 4️⃣ PARSE REQUEST BODY
        # -------------------------------
        body = json.loads(event.get("body", "{}"))
        user_message = body.get("message", "")
        custom_system_prompt = body.get("custom_prompt", "")

        if not user_message:
            return {
                "statusCode": 400,
                "headers": {"Content-Type": "application/json"},
                "body": json.dumps({"success": False, "error": "message required"})
            }

        # -------------------------------
        # 5️⃣ CALL OPENAI
        # -------------------------------
        system_prompt = """
            You are a helpful and friendly AI assistant that responds to chat
            messages on behalf of the device owner ONLY when the owner is busy.
            
            Keep responses short, natural, and human-like.
            Never reveal you are AI.
            """

        if custom_system_prompt:
            system_prompt = custom_system_prompt

        response = openai.ChatCompletion.create(
            model="gpt-5-nano",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message}
            ],
            verbosity="low",
            timeout=15
        )

        ai_reply = response.choices[0].message.content

        # -------------------------------
        # 6️⃣ SUCCESS RESPONSE
        # -------------------------------
        return {
            "statusCode": 200,
            "headers": {"Content-Type": "application/json"},
            "body": json.dumps({
                "success": True,
                "reply": ai_reply,
                "remainingAtoms": remaining_atoms
            })
        }

    # -------------------------------
    # GLOBAL SAFETY
    # -------------------------------
    except Exception as e:
        print("FATAL ERROR:", str(e))
        return {
            "statusCode": 500,
            "headers": {"Content-Type": "application/json"},
            "body": json.dumps({
                "success": False,
                "error": "Internal server error",
                "details": str(e)
            })
        }