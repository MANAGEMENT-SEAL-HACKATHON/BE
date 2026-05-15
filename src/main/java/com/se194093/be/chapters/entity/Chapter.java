package com.se194093.be.chapters.entity;

import com.se194093.be.chapters.value_object.ChapterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Ví dụ:
     * - FPT-HCM
     * - HUST
     */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "university", length = 300)
    private String university;

    @Column(name = "city", length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChapterStatus status = ChapterStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}