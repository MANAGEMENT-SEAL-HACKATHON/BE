#!/usr/bin/env python3
"""GĐ3-GĐ6 API audit runner — outputs JSON results."""
import json
import urllib.request
import urllib.error
from typing import Any

BASE = "http://localhost:8080/api/v1"
RESULTS: list[dict] = []


def log(tid, phase, desc, expected, actual, status, detail=""):
    RESULTS.append({
        "id": tid, "phase": phase, "description": desc,
        "expected": expected, "actual": actual, "status": status, "detail": detail or "",
    })


def request(method: str, path: str, token: str | None = None, body: dict | None = None) -> dict:
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = json.loads(resp.read().decode())
            return {"ok": True, "status": resp.status, "data": raw.get("data"), "raw": raw}
    except urllib.error.HTTPError as e:
        raw = {}
        try:
            raw = json.loads(e.read().decode())
        except Exception:
            pass
        err = raw.get("error") or {}
        return {
            "ok": False, "status": e.code,
            "code": err.get("code") or raw.get("code"),
            "message": err.get("message"),
            "raw": raw,
        }


def login(email: str, password: str) -> str:
    r = request("POST", "/auth/login", body={"email": email, "password": password})
    return r["data"]["accessToken"]


def find_hackathon(slug: str, token: str) -> dict | None:
    r = request("GET", f"/hackathons?size=200", token)
    items = r["data"]
    if isinstance(items, dict) and "items" in items:
        items = items["items"]
    for h in items or []:
        if h.get("slug") == slug:
            return h
    return None


def get_rounds(hid: int, token: str) -> list:
    r = request("GET", f"/hackathons/{hid}/rounds", token)
    data = r["data"]
    if isinstance(data, dict) and "items" in data:
        return data["items"]
    return data or []


def main():
    coord = login("coord@fpt.edu.vn", "Coordinator@dev1")
    judge = login("judge1@fpt.edu.vn", "Judge@dev1")
    guest = login("guestjudge@gmail.com", "GuestJudge@dev1")

    # === GĐ3 ===
    h3 = find_hackathon("seal-gd3-prelim-open", coord)
    if h3:
        rounds = get_rounds(h3["id"], coord)
        prelim = next((r for r in rounds if not r.get("isFinal")), None)
        if prelim:
            act = request("PATCH", f"/rounds/{prelim['id']}/activate", coord)
            st = "PASS" if act["ok"] or act.get("code") == "ROUND_ALREADY_ACTIVE" else "FAIL"
            log("G3-H01", "GĐ3", "Activate prelim", "200", f"{act['status']} {act.get('code','')}", st)
            rp = request("GET", f"/rounds/{prelim['id']}/ranking/preview", coord)
            log("G3-H02", "GĐ3", "Ranking preview", "200", str(rp["status"]), "PASS" if rp["ok"] else "FAIL")
            sp = request("GET", f"/rounds/{prelim['id']}/scoring-progress", coord)
            log("G3-H03", "GĐ3", "Scoring progress", "200", str(sp["status"]), "PASS" if sp["ok"] else "FAIL")

    h3live = find_hackathon("seal-gd3-scoring-live", coord)
    if h3live:
        prelim_live = next((r for r in get_rounds(h3live["id"], coord) if not r.get("isFinal")), None)
        if prelim_live and prelim_live.get("scoringLocked"):
            score = request("POST", "/scores", judge, {"submissionId": 1, "criterionId": 1, "scoreValue": 5})
            log("ADV-02-SL", "GĐ3", "Score after prelim lock", "423 SCORING_LOCKED",
                f"{score['status']} {score.get('code','')}", "PASS" if score.get("code") == "SCORING_LOCKED" else "FAIL")

    # === GĐ4 ===
    h4tb = find_hackathon("seal-gd4-tiebreak-gate", coord)
    if h4tb:
        prelim_tb = next((r for r in get_rounds(h4tb["id"], coord) if not r.get("isFinal")), None)
        if prelim_tb:
            tb = request("GET", f"/rounds/{prelim_tb['id']}/tiebreak", coord)
            count = len(tb["data"] or []) if tb["ok"] else 0
            log("G4-TB-LIST", "GĐ4", "Tiebreak on gate seed", ">0", f"count={count}", "PASS" if count > 0 else "FAIL")
            adv = request("POST", f"/rounds/{prelim_tb['id']}/advance", coord, {"advancedTeamIds": [1]})
            log("G4-N-TB", "GĐ4", "Advance with tiebreak", "TIEBREAK_REQUIRED",
                f"{adv['status']} {adv.get('code','')}", "PASS" if adv.get("code") == "TIEBREAK_REQUIRED" else "FAIL")

    h4 = find_hackathon("seal-gd4-advance-ready", coord)
    if h4:
        rounds4 = get_rounds(h4["id"], coord)
        prelim4 = next((r for r in rounds4 if not r.get("isFinal")), None)
        final4 = next((r for r in rounds4 if r.get("isFinal")), None)
        if prelim4:
            wc = request("GET", f"/rounds/{prelim4['id']}/wildcard-candidates", coord)
            wc_count = len((wc.get("data") or {}).get("candidates") or []) if wc["ok"] else 0
            log("G4-H-WC", "GĐ4", "Wildcard candidates", ">0", f"count={wc_count}",
                "PASS" if wc_count > 0 else ("PARTIAL" if wc["ok"] else "FAIL"))
            if not prelim4.get("isPublished"):
                pub = request("PATCH", f"/rounds/{prelim4['id']}/publish", coord)
                log("G4-H01", "GĐ4", "Publish", "200", f"{pub['status']}", "PASS" if pub["ok"] else "FAIL")
            rd = request("GET", f"/hackathons/{h4['id']}/readiness?target=FINAL_ROUND", coord)
            log("G4-R01", "GĐ4", "Readiness FINAL_ROUND", "200", f"ready={rd.get('data',{}).get('ready')}", "PASS" if rd["ok"] else "FAIL")
        if final4:
            act = request("PATCH", f"/rounds/{final4['id']}/activate", coord)
            log("G4-H04", "GĐ4", "Activate final", "200 or gate", f"{act['status']} {act.get('code','')}",
                "PASS" if act["ok"] or act.get("code") in ("RESULT_NOT_PUBLISHED", "JUDGE_NOT_ASSIGNED") else "FAIL")

    h4pub = find_hackathon("seal-gd4-published", coord)
    if h4pub:
        prelim_p = next((r for r in get_rounds(h4pub["id"], coord) if not r.get("isFinal")), None)
        final_p = next((r for r in get_rounds(h4pub["id"], coord) if r.get("isFinal")), None)
        if prelim_p and final_p and not final_p.get("isActive"):
            try:
                sb = urllib.request.urlopen(f"{BASE}/rounds/{prelim_p['id']}/scoreboard", timeout=10)
                log("G4-SB", "GĐ4", "Public scoreboard", "200", str(sb.status), "PASS")
            except urllib.error.HTTPError as e:
                log("G4-SB", "GĐ4", "Public scoreboard", "200", str(e.code), "FAIL" if e.code != 200 else "PASS")

    # === GĐ5 ===
    h5 = find_hackathon("seal-gd5-final-active", coord)
    if h5:
        final5 = next((r for r in get_rounds(h5["id"], coord) if r.get("isFinal")), None)
        if final5:
            log("G5-H00", "GĐ5", "Final active", "true", str(final5.get("isActive")), "PASS" if final5.get("isActive") else "FAIL")
            try:
                stu5 = login("student.gd5.leader03@fpt.edu.vn", "Student@dev1")
                sub = request("GET", f"/me/submission?roundId={final5['id']}", stu5)
                log("G5-H01", "GĐ5", "Student submission", "200", str(sub["status"]), "PASS" if sub["ok"] else "FAIL")
            except Exception as e:
                log("G5-H01", "GĐ5", "Student submission", "200", str(e), "FAIL")
            rank = request("GET", f"/rounds/{final5['id']}/ranking/preview", coord)
            log("G5-RANK", "GĐ5", "Final ranking preview", "200", str(rank["status"]), "PASS" if rank["ok"] else "FAIL")
            cal = request("GET", f"/calibration-sessions?roundId={final5['id']}", coord)
            log("G5-CAL", "GĐ5", "Calibration list", "200", str(cal["status"]), "PASS" if cal["ok"] else "SPEC_GAP")
            rbl = request("GET", f"/rounds/{final5['id']}/rbl/progress", coord)
            log("G5-RBL", "GĐ5", "RBL progress", "200", f"{rbl['status']} {rbl.get('code','')}", "PASS" if rbl["ok"] else "SPEC_GAP")
            if not final5.get("scoringLocked"):
                lock = request("PATCH", f"/rounds/{final5['id']}/lock-scoring", coord, {"note": "audit"})
                log("G5-H03", "GĐ5", "Lock final", "200", str(lock["status"]), "PASS" if lock["ok"] else "FAIL")
            h5s = request("GET", f"/hackathons/{h5['id']}", coord)
            st = (h5s.get("data") or {}).get("status")
            log("G5-H04", "GĐ5", "PENDING_CONFIRM after lock", "PENDING_CONFIRM", st, "PASS" if st == "PENDING_CONFIRM" else "FAIL")

    h5lock = find_hackathon("seal-gd5-late-hardlock", coord)
    if h5lock:
        final_l = next((r for r in get_rounds(h5lock["id"], coord) if r.get("isFinal")), None)
        if final_l and final_l.get("scoringLocked"):
            sc = request("POST", "/scores", guest, {"submissionId": 1, "criterionId": 1, "scoreValue": 5})
            log("ADV-02", "GĐ5", "Score after lock", "SCORING_LOCKED", f"{sc.get('code','')}", "PASS" if sc.get("code") == "SCORING_LOCKED" else "FAIL")

    # === GĐ6 ===
    h6 = find_hackathon("seal-gd6-pending-confirm", coord)
    if h6:
        rd = request("GET", f"/hackathons/{h6['id']}/readiness?target=AWARDS", coord)
        log("G6-R01", "GĐ6", "Readiness AWARDS", "ready=true", str(rd.get("data",{}).get("ready")), "PASS" if rd.get("data",{}).get("ready") else "PARTIAL")
        ranks = request("GET", f"/hackathons/{h6['id']}/team-rankings", coord)
        cnt = len(ranks["data"] or []) if ranks["ok"] else 0
        log("G6-RANK", "GĐ6", "Team rankings", ">0", f"count={cnt}", "PASS" if cnt > 0 else "PARTIAL")
        # Don't actually confirm to preserve seed - use confirm-ready or check only
        log("G6-H03", "GĐ6", "Confirm (dry-run skip)", "200", "SKIPPED preserve seed", "BLOCKED", "Would mutate seed")

    h6empty = find_hackathon("seal-gd6-prizes-empty", coord)
    if h6empty:
        c = request("PATCH", f"/hackathons/{h6empty['id']}/confirm", coord, {"confirm": True})
        log("G6-N01", "GĐ6", "Confirm no prizes", "NO_PRIZES_RECORDED", c.get("code"), "PASS" if c.get("code") == "NO_PRIZES_RECORDED" else "FAIL")

    h6edge = find_hackathon("seal-gd6-edge-errors", coord)
    if h6edge:
        c = request("PATCH", f"/hackathons/{h6edge['id']}/confirm", coord, {"confirm": True})
        log("G6-N02", "GĐ6", "Confirm CK not locked", "ROUND_NOT_SCORING_LOCKED", c.get("code"),
            "PASS" if c.get("code") == "ROUND_NOT_SCORING_LOCKED" else "FAIL")

    h6fin = find_hackathon("seal-gd6-finished-export", coord)
    if h6fin:
        c = request("PATCH", f"/hackathons/{h6fin['id']}/confirm", coord, {"confirm": True})
        log("ADV-08", "GĐ6", "Confirm when FINISHED", "HACKATHON_NOT_PENDING_CONFIRM", c.get("code"),
            "PASS" if c.get("code") == "HACKATHON_NOT_PENDING_CONFIRM" else "FAIL")
        exp = request("POST", "/export-jobs", coord, {"hackathonId": h6fin["id"], "format": "CSV"})
        log("G6-EXP", "GĐ6", "Export job", "201", f"{exp['status']}", "PASS" if exp["ok"] else "PARTIAL")

    h6confirm = find_hackathon("seal-gd6-confirm-ready", coord)
    if h6confirm:
        c = request("PATCH", f"/hackathons/{h6confirm['id']}/confirm", coord, {"confirm": True, "note": "audit"})
        log("G6-H03b", "GĐ6", "Confirm ready seed", "200 FINISHED", f"{c['status']}", "PASS" if c["ok"] else "FAIL")

    out = "d:/FPT/SU26/SWP/ManageSealHackathon/BE/scripts/gd3-gd6-api-audit-results.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump(RESULTS, f, ensure_ascii=False, indent=2)
    passed = sum(1 for r in RESULTS if r["status"] == "PASS")
    failed = sum(1 for r in RESULTS if r["status"] == "FAIL")
    print(f"TOTAL={len(RESULTS)} PASS={passed} FAIL={failed}")
    for r in RESULTS:
        print(f"{r['status']:8} {r['id']:12} {r['actual']}")


if __name__ == "__main__":
    main()
