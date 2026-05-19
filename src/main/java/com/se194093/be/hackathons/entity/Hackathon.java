package com.se194093.be.hackathons.entity;

import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hackathons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hackathon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "season", nullable = false, length = 20)
    private Season season;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HackathonStatus status = HackathonStatus.DRAFT;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "registration_start")
    private LocalDate registrationStart;

    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    @Column(name = "event_start")
    private LocalDate eventStart;

    @Column(name = "event_end")
    private LocalDate eventEnd;

    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    @Column(name = "individual_ranking_enabled", nullable = false)
    private Boolean individualRankingEnabled = false;

    @Column(name = "chapter_scoring_formula", columnDefinition = "TEXT")
    private String chapterScoringFormula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
