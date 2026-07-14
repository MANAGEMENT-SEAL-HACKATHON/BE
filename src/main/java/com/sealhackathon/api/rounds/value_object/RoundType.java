package com.sealhackathon.api.rounds.value_object;

/**
 * [BC-01] Phân loại Round trong Hackathon.
 *
 * <ul>
 *   <li>{@code PRELIMINARY} — Sơ loại, có Track con</li>
 *   <li>{@code SEMIFINAL}   — Bán kết, có Track con</li>
 *   <li>{@code FINAL}       — Chung kết, KHÔNG có Track con; Judge EXTERNAL (FINAL_EXTERNAL) + optional HEAD INTERNAL</li>
 * </ul>
 *
 * <p>Constraint nhất quán: {@code is_final = TRUE} ↔ {@code round_type = FINAL}.
 * Enforce ở DB (CHECK constraint) + trigger {@code trg_prevent_track_in_final_round}.
 */
public enum RoundType {
    PRELIMINARY,
    SEMIFINAL,
    FINAL
}
