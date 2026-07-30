package com.sealhackathon.api.appeals.entity;

import com.sealhackathon.api.appeals.value_object.AppealEvidenceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appeal_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appeal_id", nullable = false)
    private Appeal appeal;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AppealEvidenceType type;

    @Column(name = "caption", length = 500)
    private String caption;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
