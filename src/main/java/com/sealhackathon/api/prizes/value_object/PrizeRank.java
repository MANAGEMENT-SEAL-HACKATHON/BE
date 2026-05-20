package com.sealhackathon.api.prizes.value_object;

/**
 * Hạng giải thưởng.
 *
 * <ul>
 *   <li>{@code FIRST}     — Giải Nhất</li>
 *   <li>{@code SECOND}    — Giải Nhì</li>
 *   <li>{@code THIRD}     — Giải Ba</li>
 *   <li>{@code HONORABLE} — Giải Khuyến khích</li>
 *   <li>{@code SPECIAL}   — Giải đặc biệt (Best Idea, Best UX, ...)</li>
 * </ul>
 */
public enum PrizeRank {
    FIRST,
    SECOND,
    THIRD,
    HONORABLE,
    SPECIAL
}
