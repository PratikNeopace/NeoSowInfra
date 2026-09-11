import urllib.request
import json

# 1. Login
data = json.dumps({"email": "superadmin@neosowinfra.com", "password": "SuperAdmin@123"}).encode("utf-8")
req = urllib.request.Request("http://localhost:8080/api/v1/auth/login", data=data, headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(req) as response:
        res = json.loads(response.read().decode())
        token = res["accessToken"]
except urllib.error.HTTPError as e:
    print("Login failed:", e.read().decode())
    exit(1)

# 2. Get user id of pratik.g@neosowinfra.com (or just update the first one)
req = urllib.request.Request("http://localhost:8080/api/v1/admin/users", headers={"Authorization": "Bearer " + token})
try:
    with urllib.request.urlopen(req) as response:
        users = json.loads(response.read().decode())["content"]
        target_user = [u for u in users if u["email"] != "superadmin@neosowinfra.com"]
        if target_user:
            user_id = target_user[0]["id"]
        else:
            user_id = users[0]["id"]
except urllib.error.HTTPError as e:
    print("Get users failed:", e.read().decode())
    exit(1)

# 3. Update user
payload = json.dumps({"email":"pratik.g@neosowinfra.com","roles":["USER"],"enabled":True,"password":"Neopace@2026"}).encode("utf-8")
req = urllib.request.Request(f"http://localhost:8080/api/v1/admin/users/{user_id}", data=payload, headers={"Content-Type": "application/json", "Authorization": "Bearer " + token}, method="PUT")
try:
    with urllib.request.urlopen(req) as response:
        print("Success:", response.read().decode())
except urllib.error.HTTPError as e:
    print("Update failed with status:", e.code)
    print("Response:", e.read().decode())
