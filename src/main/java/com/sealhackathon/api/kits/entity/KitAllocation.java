package com.sealhackathon.api.kits.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "kit_allocations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kit_alloc_hackathon_user_item",
                columnNames = {"hackathon_id", "user_id", "kit_item_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kit_item_id", nullable = false)
    private KitItem kitItem;

    @Column(name = "size", length = 10)
    private String size;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KitAllocationStatus status = KitAllocationStatus.PENDING;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Column(name = "note", length = 1000)
    private String note;
}
