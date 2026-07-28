#!/usr/bin/env python3
"""Retest G3-H01: activate prelim on seal-gd3-prelim-open (idempotent 200)."""
import json
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"


def request(method: str, path: str, token: str | None = None, body: dict | None = None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = json.loads(e.read().decode()) if e.fp else {}
        return e.code, raw


def main():
    _, login = request("POST", "/auth/login", body={"email": "coord@fpt.edu.vn", "password": "Coordinator@dev1"})
    token = login["data"]["accessToken"]

    _, hacks = request("GET", "/hackathons?size=200", token)
    items = hacks.get("data", [])
    if isinstance(items, dict):
        items = items.get("items", [])
    h3 = next(x for x in items if x.get("slug") == "seal-gd3-prelim-open")

    _, rounds_raw = request("GET", f"/hackathons/{h3['id']}/rounds", token)
    rounds = rounds_raw.get("data", [])
    if isinstance(rounds, dict):
        rounds = rounds.get("items", [])
    prelim = next(r for r in rounds if not r.get("isFinal"))

    status, body = request("PATCH", f"/rounds/{prelim['id']}/activate", token, {"note": "g3-h01-retest"})
    code = (body.get("error") or {}).get("code", "")
    ok = status == 200
    print(f"G3-H01 PATCH /rounds/{prelim['id']}/activate -> HTTP {status} code={code}")
    print("PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
