package com.sealhackathon.api.teams.value_object;

/**
 * [BC-04] Hình thức gán đội vào Track.
 *
 * <ul>
 *   <li>{@code PREFERRED} — Đội tự chọn Track yêu thích (mùa Fall).</li>
 *   <li>{@code ASSIGNED}  — Coordinator gán Track sau bốc thăm (mùa Spring).</li>
 * </ul>
 */
public enum RegistrationType {
    PREFERRED,
    ASSIGNED
}
