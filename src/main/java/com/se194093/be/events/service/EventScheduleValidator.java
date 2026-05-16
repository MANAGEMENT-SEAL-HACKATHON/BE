package com.se194093.be.events.service;

import com.se194093.be.common.response.Warning;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.hackathons.entity.Hackathon;

import java.util.List;

/**
 * Validator 3 lớp cho lịch sự kiện (FR-06A).
 *
 * <ul>
 *   <li><b>Lớp 1</b> — block 422 {@code EVENT_OUT_OF_HACKATHON}:
 *       {@code starts_at >= hackathon.event_start} AND
 *       {@code ends_at <= hackathon.event_end + 1d buffer}.</li>
 *   <li><b>Lớp 2</b> — block 422 {@code EVENT_OVERLAP}: không 2 event cùng type
 *       KICKOFF/AWARDS overlap (PRESENTATION cho phép parallel khác location/meetUrl).</li>
 *   <li><b>Lớp 3</b> — WARN mềm (không block):
 *       <ul>
 *         <li>3a (chỉ check ở Gate FR-06, không ở đây): KICKOFF bắt buộc — ≥ 1 event type=KICKOFF</li>
 *         <li>3b: WORKSHOP.starts_at &lt;= registration_end</li>
 *         <li>3b: KICKOFF.starts_at ∈ [event_start, event_start + 1d]</li>
 *         <li>3c: PRESENTATION.starts_at &gt; KICKOFF.ends_at</li>
 *         <li>3c: AWARDS.starts_at &gt; max(PRESENTATION.starts_at)</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>Hành vi:
 * <ul>
 *   <li>{@link #validateBlocking}: throw BusinessRuleException cho Lớp 1, Lớp 2 — gọi TRƯỚC khi save.</li>
 *   <li>{@link #computeLayer3Warnings}: trả về list warning Lớp 3 — gắn vào response 201/200.</li>
 * </ul>
 */
public interface EventScheduleValidator {

    /**
     * Lớp 1+2. Throw {@code BusinessRuleException} nếu vi phạm.
     *
     * @param hackathon  parent
     * @param req        request POST/PUT — có {@code startsAt}, {@code endsAt}, {@code type}
     * @param excludeEventId nếu PUT, id event đang sửa để loại khỏi overlap check; truyền 0 cho POST
     */
    void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId);

    void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId);

    /**
     * Lớp 3 — Trả warning, KHÔNG throw. Service add vào response & audit WARNING_EVENT_ORDER.
     */
    List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req);

    List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req);
}
