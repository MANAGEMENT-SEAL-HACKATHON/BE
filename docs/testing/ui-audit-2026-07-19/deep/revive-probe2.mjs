const BE = 'http://localhost:8080/api/v1';
async function call(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${BE}${path}`, {
    method, headers, body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json, data: json?.data ?? json, code: json?.error?.code || json?.code };
}
async function login(email, password) {
  const r = await call('POST', '/auth/login', { body: { email, password } });
  return r.data?.accessToken;
}

const coord = await login('coord@fpt.edu.vn', 'Coordinator@dev1');
const mentor2 = await login('mentor2@fpt.edu.vn', 'Mentor@dev1');
const mentor3 = await login('mentor3@fpt.edu.vn', 'Mentor@dev1');

// correct endpoint: /me/mentor-track-assignments
for (const [name, tok] of [['mentor2', mentor2], ['mentor3', mentor3]]) {
  const ta = await call('GET', '/me/mentor-track-assignments', { token: tok });
  console.log(name, 'mentor-track-assignments:', ta.status, 'count =', (ta.data || []).length,
    JSON.stringify((ta.data || []).slice(0, 3)));
  const mr = await call('GET', '/me/mentor/rounds', { token: tok });
  console.log(name, 'mentor rounds:', mr.status, 'count =', (mr.data || []).length);
}

// team mentors for GD3-01
const hacks = await call('GET', '/hackathons?size=200', { token: coord });
const list = Array.isArray(hacks.data) ? hacks.data : hacks.data?.items || [];
const gd3 = list.find((h) => h.slug === 'seal-gd3-prelim-open');
const teams = await call('GET', `/teams?hackathonId=${gd3.id}&size=100`, { token: coord });
const tl = Array.isArray(teams.data) ? teams.data : teams.data?.items || [];
const t1 = tl.find((t) => /GD3-01/i.test(t.teamName || t.name || ''));
console.log('GD3-01 id =', t1?.id);
const tm = await call('GET', `/teams/${t1?.id}/mentors`, { token: coord });
console.log('team mentors:', tm.status, JSON.stringify(tm.data).slice(0, 400));

// student view: leader01 team mentors
const stu = await login('student.gd3.leader01@fpt.edu.vn', 'Student@dev1');
const tmStu = await call('GET', `/teams/${t1?.id}/mentors`, { token: stu });
console.log('team mentors (student):', tmStu.status, 'items =', (Array.isArray(tmStu.data) ? tmStu.data : tmStu.data?.items || []).length);

// hackathon-level tracks endpoint?
const ht = await call('GET', `/hackathons/${gd3.id}/tracks`, { token: coord });
console.log('hackathon tracks endpoint:', ht.status);

// queue current state on gd3 prelim
const rounds = await call('GET', `/hackathons/${gd3.id}/rounds`, { token: coord });
const rl = Array.isArray(rounds.data) ? rounds.data : rounds.data?.items || [];
const prelim = rl.find((r) => !(r.isFinal || r.is_final));
const tr = await call('GET', `/rounds/${prelim.id}/tracks`, { token: coord });
const trl = Array.isArray(tr.data) ? tr.data : tr.data?.items || [];
const q = await call('GET', `/presentation/queue?roundId=${prelim.id}&trackId=${trl[0].id}`, { token: coord });
console.log('queue track1:', q.status, JSON.stringify(q.data).slice(0, 300));

// gd1-incomplete details
const gd1 = list.find((h) => h.slug === 'seal-gd1-incomplete');
console.log('gd1-incomplete:', gd1?.id, gd1?.status, 'eventStart =', gd1?.eventStart || gd1?.event_start);

// gd4-tiebreak-manual results page prereqs
const tb = list.find((h) => h.slug === 'seal-gd4-tiebreak-manual');
const tbRounds = await call('GET', `/hackathons/${tb.id}/rounds`, { token: coord });
const tbrl = Array.isArray(tbRounds.data) ? tbRounds.data : tbRounds.data?.items || [];
const tbPrelim = tbrl.find((r) => !(r.isFinal || r.is_final));
console.log('tb-manual prelim:', tbPrelim?.id, 'locked =', tbPrelim?.scoringLocked, 'published =', tbPrelim?.isPublished);
