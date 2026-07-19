// Probe live seeds to validate mapping for reviving skipped e2e suites.
const BE = 'http://localhost:8080/api/v1';

async function call(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${BE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json, data: json?.data ?? json, code: json?.error?.code || json?.code };
}

async function login(email, password) {
  const r = await call('POST', '/auth/login', { body: { email, password } });
  return r.data?.accessToken;
}

const out = [];
const log = (...a) => { console.log(...a); out.push(a.join(' ')); };

const coord = await login('coord@fpt.edu.vn', 'Coordinator@dev1');
const hacks = await call('GET', '/hackathons?size=200', { token: coord });
const list = Array.isArray(hacks.data) ? hacks.data : hacks.data?.items || [];
const bySlug = (s) => list.find((h) => h.slug === s);

log('LIVE SLUGS:', list.map((h) => h.slug).join(', '));

// 1. seal-e2e-2026 season + fall-track-select gate for spring leader
const e2e = bySlug('seal-e2e-2026');
log('e2e-2026 season =', e2e?.season, 'status =', e2e?.status);
const springLeader = await login('student.e2e.t01.leader@fpt.edu.vn', 'Student@dev1');
if (springLeader && e2e) {
  const sel = await call('GET', `/me/hackathons/${e2e.id}/selectable-tracks`, { token: springLeader });
  log('spring leader selectable-tracks:', sel.status, sel.code);
  const pick = await call('POST', '/me/tracks/999999/select', { token: springLeader });
  log('spring leader select track:', pick.status, pick.code);
} else {
  log('spring leader login failed?', Boolean(springLeader));
}

// 2. mentor2 track-only check
const mentor2 = await login('mentor2@fpt.edu.vn', 'Mentor@dev1');
if (mentor2) {
  const rounds = await call('GET', '/me/mentor/rounds', { token: mentor2 });
  const ta = await call('GET', '/me/mentor/track-assignments', { token: mentor2 });
  log('mentor2 rounds:', rounds.status, 'count =', (rounds.data || []).length);
  log('mentor2 track-assignments:', ta.status, 'count =', (ta.data || []).length);
} else log('mentor2 login FAILED');

// 3. mentor@ on gd3-prelim-open
const mentor = await login('mentor@fpt.edu.vn', 'Mentor@dev1');
const gd3 = bySlug('seal-gd3-prelim-open');
if (mentor && gd3) {
  const rounds = await call('GET', `/hackathons/${gd3.id}/rounds`, { token: coord });
  const rl = Array.isArray(rounds.data) ? rounds.data : rounds.data?.items || [];
  const prelim = rl.find((r) => !(r.isFinal || r.is_final));
  log('gd3 prelim id =', prelim?.id);
  const mr = await call('GET', '/me/mentor/rounds', { token: mentor });
  log('mentor rounds count =', (mr.data || []).length, JSON.stringify((mr.data || []).map((r) => r.roundId)));
  if (prelim) {
    const at = await call('GET', `/me/mentor/rounds/${prelim.id}/assigned-teams`, { token: mentor });
    const teams = at.data?.teams || [];
    log('mentor assigned-teams:', at.status, 'teams =', teams.map((t) => t.teamName || t.team_name).join('|'));
    // IDOR: mentor2 tries reading one of mentor's teams
    const teamId = teams[0]?.teamId ?? teams[0]?.team_id ?? teams[0]?.id;
    if (teamId && mentor2) {
      const idor = await call('GET', `/me/mentor/teams/${teamId}/submissions?roundId=${prelim.id}`, { token: mentor2 });
      log('mentor2 IDOR read team', teamId, ':', idor.status, idor.code);
    }
    // tracks on prelim
    const tr = await call('GET', `/rounds/${prelim.id}/tracks`, { token: coord });
    const trl = Array.isArray(tr.data) ? tr.data : tr.data?.items || [];
    log('gd3 prelim tracks:', trl.map((t) => `${t.id}:${t.name}`).join('|'));
    // mentor userId
    const meMentor = await call('GET', '/users/me', { token: mentor });
    log('mentor userId =', meMentor.data?.id);
    // conflict probe (dry): try assigning mentor as judge on track1 — EXPECT CONFLICT (no mutation on conflict)
    if (trl[0]) {
      const conflict = await call('POST', '/judge-assignments', {
        token: coord,
        body: { judgeId: meMentor.data?.id, trackId: trl[0].id, assignmentType: 'NORMAL' },
      });
      log('assign mentor as judge track1:', conflict.status, conflict.code);
    }
  }
}

// 4. tiebreak seeds
for (const slug of ['seal-gd4-tiebreak-manual', 'seal-gd4-tiebreak-submission-time']) {
  const h = bySlug(slug);
  if (!h) { log(slug, 'MISSING'); continue; }
  const rounds = await call('GET', `/hackathons/${h.id}/rounds`, { token: coord });
  const rl = Array.isArray(rounds.data) ? rounds.data : rounds.data?.items || [];
  const prelim = rl.find((r) => !(r.isFinal || r.is_final));
  const tb = await call('GET', `/rounds/${prelim?.id}/tiebreak`, { token: coord });
  const items = Array.isArray(tb.data) ? tb.data : tb.data?.items || [];
  log(slug, 'prelim', prelim?.id, 'tiebreak:', tb.status, 'items =', items.length);
}

// 5. orphans + teams on e2e-2026
if (e2e) {
  const orphans = await call('GET', `/teams/hackathons/${e2e.id}/orphans`, { token: coord });
  const ol = Array.isArray(orphans.data) ? orphans.data : orphans.data?.items || [];
  log('e2e orphans:', orphans.status, 'count =', ol.length, ol.map((o) => o.email || o.userEmail || '').join('|'));
  const teams = await call('GET', `/teams?hackathonId=${e2e.id}&size=100`, { token: coord });
  const tl = Array.isArray(teams.data) ? teams.data : teams.data?.items || [];
  log('e2e teams:', tl.map((t) => `${t.id}:${t.teamName || t.name}:${t.status}`).join('|'));
  const orphan1 = await login('student.e2e.orphan1@fpt.edu.vn', 'Student@dev1');
  log('orphan1 login:', Boolean(orphan1));
  if (orphan1) {
    const mm = await call('GET', `/teams/hackathons/${e2e.id}/matchmaking`, { token: orphan1 });
    log('orphan1 matchmaking:', mm.status);
  }
  // busy invite: leader t01 invites t02 leader
  const t01 = tl.find((t) => /T01/i.test(t.teamName || t.name || ''));
  const l1 = await login('student.e2e.t01.leader@fpt.edu.vn', 'Student@dev1');
  if (t01 && l1) {
    const inv = await call('POST', `/teams/${t01.id}/members/invite`, {
      token: l1, body: { email: 'student.e2e.t02.leader@fpt.edu.vn' },
    });
    log('invite busy member:', inv.status, inv.code);
  }
}

// 6. gd5-final-active accounts + gd1 event create target
const gd5 = bySlug('seal-gd5-final-active');
log('gd5-final-active:', gd5?.id, gd5?.status);
const gd5stu = await login('student.gd5.leader03@fpt.edu.vn', 'Student@dev1');
log('gd5 student login:', Boolean(gd5stu));

// 7. RBL progress on gd3 prelim
if (gd3) {
  const rounds = await call('GET', `/hackathons/${gd3.id}/rounds`, { token: coord });
  const rl = Array.isArray(rounds.data) ? rounds.data : rounds.data?.items || [];
  const prelim = rl.find((r) => !(r.isFinal || r.is_final));
  const rbl = await call('GET', `/rounds/${prelim?.id}/rbl/progress`, { token: coord });
  log('gd3 rbl progress:', rbl.status);
}

// 8. event create window on e2e-2026 (dry check dates only)
log('e2e eventStart =', e2e?.eventStart || e2e?.event_start, 'eventEnd =', e2e?.eventEnd || e2e?.event_end);
