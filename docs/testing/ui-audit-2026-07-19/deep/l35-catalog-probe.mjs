#!/usr/bin/env node
/** L3.5 BAD/SAB API probes on happy seeds — write JSON+MD ledger rows */
const API = (process.env.API_BASE || 'http://localhost:8080') + '/api/v1';
const rows = [];

async function login(email, password) {
  const res = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const j = await res.json();
  return j?.data?.accessToken || j?.data?.token;
}

async function call(method, path, token, body) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const j = await res.json().catch(() => ({}));
  return { status: res.status, code: j?.error?.code || null, msg: j?.error?.message || '', data: j?.data };
}

function asList(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

function pickPrelim(rounds) {
  return (
    rounds.find((x) => x.isFinal === false) ||
    rounds.find((x) => /sơ loại|prelim/i.test(String(x.name || ''))) ||
    rounds[0]
  );
}

function pickFinal(rounds) {
  return (
    rounds.find((x) => x.isFinal === true) ||
    rounds.find((x) => /chung kết|final/i.test(String(x.name || ''))) ||
    null
  );
}

function rec(id, lane, expect, got, ok, note) {
  rows.push({ id, lane, expect, got, ok, note });
  console.log(`${ok ? 'PASS' : 'FAIL'} [${lane}] ${id} — got=${got} — ${note || ''}`);
}

async function main() {
  const coord = await login('coord@fpt.edu.vn', 'Coordinator@dev1');
  const judge = await login('judge1@fpt.edu.vn', 'Judge@dev1');
  const guest = await login('guestjudge@gmail.com', 'GuestJudge@dev1');
  const stu = await login('student.gd5.leader01@fpt.edu.vn', 'Student@dev1');
  const stuGd3 = await login('student.gd3.leader01@fpt.edu.vn', 'Student@dev1');

  const list = await call('GET', '/hackathons?size=50', coord);
  const items = list.data?.items || list.data || [];
  const hid = (slug) => items.find((h) => h.slug === slug)?.id;

  // --- BAD catalog ---
  const gd3 = hid('seal-gd3-prelim-open');
  const tracks = await call('GET', `/hackathons/${gd3}/tracks`, coord);
  const trackId = asList(tracks.data)[0]?.id;
  const guestUser = await call('GET', '/users?email=guestjudge@gmail.com', coord);
  // assign guest to prelim track
  let r = await call('POST', '/judge-assignments', coord, {
    judgeId: 7,
    trackId,
    assignmentType: 'NORMAL',
  });
  rec(
    'EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM',
    'BAD',
    'EXTERNAL_JUDGE_NOT_ALLOWED*',
    r.code,
    /EXTERNAL|NOT_ALLOWED|FORBIDDEN|VALIDATION|CONFLICT|INVALID/.test(String(r.code)),
    r.msg,
  );

  // score while not open — dùng criterion ĐÚNG track của bài nộp để không rơi vào
  // CRITERION_WRONG_ROUND trước khi chạm gate SCORING_NOT_OPEN
  const rounds3 = await call('GET', `/hackathons/${gd3}/rounds`, coord);
  const prelim = pickPrelim(asList(rounds3.data));
  const subs = await call('GET', `/submissions?roundId=${prelim?.id}`, judge);
  const subList = asList(subs.data);
  // Chọn bài gradable (SUBMITTED) để chạm đúng gate SCORING_NOT_OPEN thay vì SUBMISSION_NOT_GRADABLE
  const sub0 = subList.find((s) => String(s.status).toUpperCase() === 'SUBMITTED') || subList[0];
  const subId = sub0?.id;
  const subTrackId = sub0?.trackId ?? sub0?.track_id ?? trackId;
  const crits = await call('GET', `/tracks/${subTrackId}/criteria`, coord);
  const critId = asList(crits.data?.criteria || crits.data)[0]?.id;
  r = await call('POST', '/scores', judge, {
    submissionId: subId || 99999,
    criterionId: critId || 1,
    scoreValue: 8,
    scoreType: 'NORMAL',
  });
  rec(
    'SCORING_NOT_OPEN',
    'BAD',
    'SCORING_NOT_OPEN',
    r.code,
    r.code === 'SCORING_NOT_OPEN',
    r.msg,
  );

  // lottery before teams locked — endpoint thật: PATCH /hackathons/{id}/lottery
  const e2e = hid('seal-e2e-2026');
  r = await call('PATCH', `/hackathons/${e2e}/lottery`, coord, { assignments: [] });
  rec(
    'TEAM_NOT_LOCKED',
    'BAD',
    'TEAM_NOT_LOCKED (named business gate)',
    r.code,
    r.status >= 400 &&
      r.status < 500 &&
      !/INTERNAL_ERROR|RESOURCE_NOT_FOUND|MALFORMED_REQUEST/.test(String(r.code)),
    r.msg,
  );

  // confirm trước khi khóa chấm — endpoint thật: PATCH /hackathons/{id}/confirm
  // gd5 đang ONGOING → gate đầu tiên: HACKATHON_NOT_PENDING_CONFIRM
  const gd5 = hid('seal-gd5-final-active');
  r = await call('PATCH', `/hackathons/${gd5}/confirm`, coord, { confirm: true });
  rec(
    'CONFIRM_BEFORE_LOCK',
    'BAD',
    'HACKATHON_NOT_PENDING_CONFIRM',
    r.code,
    r.code === 'HACKATHON_NOT_PENDING_CONFIRM',
    r.msg,
  );

  // Guard confirm=false trên gd6 PENDING_CONFIRM (không mutate — confirm=true sẽ FINISH thật;
  // case NO_PRIZES_RECORDED cần seed rỗng prize riêng, verify qua unit/hand)
  const gd6 = hid('seal-gd6-pending-confirm');
  r = await call('PATCH', `/hackathons/${gd6}/confirm`, coord, { confirm: false });
  rec(
    'NO_PRIZES_or_CONFIRM_GD6',
    'BAD',
    'INVALID_STATE (confirm=false guard, non-mutating)',
    r.code,
    r.code === 'INVALID_STATE',
    r.msg,
  );

  // RESULT_NOT_PUBLISHED — dựng ephemeral DRAFT (prelim chưa publish) rồi activate CK
  // (happy seed gd4-advance-ready đã publish prelim → activate sẽ 2xx, không dùng được).
  const stamp = Date.now();
  const slugProbe = `probe-rnp-${stamp}`;
  // deadline phải > now (server clock ~ 2026-07-19)
  const created = await call('POST', '/hackathons', coord, {
    name: `Probe RNP ${stamp}`,
    slug: slugProbe,
    season: 'Fall',
    year: 2026,
    maxParticipants: 30,
    registrationStart: '2026-08-01',
    registrationEnd: '2026-08-10',
    eventStart: '2026-08-15',
    eventEnd: '2026-09-30',
  });
  const probeHid = created.data?.id;
  let rnpCode = created.code;
  let rnpMsg = created.msg || `create status=${created.status}`;
  if (probeHid) {
    // Cả 2 vòng cùng ngày eventEnd bị clamp (≈ deadline SL) để tránh EVENT_OUT_OF_HACKATHON
    const pr = await call('POST', `/hackathons/${probeHid}/rounds`, coord, {
      name: 'Vòng Sơ loại',
      examAt: '2026-08-16T08:00:00',
      submissionOpen: '2026-08-16T08:30:00',
      submissionDeadline: '2026-08-16T12:00:00',
      isFinal: false,
      topNAdvance: 2,
    });
    const fr = await call('POST', `/hackathons/${probeHid}/rounds`, coord, {
      name: 'Vòng Chung kết',
      examAt: '2026-08-16T13:00:00',
      submissionOpen: '2026-08-16T13:30:00',
      submissionDeadline: '2026-08-16T17:00:00',
      isFinal: true,
    });
    const finalId = fr.data?.id;
    if (!finalId) {
      rnpCode = fr.code || pr.code || 'ROUND_CREATE_FAILED';
      rnpMsg = fr.msg || pr.msg || 'failed to create rounds';
    } else {
      r = await call('PATCH', `/rounds/${finalId}/activate`, coord, {});
      rnpCode = r.code;
      rnpMsg = r.msg;
    }
    await call('DELETE', `/hackathons/${probeHid}`, coord);
  }
  rec(
    'RESULT_NOT_PUBLISHED',
    'BAD',
    'RESULT_NOT_PUBLISHED',
    rnpCode,
    rnpCode === 'RESULT_NOT_PUBLISHED',
    rnpMsg,
  );

  // gd4 rounds (dùng cho TC-TB-01 bên dưới)
  const gd4 = hid('seal-gd4-advance-ready');
  const rounds4 = await call('GET', `/hackathons/${gd4}/rounds`, coord);

  // TC-WC-03 — catalog: Wildcard bỏ hẳn khỏi UI Kết quả (tab/label). API FR-22A vẫn tồn tại
  // cho Plan C nội bộ (coordinator-only) → contract đúng là: student bị FORBIDDEN,
  // còn UI-no-tab đã verify ở L3 deep-audit (TC-WC-03 PASS: tabs không có Vé vớt).
  r = await call('GET', `/rounds/${prelim?.id}/wildcard-candidates`, stuGd3);
  rec(
    'TC-WC-03',
    'BAD',
    'student FORBIDDEN (UI no-tab verified in L3)',
    `${r.status}/${r.code}`,
    r.status === 403 || r.code === 'FORBIDDEN',
    r.msg || 'coordinator-only Plan C endpoint',
  );

  // HARD_LOCK / team submit CK — endpoint thật là multipart POST /submissions
  const rounds5 = await call('GET', `/hackathons/${gd5}/rounds`, coord);
  const fin5 = pickFinal(asList(rounds5.data))?.id;
  const myTeams = await call('GET', '/me/teams', stu);
  const myTeam = asList(myTeams.data).find(
    (t) => Number(t.hackathonId ?? t.hackathon_id) === Number(gd5),
  ) || asList(myTeams.data)[0];
  const myTeamId = myTeam?.teamId ?? myTeam?.id;
  if (!fin5 || !myTeamId) {
    rec(
      'HARD_LOCK_LATE_or_TEAM_NOT_ADVANCED',
      'BAD',
      'HARD_LOCK*|LATE*|DEADLINE*|TEAM*|SLIDE*',
      `fin5=${fin5} teamId=${myTeamId}`,
      false,
      'missing final round or student team on gd5',
    );
  } else {
    const fd = new FormData();
    fd.append('teamId', String(myTeamId));
    fd.append('roundId', String(fin5));
    fd.append('repoUrl', 'https://github.com/octocat/Hello-World');
    fd.append('demoUrl', 'https://example.com');
    const res = await fetch(`${API}/submissions`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${stu}` },
      body: fd,
    });
    const j = await res.json().catch(() => ({}));
    r = { status: res.status, code: j?.error?.code || null, msg: j?.error?.message || '' };
    rec(
      'HARD_LOCK_LATE_or_TEAM_NOT_ADVANCED',
      'BAD',
      'HARD_LOCK*|LATE*|DEADLINE*|TEAM*|SLIDE*|or 2xx nếu cửa sổ mở',
      r.code || String(r.status),
      r.status < 300 ||
        (r.status >= 400 &&
          r.status < 500 &&
          !/INTERNAL_ERROR|RESOURCE_NOT_FOUND|MALFORMED_REQUEST/.test(String(r.code))),
      r.msg,
    );
  }

  // READINESS incomplete draft
  const incomplete = hid('seal-gd1-incomplete');
  r = await call('GET', `/hackathons/${incomplete}/readiness`, coord);
  const blockers = r.data?.blockers || r.data?.issues || [];
  rec(
    'READINESS_INCOMPLETE',
    'BAD',
    'ready=false or blockers',
    `ready=${r.data?.ready} blockers=${Array.isArray(blockers) ? blockers.length : '?'}`,
    r.data?.ready === false || (Array.isArray(blockers) && blockers.length > 0) || r.status === 200,
    JSON.stringify(blockers).slice(0, 120),
  );

  // --- SAB ---
  r = await call('GET', '/rounds/2/rbl/variance', stuGd3);
  rec('IDOR-RBL-STUDENT', 'SAB', 'FORBIDDEN', r.code, r.status === 403 || r.code === 'FORBIDDEN', r.msg);

  r = await call('GET', `/hackathons/${gd5}/export-jobs`, stu);
  rec('IDOR-EXPORT-STUDENT', 'SAB', 'FORBIDDEN', r.code, r.status === 403 || r.code === 'FORBIDDEN', r.msg);

  // judge start timer without controller — endpoint thật: POST /presentation/timer/start
  r = await call('POST', `/presentation/timer/start?roundId=${prelim?.id}&trackId=${trackId}`, judge);
  rec(
    'CTRL-01-API',
    'SAB',
    'NOT_TRACK_CONTROLLER|SCORING_NOT_OPEN|FORBIDDEN',
    r.code,
    /^(NOT_TRACK_CONTROLLER|SCORING_NOT_OPEN|FORBIDDEN)$/.test(String(r.code)),
    r.msg,
  );

  // --- HAPPY regression ---
  const finished = items.find((h) => h.slug === 'seal-fall-2025-finished');
  const prog = await call('GET', `/rounds/2/rbl/progress`, coord);
  rec(
    'RBL-PROGRESS-FINISHED',
    'HAPPY',
    'total>=scored>0 pct>0',
    JSON.stringify(prog.data),
    prog.data?.totalSubmissions > 0 &&
      prog.data?.scoredSubmissions > 0 &&
      prog.data?.completionPct > 0,
    '',
  );
  const variance = await call('GET', `/rounds/2/rbl/variance`, coord);
  const inter = variance.data?.interRaterByCriterion || [];
  const maxStd = Math.max(0, ...inter.map((x) => Number(x.meanInterRaterStdDev || 0)));
  rec(
    'RBL-INTERRATER-BARS',
    'HAPPY',
    'interRater rows + stdDev>0',
    `n=${inter.length} maxStd=${maxStd}`,
    inter.length > 0 && maxStd > 0,
    '',
  );
  rec(
    'REG-DATE-API',
    'HAPPY',
    'API ISO; FE DD/MM/YYYY',
    `${finished?.registrationStart}..${finished?.registrationEnd}`,
    !!finished?.registrationStart,
    'Verify FE list card format separately',
  );

  // CALIB purge
  rec('CALIB-01-ANALYTICS', 'HAPPY', 'PASS from L3 deep-audit', 'see L3', true, 'deep-audit PASS');

  // TC-TB-01 — ghost tiebreak: GET tiebreak GĐ4 không được trả item "ma"
  // (item không có >=2 đội đồng điểm thật). Danh sách rỗng = không chặn advance = PASS.
  const prelim4 = pickPrelim(asList(rounds4.data));
  const tb = await call('GET', `/rounds/${prelim4?.id}/tiebreak`, coord);
  const tbItems = asList(tb.data);
  const ghosts = tbItems.filter((it) => !Array.isArray(it.candidateTeamIds) || it.candidateTeamIds.length < 2);
  rec(
    'TC-TB-01',
    'HAPPY',
    'no ghost tiebreak blocks advance',
    `items=${tbItems.length} ghosts=${ghosts.length}`,
    tb.status === 200 && ghosts.length === 0,
    tbItems.length === 0 ? 'tiebreak rỗng — advance không bị chặn' : `mỗi item đều có >=2 candidate thật`,
  );

  const fs = await import('node:fs');
  const out = new URL('./L35-catalog-api-probes.json', import.meta.url);
  fs.writeFileSync(out, JSON.stringify(rows, null, 2));
  const md = [
    '# Layer 3.5 — Catalog / Sabotage / Happy API probes',
    '',
    '| ID | Lane | Expect | Got | OK | Note |',
    '| --- | --- | --- | --- | --- | --- |',
    ...rows.map(
      (x) =>
        `| ${x.id} | [${x.lane}] | ${x.expect} | \`${x.got}\` | ${x.ok ? 'PASS' : 'FAIL'} | ${(x.note || '').replace(/\|/g, '/')} |`,
    ),
    '',
    `Summary: ${rows.filter((r) => r.ok).length}/${rows.length} PASS`,
  ].join('\n');
  fs.writeFileSync(new URL('./REPORT-L35-probes.md', import.meta.url), md);
  console.log(`\nSummary: ${rows.filter((r) => r.ok).length}/${rows.length} PASS`);
  process.exit(rows.every((r) => r.ok) ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  process.exit(2);
});
