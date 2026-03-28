package com.cerebro.finalproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz")
public class Quiz {

    // ── Status enum ─────────────────────────────────────────────────────────

    public enum QuizStatus {
        DRAFT,   // Not yet deployed; invisible to students
        ACTIVE   // Deployed; students can see and attempt it
    }

    // ── Fields ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Legacy visibility flag. True when status == ACTIVE. */
    @Column(nullable = false)
    private Boolean published = false;

    /** Draft-vs-deployed state. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizStatus status = QuizStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "class_room_id")
    private Classroom classRoom;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "total_points")
    private Double totalPoints = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Minutes a student has after opening the quiz. NULL = no limit. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    /** Students cannot start or submit after this time. NULL = no deadline. */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /**
     * Controls whether students see correct/wrong answer feedback
     * after submitting. false = score only (default).
     */
    @Column(name = "show_answers", nullable = false)
    private Boolean showAnswers = false;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Attempt> attempts = new ArrayList<>();

    // ── Convenience helpers ──────────────────────────────────────────────────

    /** Returns true when this quiz has been deployed and students can see it. */
    public boolean isDraft()  { return status == QuizStatus.DRAFT; }
    public boolean isActive() { return status == QuizStatus.ACTIVE; }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public QuizStatus getStatus() { return status; }
    public void setStatus(QuizStatus status) { this.status = status; }

    public Classroom getClassRoom() { return classRoom; }
    public void setClassRoom(Classroom classRoom) { this.classRoom = classRoom; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Double totalPoints) { this.totalPoints = totalPoints; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Boolean getShowAnswers() { return showAnswers != null && showAnswers; }
    public void setShowAnswers(Boolean showAnswers) { this.showAnswers = showAnswers != null && showAnswers; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public List<Attempt> getAttempts() { return attempts; }
    public void setAttempts(List<Attempt> attempts) { this.attempts = attempts; }
}