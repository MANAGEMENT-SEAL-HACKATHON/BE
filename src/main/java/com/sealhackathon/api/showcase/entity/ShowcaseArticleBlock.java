package com.sealhackathon.api.showcase.entity;

import com.sealhackathon.api.showcase.value_object.ShowcaseBlockType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "showcase_article_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowcaseArticleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private ShowcaseArticle article;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ShowcaseBlockType type;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "image_key", length = 512)
    private String imageKey;
}
