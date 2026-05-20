package com.sealhackathon.api.tracks.entity;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * [BC-02] Track là CON của Round Sơ loại/Bán kết (đảo FK so với v2.1).
 *
 * <p>Round Chung kết ({@code round.isFinal = TRUE}) KHÔNG có Track con.
 * Trigger {@code trg_prevent_track_in_final_round} chặn việc tạo Track gán vào Round FINAL.
 *
 * <p>Xem chi tiết: {@code docs/db/schema-v3.0-mysql.md} §2 (tracks).
 */
@Entity
@Table(
        name = "tracks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tracks_round_sequence",
                        columnNames = {"round_id", "sequence_order"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // [BC-02] FK đảo: hackathon_id -> round_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * [BC-02] Chủ đề bốc thăm của Track trong Round (vd "Business Analysis App").
     * Set sau bốc thăm tại Khai mạc.
     */
    @Column(name = "topic", length = 300)
    private String topic;

    @Column(name = "max_teams")
    private Integer maxTeams;

    /**
     * [FIX-02] Số đội tối đa mỗi bảng đấu (assigned_group).
     * Ví dụ: Fall 2025 = 6, Spring 2026 = 8.
     */
    @Column(name = "max_teams_per_group")
    private Integer maxTeamsPerGroup;

    @Builder.Default
    @Column(name = "min_team_size", nullable = false)
    private Integer minTeamSize = 3;

    @Builder.Default
    @Column(name = "max_team_size", nullable = false)
    private Integer maxTeamSize = 5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TrackStatus status = TrackStatus.OPEN;

    /**
     * [BC-02] Thứ tự Track trong Round. UNIQUE(round_id, sequence_order).
     */
    @Builder.Default
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 1;

    /**
     * [BC-02 — back-compat shim]
     *
     * <p>Track không còn FK trực tiếp tới Hackathon ở v3.0
     * (đi qua Round: {@code track.round.hackathon}). Method này giữ lại
     * tạm thời để service-layer/mapper cũ vẫn compile mà không phải sửa ngay.
     *
     * @deprecated dùng {@code track.getRound().getHackathon()} thay cho {@code track.getHackathon()}.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    @Transient
    public Hackathon getHackathon() {
        return round == null ? null : round.getHackathon();
    }
}
