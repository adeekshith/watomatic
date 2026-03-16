import json
import logging
import os

import urllib3
import firebase_admin
from firebase_admin import auth, credentials

# ─── LOGGING ───────────────────────────────────────────────
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# ─── MODULE-LEVEL INIT (runs once per cold start) ─────────
_firebase_cred = credentials.Certificate(
    json.loads(os.environ["FIREBASE_SERVICE_ACCOUNT"])
)
firebase_admin.initialize_app(_firebase_cred)

_http = urllib3.PoolManager(num_pools=2, maxsize=4)

OPEN_AI_KEY = os.environ["OPEN_AI_KEY"]
CONSUME_ATOM_URL = os.environ["CONSUME_ATOM_URL"]
CONSUME_ATOM_SECRET = os.environ["CONSUME_ATOM_SECRET"]

OPENAI_URL = "https://api.openai.com/v1/responses"

OPENAI_HEADERS = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {OPEN_AI_KEY}",
}

OPENAI_BASE_PAYLOAD = {
    "model": "gpt-5-nano",
    "reasoning": {"effort": "minimal"},
    "max_output_tokens": 500,
    "store": False,
    "parallel_tool_calls": False,
    "text": {"verbosity": "low"},
}

DEFAULT_SYSTEM_PROMPT = (
    "You are a helpful and friendly AI assistant that responds to chat "
    "messages on behalf of the device owner ONLY when the owner is busy. "
    "Keep responses short, natural, and human-like. "
    "Never reveal you are AI."
)


# ─── HELPERS ───────────────────────────────────────────────
def _json_response(status_code, body):
    return {
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(body),
    }


def _post_json(url, body, headers, timeout=10):
    resp = _http.request(
        "POST",
        url,
        body=json.dumps(body).encode("utf-8"),
        headers=headers,
        timeout=timeout,
    )

    data = resp.data.decode("utf-8")

    try:
        result = json.loads(data)
    except Exception:
        logger.error("Invalid JSON response: %s", data)
        raise

    if resp.status >= 400:
        logger.error("HTTP %d: %s", resp.status, data)
        raise Exception(f"HTTP {resp.status}: {data}")

    return resp.status, result


# ─── HANDLER ───────────────────────────────────────────────
def lambda_handler(event, context):
    try:
        # ─── 1. AUTH ───────────────────────────────────────
        headers = event.get("headers") or {}
        auth_header = {k.lower(): v for k, v in headers.items()}.get("authorization", "")

        if not auth_header.startswith("Bearer "):
            return _json_response(401, {"success": False, "error": "Missing Bearer token"})

        id_token = auth_header.split("Bearer ", 1)[1]

        try:
            decoded = auth.verify_id_token(id_token)
            user_uid = decoded["uid"]
            logger.info("Authenticated UID: %s", user_uid)
        except Exception as e:
            logger.warning("Firebase token invalid: %s", e)
            return _json_response(401, {"success": False, "error": "Invalid Firebase token"})

        # ─── 2. PARSE BODY ─────────────────────────────────
        body = json.loads(event.get("body", "{}"))
        user_message = body.get("message", "")
        custom_system_prompt = body.get("custom_prompt", "")

        if not user_message:
            return _json_response(400, {"success": False, "error": "message required"})

        # ─── 3. CONSUME ATOM ──────────────────────────────
        status, atom_response = _post_json(
            CONSUME_ATOM_URL,
            {"userId": user_uid, "deductionId": context.aws_request_id},
            {
                "Content-Type": "application/json",
                "Authorization": f"Bearer {CONSUME_ATOM_SECRET}",
            },
        )

        if not atom_response or not atom_response.get("success"):
            return _json_response(
                402,
                {
                    "success": False,
                    "error": "Atom limit exceeded",
                    "details": (atom_response or {}).get("message", "Unknown"),
                },
            )

        remaining_atoms = atom_response.get("remainingAtoms")

        # ─── 4. CALL OPENAI ───────────────────────────────
        system_prompt = custom_system_prompt or DEFAULT_SYSTEM_PROMPT

        openai_payload = {
            **OPENAI_BASE_PAYLOAD,
            "input": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message},
            ],
        }

        status, openai_resp = _post_json(
            OPENAI_URL,
            openai_payload,
            OPENAI_HEADERS,
            timeout=25,
        )

        if status != 200:
            raise Exception(f"OpenAI API error: {openai_resp}")

        ai_reply = ""

        if "output" in openai_resp and isinstance(openai_resp["output"], list):
            for item in openai_resp["output"]:
                if item.get("type") == "message":
                    for c in item.get("content", []):
                        if c.get("type") == "output_text":
                            ai_reply += c.get("text", "")
        else:
            raise Exception(f"Unexpected response format from OpenAI API: {openai_resp}")

        # ─── 5. SUCCESS ───────────────────────────────────
        return _json_response(
            200,
            {
                "success": True,
                "reply": ai_reply,
                "remainingAtoms": remaining_atoms,
            },
        )

    except Exception as e:
        logger.error("FATAL ERROR: %s", e, exc_info=True)
        return _json_response(
            500,
            {
                "success": False,
                "error": "Internal server error",
                "details": str(e),
            },
        )