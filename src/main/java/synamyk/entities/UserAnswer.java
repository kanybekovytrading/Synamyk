package synamyk.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_answer_selected_options",
            joinColumns = @JoinColumn(name = "user_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_option_id")
    )
    @Builder.Default
    private List<AnswerOption> selectedOptions = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSkipped = false;
}