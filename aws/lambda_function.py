import json
import os
import openai
import firebase_admin
from firebase_admin import auth, credentials

import urllib.request
import urllib.error

# Initialize Firebase Admin SDK once
if not firebase_admin._apps:
    cred = credentials.Certificate(json.loads(os.environ["FIREBASE_SERVICE_ACCOUNT"]))
    firebase_admin.initialize_app(cred)

openai.api_key = os.environ["OPEN_AI_KEY"]
# client = OpenAI(api_key=os.environ["OPEN_AI_KEY"])


def lambda_handler(event, context):
    try:
        # Check Authorization header
        headers = event.get("headers", {}) or {}
        headers = {k.lower(): v for k, v in headers.items()}
        auth_header = headers.get("authorization", "")
        print("Auth Header:", auth_header)
        
        if not auth_header.startswith("Bearer "):
            return {"statusCode": 401, "body": json.dumps({"error": "Missing or invalid token"})}

        id_token = auth_header.split("Bearer ")[1]

        # Verify Firebase token
        try:
            decoded_token = auth.verify_id_token(id_token)
            user_uid = decoded_token["uid"]
            print("Authenticated user UID:", user_uid)
        except Exception as e:
            # Token is invalid or expired
            return {"statusCode": 401, "body": json.dumps({"error": "Invalid or expired token"})}

        # Call consumeAtom (Firebase Cloud Function)
        consume_url = os.environ.get("CONSUME_ATOM_URL")
        consume_secret = os.environ.get("CONSUME_ATOM_SECRET")
        remaining_atoms = None

        if consume_url:
            try:
                # Prepare payload for Firebase Callable Function (must be wrapped in "data")
                # If using Shared Secret, we pass userId explicitly.
                # If using ID Token forwarding, userId is effectively ignored by generic onCall but we pass it anyway.
                payload_data = {
                    "userId": user_uid,
                    "deductionId": context.aws_request_id if context else "test-invocation"
                }
                
                # The onCall protocol expects a JSON body with a "data" key
                body = {"data": payload_data}
                data = json.dumps(body).encode("utf-8")
                
                req_headers = {"Content-Type": "application/json"}
                
                # Auth Strategy: Shared Secret (Preferred for Backend-to-Backend) or ID Token Forwarding
                if consume_secret:
                    req_headers["Authorization"] = f"Bearer {consume_secret}"
                elif auth_header:
                    req_headers["Authorization"] = auth_header # Forward the user's Bearer token
                
                req = urllib.request.Request(consume_url, data=data, headers=req_headers, method="POST")
                
                with urllib.request.urlopen(req, timeout=10) as response:
                    if 200 <= response.status < 300:
                        response_body = json.loads(response.read().decode("utf-8"))
                        # Firebase Callable Functions return data wrapped in a "result" key
                        result = response_body.get("result")
                        
                        if not result or not result.get("success"):
                            print(f"Atom deduction failed for {user_uid}: {result}")
                            return {
                                "statusCode": 402,
                                "body": json.dumps({
                                    "error": "Atom limit exceeded",
                                    "details": result.get("message", "Insufficient atoms") if result else "Invalid response from Atom Service"
                                })
                            }
                        remaining_atoms = result.get("remainingAtoms")
                        print(f"Atom deduction successful. Remaining: {remaining_atoms}")
                    else:
                        print(f"consumeAtom returned status {response.status}")
                        return {"statusCode": 502, "body": json.dumps({"error": "Upstream error (Atom Service)"})}
            
            except Exception as e:
                print(f"Error calling consumeAtom: {e}")
                # Constraint: Do NOT call OpenAI if atom deduction fails (or we can't verify)
                return {"statusCode": 500, "body": json.dumps({"error": "Failed to verify atom balance", "details": str(e)})}
        else:
             print("Warning: CONSUME_ATOM_URL not set. Skipping deduction (Dev Mode).")
             # Strict requirement: "Before calling OpenAI, the Lambda must call Firebase Cloud Function consumeAtom()"
             # If URL is missing, we must fail if we are strictly following requirements, but usually it's better to log error.
             # However, prompt says "Modify... that ... Calls consumeAtom". If I skip, I violate requirement.
             # I will treat it as error if missing, but for safety in existing devs environments I will log.
             # Re-reading: "Requirement: Before calling OpenAI, the Lambda must call..."
             # I will return error if not configured.
             return {"statusCode": 500, "body": json.dumps({"error": "Server configuration error: CONSUME_ATOM_URL missing"})}
        
        # Print to CloudWatch logs
        print("Value of user_uid:", user_uid)

        # Parse request body
        params = event
        if 'body' in event and event['body'] is not None:
            params = json.loads(event['body'])
        user_message = params.get('message')

        # Call OpenAI
        aiContent = """You are a helpful and friendly AI assistant that responds to chat messages on behalf of the device owner ONLY when the owner is busy according to their schedule.

Guidelines:
- If the owner is busy, craft a natural, short, and friendly response as if sent by the owner. You may politely mention they are occupied but avoid being robotic.
- If you know what the owner might be doing (e.g., in a meeting, driving, at the gym), include that context naturally if appropriate.
- Keep responses conversational and human-like. Avoid over-explaining.
- Do NOT reveal you are an AI.
- If the schedule indicates the owner is NOT busy, return an empty response or do not reply."""



        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": aiContent},
                {"role": "user", "content": user_message}
            ],
            max_tokens=200
        )

        ai_reply = response.choices[0].message.content

        return {
            "statusCode": 200,
            "body": json.dumps({
                "reply": ai_reply,
                "remainingAtoms": remaining_atoms
            })
        }

    except Exception as e:
        return {
            "statusCode": 401,
            "body": json.dumps({"error": str(e)})
        }
