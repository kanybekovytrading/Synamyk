package synamyk.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "answer_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AnswerOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** A, Б, В, Д */
    @Column(nullable = false, length = 5)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String textKy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}