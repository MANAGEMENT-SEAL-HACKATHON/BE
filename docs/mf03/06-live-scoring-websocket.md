# MF-03 — Live Scoring WebSocket (FR-18A)

**Stack:** STOMP over SockJS · endpoint `/ws` · JWT trên CONNECT header.

**Fallback:** REST `GET /rounds/{id}/ranking/preview` và `GET /rounds/{id}/scoring-progress` (polling).

---

## 1. Kết nối (FE)

```bash
npm install @stomp/stompjs sockjs-client
```

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const token = localStorage.getItem('accessToken');

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe(`/topic/rounds/${roundId}/leaderboard-preview`, onLeaderboard);
    client.subscribe(`/topic/rounds/${roundId}/scoring-progress`, onProgress);
    client.subscribe(`/topic/tracks/${trackId}/score-saved`, onScoreEcho);
  },
});

client.activate();
```

**Dev (`security.jwt.enabled=false`):** CONNECT không cần token — server gắn Coordinator stub.

---

## 2. Topics

| Topic | Payload | Ai subscribe |
|-------|---------|--------------|
| `/topic/rounds/{roundId}/leaderboard-preview` | `RoundRankingItemResponse[]` | COORDINATOR; JUDGE assigned round |
| `/topic/rounds/{roundId}/scoring-progress` | `RoundScoringProgressResponse` | COORDINATOR; JUDGE assigned round |
| `/topic/tracks/{trackId}/score-saved` | `ScoreResponse` | COORDINATOR; JUDGE assigned track |

Broadcast leaderboard/progress **debounce 300ms** sau mỗi `POST /scores`.

---

## 3. Luồng Judge (live autosave)

1. `GET /submissions?roundId=` — danh sách bài
2. `POST /scores` — upsert nháp (`is_final=false`)
3. Nhận echo trên `/topic/tracks/{trackId}/score-saved`
4. Sau `PATCH /rounds/{id}/lock-scoring` → `POST /scores` trả **423** `SCORING_LOCKED`

---

## 4. Token refresh

Access token hết hạn (~30 phút): disconnect STOMP → refresh JWT → CONNECT lại với header mới.

---

## 5. CORS / origins

Allowed: `http://localhost:5173`, `https://seal-hackathon-fe.vercel.app` (cấu hình trong `WebSocketConfig`).
