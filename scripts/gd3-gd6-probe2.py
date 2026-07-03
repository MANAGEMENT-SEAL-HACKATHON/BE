import json, urllib.request, urllib.error

BASE = "http://localhost:8080/api/v1"

def req(m, p, t=None, b=None):
    data = json.dumps(b).encode() if b else None
    h = {"Content-Type": "application/json"}
    if t: h["Authorization"] = f"Bearer {t}"
    r = urllib.request.Request(f"{BASE}{p}", data=data, headers=h, method=m)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

login = req("POST", "/auth/login", b={"email": "coord@fpt.edu.vn", "password": "Coordinator@dev1"})
tok = login[1]["data"]["accessToken"]
raw = req("GET", "/hackathons?size=200", tok)[1]
hacks = raw.get("data") or raw
if isinstance(hacks, dict) and "items" in hacks:
    hacks = hacks["items"]

# G4 tiebreak after publish
h4tb = next(x for x in hacks if x["slug"] == "seal-gd4-tiebreak-gate")
rounds = req("GET", f"/hackathons/{h4tb['id']}/rounds", tok)[1]["data"]
p = next(r for r in rounds if not r.get("isFinal"))
print("tiebreak-gate published?", p.get("isPublished"), "locked?", p.get("scoringLocked"))
if not p.get("isPublished"):
    req("PATCH", f"/rounds/{p['id']}/publish", tok)
adv = req("POST", f"/rounds/{p['id']}/advance", tok, {"advancedTeamIds": [1]})
print("advance after publish:", adv[0], adv[1].get("error", {}).get("code"))

# G5
h5 = next(x for x in hacks if x["slug"] == "seal-gd5-final-active")
r5 = req("GET", f"/hackathons/{h5['id']}/rounds", tok)[1]["data"]
final = next(r for r in r5 if r.get("isFinal"))
print("G5 final active", final.get("isActive"), "status", h5["status"])
rank = req("GET", f"/rounds/{final['id']}/ranking/preview", tok)
print("G5 rank", rank[0], "items", len(rank[1].get("data") or []))

# Export correct endpoint
h6fin = next(x for x in hacks if x["slug"] == "seal-gd6-finished-export")
exp = req("POST", f"/hackathons/{h6fin['id']}/export-jobs", tok, {"type": "CSV_RANKINGS"})
print("export correct path", exp[0], exp[1].get("data", {}).get("status") if exp[0] < 300 else exp[1].get("error", {}).get("code"))

# G3 activate error body
h3 = next(x for x in hacks if x["slug"] == "seal-gd3-prelim-open")
r3 = req("GET", f"/hackathons/{h3['id']}/rounds", tok)[1]["data"]
prelim = next(r for r in r3 if not r.get("isFinal"))
act = req("PATCH", f"/rounds/{prelim['id']}/activate", tok)
print("G3 activate", act[0], act[1].get("error", {}))

# Scoreboard public
h4pub = next(x for x in hacks if x["slug"] == "seal-gd4-published")
rp = req("GET", f"/hackathons/{h4pub['id']}/rounds", tok)[1]["data"]
pp = next(r for r in rp if not r.get("isFinal"))
try:
    sb = urllib.request.urlopen(f"{BASE}/rounds/{pp['id']}/scoreboard", timeout=10)
    print("scoreboard", sb.status)
except urllib.error.HTTPError as e:
    print("scoreboard", e.code)
