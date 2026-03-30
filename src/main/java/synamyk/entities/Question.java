package synamyk.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_test_id", nullable = false)
    private SubTest subTest;

    /**
     * Optional section name, e.g. "1-часть: Математика"
     */
    @Column
    private String sectionName;

    @Column
    private String sectionNameKy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String textKy;

    @Column
    private String imageUrl;

    /**
     * Stored explanation used as fallback / context for AI analysis.
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String explanationKy;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer pointValue = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<AnswerOption> options = new ArrayList<>();
}