package com.sealhackathon.api.events.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buffet_menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuffetMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "display_order")
    private Integer displayOrder;
}
