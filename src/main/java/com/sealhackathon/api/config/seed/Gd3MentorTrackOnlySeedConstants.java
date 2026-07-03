package com.sealhackathon.api.config.seed;

/** GĐ3 — mentor có track assignment, chưa có mentor-team (FR-M-05 bootstrap). */
public final class Gd3MentorTrackOnlySeedConstants {

    private Gd3MentorTrackOnlySeedConstants() {
    }

    public static final String SLUG_GD3_MENTOR_TRACK_ONLY = "seal-gd3-mentor-track-only";

    /** Mentor riêng — không dùng chung mentor@ để probe track-only không bị nhiễu từ slug khác. */
    public static final String EMAIL_MENTOR_TRACK_ONLY = "mentor.trackonly@fpt.edu.vn";
}
