package com.cerebro.finalproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne
    @JoinColumn(name = "choice_id")
    private Choice choice;

    /** Raw text answer for IDENT, TF, ESSAY, CODING questions. */
    @Column(name = "given_text", columnDefinition = "TEXT")
    private String givenText;

    @Column(nullable = false)
    private Boolean correct = false;

    /**
     * Manual score for ESSAY questions, stored in its own column.
     * NULL means not yet graded.
     */
    @Column(name = "essay_score")
    private Double essayScore;

    // ── Getters & Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Attempt getAttempt() { return attempt; }
    public void setAttempt(Attempt attempt) { this.attempt = attempt; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public Choice getChoice() { return choice; }
    public void setChoice(Choice choice) { this.choice = choice; }

    public String getGivenText() { return givenText; }
    public void setGivenText(String givenText) { this.givenText = givenText; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Double getEssayScore() { return essayScore; }
    public void setEssayScore(Double essayScore) { this.essayScore = essayScore; }

    // ── Thymeleaf helpers ───────────────────────────────────────────────────

    /**
     * Returns the student's actual essay text.
     * Also handles the legacy "ESSAY_SCORE:x|||text" encoding
     * that may exist in rows saved by older code.
     */
    public String getActualEssayText() {
        if (givenText == null) return "";
        if (givenText.startsWith("ESSAY_SCORE:")) {
            int sep = givenText.indexOf("|||");
            return sep >= 0 ? givenText.substring(sep + 3) : givenText;
        }
        return givenText;
    }

    /**
     * Applies a teacher's manual grade to this essay answer.
     * Marks correct=true so it counts toward the attempt score.
     */
    public void applyEssayGrade(Double score) {
        this.essayScore = score;
        this.correct = (score != null && score > 0);
    }
}