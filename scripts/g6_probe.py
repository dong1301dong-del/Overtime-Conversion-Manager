#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""G6 四角色权限矩阵自动验收探针。
判定依据：响应体 JSON 的 code 字段（本后端统一返回 HTTP 200 + body.code）。
  - 未登录/伪造 token -> code 401
  - 角色不符 -> code 1, message "无权限执行此操作"
  - 通过校验 -> code 0
写操作允许的校验用“已存在/不存在的ID”触发业务校验而非鉴权校验，
从而证明角色闸口已打开且不产生脏数据。
"""
import json
import urllib.request
import urllib.parse
import urllib.error

BASE = "http://localhost:8080"
DENY_MSG = "无权限执行此操作"

# ---------- HTTP helpers ----------
def _call(method, path, token=None, body=None):
    url = BASE + path
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("X-Auth-Token", token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            raw = r.read().decode("utf-8")
            return r.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"code": e.code, "message": raw[:200]}
    except Exception as e:
        return -1, {"code": -1, "message": str(e)}

def get(path, token=None):
    return _call("GET", path, token=token)

def post(path, token=None, body=None):
    return _call("POST", path, token=token, body=body)

def code_of(res):
    return res[1].get("code") if isinstance(res[1], dict) else None

def msg_of(res):
    return res[1].get("message") if isinstance(res[1], dict) else None

# ---------- Accounts setup ----------
def setup_accounts():
    admin = login("admin", "Admin@123456")
    atoken = admin["token"]
    accts = get("/api/accounts", atoken)[1].get("data", [])
    amap = {a["username"]: a for a in accts}

    def ensure(role_user, role, pwd):
        if role_user in amap:
            uid = amap[role_user]["id"]
            rp = post(f"/api/accounts/{uid}/reset-password", atoken,
                      {"newPassword": pwd})
            print(f"  reset {role_user}: code={rp[1].get('code')} msg={rp[1].get('message')}")
        else:
            cr = post("/api/accounts", atoken,
                      {"username": role_user, "role": role, "password": pwd, "mustChangePwd": False})
            print(f"  create {role_user}: code={cr[1].get('code')} msg={cr[1].get('message')}")
        return login(role_user, pwd)

    clerk = ensure("clerk1", "CLERK", "Clerk@123456")
    ro = ensure("ro1", "READONLY", "Ro@123456")
    # employee: 自动由成员创建，默认 Abc_123456
    emp_login = None
    if "emp1" in amap:
        uid = amap["emp1"]["id"]
        rp = post(f"/api/accounts/{uid}/reset-password", atoken, {"newPassword": "Emp@123456"})
        print(f"  reset emp1: code={rp[1].get('code')} msg={rp[1].get('message')}")
        emp_login = login("emp1", "Emp@123456")
    else:
        # 创建成员会同步创建 EMPLOYEE 账号
        mb = post("/api/members", atoken,
                  {"name": "emp1", "username": "emp1", "department": "G6"})
        if mb[1].get("code") == 0:
            emp_login = login("emp1", "Abc_123456")
    return {
        "ADMIN": admin["token"],
        "CLERK": clerk["token"],
        "READONLY": ro["token"],
        "EMPLOYEE": emp_login["token"] if emp_login else None,
    }

def login(username, password):
    res = post("/api/auth/login", body={"username": username, "password": password})
    if res[1].get("code") == 0:
        return res[1]["data"]
    raise RuntimeError(f"login failed for {username}: {res[1]}")

# ---------- Test matrix ----------
def run():
    T = setup_accounts()
    R = {}  # role -> token
    for k, v in T.items():
        if v is None:
            raise RuntimeError(f"missing token for {k}")
        R[k] = v

    cases = []  # (name, method, path, token_role_or_special, expect)

    # 1) 未登录 / 伪造 token -> 401
    cases.append(("no-token GET /api/members", "GET", "/api/members", "__none__", "unauth"))
    cases.append(("fake-token GET /api/members", "GET", "/api/members", "__fake__", "unauth"))

    # 2) ADMIN 专属：/api/accounts
    cases.append(("ADMIN  GET /api/accounts", "GET", "/api/accounts", "ADMIN", "granted"))
    cases.append(("CLERK  GET /api/accounts", "GET", "/api/accounts", "CLERK", "denied"))
    cases.append(("READONLY GET /api/accounts", "GET", "/api/accounts", "READONLY", "denied"))
    cases.append(("EMPLOYEE GET /api/accounts", "GET", "/api/accounts", "EMPLOYEE", "denied"))

    # 3) 读：ADMIN/CLERK/READONLY 共享
    for ep in ["/api/members",
               "/api/overtime/month?month=2026-08",
               "/api/comp-usage",
               "/api/dashboard",
               "/api/balance/all",
               "/api/config"]:
        for role in ["ADMIN", "CLERK", "READONLY"]:
            cases.append((f"{role} GET {ep}", "GET", ep, role, "granted"))
        cases.append((f"EMPLOYEE GET {ep}", "GET", ep, "EMPLOYEE", "denied"))

    # 4) EMPLOYEE 专属：/api/employee/me
    cases.append(("EMPLOYEE GET /api/employee/me", "GET", "/api/employee/me", "EMPLOYEE", "granted"))
    cases.append(("ADMIN GET /api/employee/me", "GET", "/api/employee/me", "ADMIN", "denied"))
    cases.append(("CLERK GET /api/employee/me", "GET", "/api/employee/me", "CLERK", "denied"))
    cases.append(("READONLY GET /api/employee/me", "GET", "/api/employee/me", "READONLY", "denied"))

    # 5) 写：仅 ADMIN/CLERK；READONLY/EMPLOYEE 应被拒
    cases.append(("READONLY POST /api/members", "POST", "/api/members", "READONLY", "denied", {"name": "x", "username": "x", "department": "x"}))
    cases.append(("EMPLOYEE POST /api/members", "POST", "/api/members", "EMPLOYEE", "denied", {"name": "x", "username": "x", "department": "x"}))
    cases.append(("READONLY POST /api/comp-usage", "POST", "/api/comp-usage", "READONLY", "denied", {"memberId": 999999}))
    cases.append(("EMPLOYEE POST /api/comp-usage", "POST", "/api/comp-usage", "EMPLOYEE", "denied", {"memberId": 999999}))
    cases.append(("READONLY POST /api/overtime", "POST", "/api/overtime", "READONLY", "denied", {"memberId": 999999}))
    cases.append(("EMPLOYEE POST /api/overtime", "POST", "/api/overtime", "EMPLOYEE", "denied", {"memberId": 999999}))

    # 6) 允许的写：CLERK 角色闸口应打开（用已存在/不存在ID触发业务校验，不落脏数据）
    cases.append(("CLERK POST /api/members(gate)", "POST", "/api/members", "CLERK",
                  "not_denied", {"name": "probe", "username": "admin", "department": "x"}))
    cases.append(("CLERK POST /api/comp-usage(gate)", "POST", "/api/comp-usage", "CLERK",
                  "not_denied", {"memberId": 999999}))

    # run
    results = []
    for c in cases:
        name, method, path, role, expect = c[0], c[1], c[2], c[3], c[4]
        body = c[5] if len(c) > 5 else None
        if role == "__none__":
            res = _call(method, path)  # no token
        elif role == "__fake__":
            res = _call(method, path, token="fake-token-xyz")
        else:
            res = _call(method, path, token=R[role], body=body)
        code = code_of(res)
        msg = msg_of(res)

        ok = False
        if expect == "unauth":
            ok = (code == 401)
        elif expect == "denied":
            ok = (code == 1 and msg == DENY_MSG)
        elif expect == "granted":
            ok = (code == 0)
        elif expect == "not_denied":
            ok = (msg != DENY_MSG)  # 闸口打开（可能是业务校验失败，但不是“无权限”）

        results.append((name, ok, code, msg))
        print(f"[{'PASS' if ok else 'FAIL'}] {name} -> code={code} msg={msg}")

    passed = sum(1 for r in results if r[1])
    total = len(results)
    print("\n==== G6 SUMMARY ====")
    print(f"PASS {passed}/{total}")
    if passed != total:
        print("FAILED CASES:")
        for r in results:
            if not r[1]:
                print(f"  - {r[0]} (code={r[2]}, msg={r[3]})")
    return passed, total

if __name__ == "__main__":
    run()
