# -*- coding: utf-8 -*-
"""Generate api-catalog-with-json.md with real sample JSON."""
import re, os, json

SAMPLES = {
    ("POST", "/api/v1/auth/login"): (
        {"email": "coord@fpt.edu.vn", "password": "Coordinator@dev1"},
        {"accessToken": "eyJ...", "refreshToken": "...", "tokenType": "Bearer", "expiresInSeconds": 1800, "mustChangePassword": False},
    ),
    ("POST", "/api/v1/auth/register"): (
        {"email": "sv@fpt.edu.vn", "password": "password12", "confirmPassword": "password12"},
        {"userId": 42, "email": "sv@fpt.edu.vn", "status": "PENDING", "message": "Đăng ký thành công."},
    ),
    ("POST", "/api/v1/auth/refresh"): (
        {"refreshToken": "{{refreshToken}}"},
        {"accessToken": "eyJ...", "refreshToken": "new-refresh...", "expiresInSeconds": 1800},
    ),
    ("POST", "/api/v1/auth/forgot-password"): (
        {"email": "user@fpt.edu.vn"},
        None,
    ),
    ("POST", "/api/v1/auth/reset-password"): (
        {"token": "reset-token-from-email", "newPassword": "NewPass@123", "confirmPassword": "NewPass@123"},
        None,
    ),
    ("POST", "/api/v1/auth/change-password"): (
        {"oldPassword": "password12", "newPassword": "NewPass@123", "confirmPassword": "NewPass@123"},
        None,
    ),
    ("POST", "/api/v1/hackathons"): (
        {"name": "SEAL Test", "slug": "seal-test-2026", "season": "Spring", "year": 2026,
         "registrationStart": "2026-05-01", "registrationEnd": "2026-06-01",
         "eventStart": "2026-06-02", "eventEnd": "2026-07-17", "wildcardEnabled": True,
         "individualRankingEnabled": False},
        {"id": 1, "name": "SEAL Test", "slug": "seal-test-2026", "status": "DRAFT", "season": "Spring", "year": 2026},
    ),
    ("PATCH", "/api/v1/hackathons/{id}/status"): (
        {"status": "ONGOING"},
        {"id": 1, "status": "ONGOING"},
    ),
    ("POST", "/api/v1/teams"): (
        {"hackathonId": 1, "teamName": "Team Alpha"},
        {"id": 10, "teamName": "Team Alpha", "status": "PENDING", "hackathonId": 1},
    ),
    ("POST", "/api/v1/teams/{teamId}/members/invite"): (
        {"email": "member@fpt.edu.vn"},
        {"invitationId": 5, "status": "PENDING"},
    ),
    ("PATCH", "/api/v1/teams/{teamId}/members/{userId}"): (
        {"action": "ACCEPT"},
        None,
    ),
    ("POST", "/api/v1/submissions"): (
        {"teamId": 10, "trackId": 5, "repoUrl": "https://github.com/o/r", "demoUrl": "https://d.example.com", "slideUrl": "https://s.example.com"},
        {"id": 7, "teamId": 10, "trackId": 5, "status": "SUBMITTED"},
    ),
    ("POST", "/api/v1/scores"): (
        {"submissionId": 7, "criterionId": 1, "scoreValue": 8.5, "scoreType": "NORMAL", "comment": "Good"},
        {"id": 100, "submissionId": 7, "criterionId": 1, "scoreValue": 8.5},
    ),
    ("PATCH", "/api/v1/rounds/{id}/activate"): (
        {"note": "Kích hoạt Sơ loại"},
        {"id": 3, "isActive": True},
    ),
    ("PATCH", "/api/v1/rounds/{id}/release-problem"): (
        {"problemStatementUrl": "https://example.com/de.pdf"},
        {"id": 3, "problemReleasedAt": "2026-05-29T07:00:00"},
    ),
    ("PATCH", "/api/v1/rounds/{id}/lock-scoring"): (
        {"force": False},
        {"id": 3, "scoringLocked": True},
    ),
    ("PATCH", "/api/v1/rounds/{id}/publish"): (
        {"confirm": True},
        {"id": 3, "isPublished": True},
    ),
    ("POST", "/api/v1/judge-assignments"): (
        {"judgeId": 3, "trackId": 5, "assignmentType": "NORMAL"},
        {"id": 20, "judgeId": 3, "trackId": 5, "assignmentType": "NORMAL"},
    ),
    ("POST", "/api/v1/mentor-assignments"): (
        {"mentorId": 4, "trackId": 5},
        {"id": 15, "mentorId": 4, "trackId": 5},
    ),
    ("POST", "/api/v1/users/temp-judges"): (
        {"email": "guest.judge@company.com", "fullName": "Guest Judge", "organization": "ACME"},
        {"userId": 8, "email": "guest.judge@company.com", "invitationId": 3},
    ),
    ("PATCH", "/api/v1/users/{id}/status"): (
        {"status": "APPROVED"},
        {"id": 42, "status": "APPROVED"},
    ),
    ("GET", "/api/v1/users/me"): (
        None,
        {"id": 42, "email": "sv@fpt.edu.vn", "fullName": "Nguyen Van A", "role": "STUDENT", "status": "APPROVED"},
    ),
    ("POST", "/api/v1/me/appeals"): (
        {"teamId": 10, "roundId": 3, "reason": "Kết quả chưa đúng", "evidenceUrl": "https://..."},
        {"id": 1, "status": "PENDING"},
    ),
    ("PATCH", "/api/v1/me/notifications/read"): (
        {"notificationIds": [1, 2, 3]},
        None,
    ),
    ("POST", "/api/v1/me/tiebreak-evaluations"): (
        {"roundId": 3, "orderedTeamIds": [10, 12, 8]},
        {"roundId": 3, "orderedTeamIds": [10, 12, 8], "status": "SUBMITTED"},
    ),
}

def normalize(path):
    path = re.sub(r"\{[^}]+\}", "{id}", path)
    return path

def get_samples(method, path):
    key = (method, normalize(path))
    if key in SAMPLES:
        return SAMPLES[key]
    if method == "GET":
        if "{id}" in path or path.endswith("/{id}"):
            return None, {"id": 1, "name": "..."}
        return None, []
    if method in ("POST", "PUT", "PATCH"):
        return {}, {"id": 1}
    if method == "DELETE":
        return None, None
    return None, {}

root = "src/main/java/com/sealhackathon/api"
apis = []
for dirpath, _, files in os.walk(root):
    for f in files:
        if not f.endswith("Controller.java"):
            continue
        text = open(os.path.join(dirpath, f), encoding="utf-8").read()
        m = re.search(r'@RequestMapping\("([^"]+)"\)', text)
        base = m.group(1) if m else ""
        for mm in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', text):
            method = mm.group(1).upper()
            sub = mm.group(2) or ""
            if sub and not sub.startswith("/"):
                sub = "/" + sub
            full = (base.rstrip("/") + sub) if sub else base
            apis.append((method, full))

def gd(path):
    """Giai đoạn test — kiểm tra pattern cụ thể trước pattern rộng."""
    if path in ("/", "/actuator/health"):
        return 0, "GĐ0 — System & Health"
    if "/auth/" in path or path.startswith("/api/v1/users") or "/invitations" in path:
        return 0, "GĐ0 — Auth & Users"
    if "/me/" in path or path == "/api/v1/me":
        return 7, "GĐ7 — Portal /me (Student · Judge · Mentor)"
    if any(x in path for x in ("/confirm", "/export-jobs", "/prizes", "team-rankings", "/rankings")):
        return 6, "GĐ6 — Kết thúc & Trao giải"
    if any(x in path for x in ("wildcard", "tiebreak", "/advance", "/publish")):
        return 4, "GĐ4 — Chuyển vòng & Publish"
    if "/teams" in path:
        return 2, "GĐ2 — Đăng ký & Đội"
    if "/submissions" in path or "/scores" in path or "/calibration" in path or "/rbl/" in path:
        return 3, "GĐ3/GĐ5 — Thi, nộp bài & Chấm"
    if "/rounds" in path:
        return 3, "GĐ3/GĐ5 — Vòng thi (rounds)"
    if any(x in path for x in ("/hackathons", "/tracks", "/criteria", "/events", "assignments", "/mentor-assignments", "/judge-assignments")):
        return 1, "GĐ1 — Chuẩn bị sự kiện"
    return 9, "GĐ9 — Khác (public, WS)"

apis.sort(key=lambda x: (gd(x[1])[0], x[1], x[0]))

lines = [
    "# API Catalog — Request / Response JSON",
    "",
    "**Playbook E2E:** [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)",
    "",
    "Envelope 2xx: `{ success, data, message?, traceId, timestamp }` — JSON dưới là **`data`**.",
    "",
    "---",
]
cur_section = None
sec_idx = {}
for method, full in apis:
    g, gname = gd(full)
    section_key = (g, gname)
    if section_key != cur_section:
        cur_section = section_key
        sec_idx[g] = 0
        lines.append(f"\n## {gname}\n")
    sec_idx[g] = sec_idx.get(g, 0) + 1
    req, res = get_samples(method, full)
    lines.append(f"### {g}.{sec_idx[g]:03d} `{method} {full}`\n")
    if method in ("GET", "DELETE"):
        lines.append("**Request:** *(không body)*\n")
    else:
        lines.append("**Request:**\n```json\n" + json.dumps(req, indent=2, ensure_ascii=False) + "\n```\n")
    if res is None:
        lines.append("**Response `data`:** `null`\n")
    else:
        lines.append("**Response `data`:**\n```json\n" + json.dumps(res, indent=2, ensure_ascii=False) + "\n```\n")
    lines.append("---\n")

open("docs/testing/api-catalog-with-json.md", "w", encoding="utf-8").write("\n".join(lines))
print("done", len(apis), "sections", len(sec_idx))
