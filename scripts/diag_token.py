import json, urllib.request, subprocess

MYSQL = "D:\\tools\\mysql\\bin\\mysql.exe"

def db_query(sql):
    out = subprocess.run([MYSQL, "-uroot", "-proot", "overtime_db", "-Nse", sql],
                         capture_output=True, text=True)
    return out.stdout.strip()

req = urllib.request.Request('http://localhost:8080/api/auth/login', method='POST')
req.add_header('Content-Type', 'application/json')
req.data = json.dumps({'username': 'admin', 'password': 'Admin@123456'}).encode()
with urllib.request.urlopen(req, timeout=10) as r:
    b = json.loads(r.read().decode())
tok = b['data']['token']
print(f'API returned token: {tok}')

db_tok = db_query("SELECT session_token FROM sys_user WHERE username='admin';")
print(f'DB has token:       {db_tok}')
print('MATCH:', tok == db_tok)

req = urllib.request.Request('http://localhost:8080/api/auth/me', method='GET')
req.add_header('X-Auth-Token', tok)
with urllib.request.urlopen(req, timeout=10) as r:
    me = json.loads(r.read().decode())
print('me:', me.get('code'), me.get('message'))

# also hit accounts with the SAME token
req = urllib.request.Request('http://localhost:8080/api/accounts', method='GET')
req.add_header('X-Auth-Token', tok)
with urllib.request.urlopen(req, timeout=10) as r:
    ac = json.loads(r.read().decode())
print('accounts:', ac.get('code'), len(ac.get('data') or []))
