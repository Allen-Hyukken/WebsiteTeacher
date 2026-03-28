package com.cerebro.finalproject.service;

import com.cerebro.finalproject.model.*;
import com.cerebro.finalproject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizService {

    @Autowired private QuizRepository quizRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ChoiceRepository choiceRepository;
    @Autowired private AttemptRepository attemptRepository;
    @Autowired private AnswerRepository answerRepository;

    // ── Quiz CRUD ────────────────────────────────────────────────────────────

    /** Legacy overload – keeps existing call sites working. */
    public Quiz createQuiz(String title, String description,
                           Classroom classroom, User teacher) {
        return createQuiz(title, description, classroom, teacher, null, null, false);
    }

    public Quiz createQuiz(String title, String description,
                           Classroom classroom, User teacher,
                           Integer timeLimitMinutes, LocalDateTime deadline) {
        return createQuiz(title, description, classroom, teacher, timeLimitMinutes, deadline, false);
    }

    /**
     * Creates a new quiz in DRAFT status (invisible to students).
     * Teacher must explicitly deploy it to make it accessible.
     */
    public Quiz createQuiz(String title, String description,
                           Classroom classroom, User teacher,
                           Integer timeLimitMinutes, LocalDateTime deadline,
                           Boolean showAnswers) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setClassRoom(classroom);
        quiz.setTeacher(teacher);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setTotalPoints(0.0);
        quiz.setTimeLimitMinutes(timeLimitMinutes);
        quiz.setDeadline(deadline);
        quiz.setShowAnswers(showAnswers != null && showAnswers);
        // New quizzes always start as DRAFT
        quiz.setStatus(Quiz.QuizStatus.DRAFT);
        quiz.setPublished(false);
        return quizRepository.save(quiz);
    }

    public Optional<Quiz> findById(Long id) {
        return quizRepository.findById(id);
    }

    public List<Quiz> findByClassRoomId(Long classRoomId) {
        return quizRepository.findByClassRoomId(classRoomId);
    }

    public Quiz updateQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }

    /**
     * Deploys a DRAFT quiz so that students can see and attempt it.
     * Sets status = ACTIVE and published = true.
     */
    @Transactional
    public Quiz deployQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found: " + quizId));
        quiz.setStatus(Quiz.QuizStatus.ACTIVE);
        quiz.setPublished(true);
        return quizRepository.save(quiz);
    }

    /**
     * Retracts a deployed quiz back to DRAFT (hides it from students).
     */
    @Transactional
    public Quiz retractQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found: " + quizId));
        quiz.setStatus(Quiz.QuizStatus.DRAFT);
        quiz.setPublished(false);
        return quizRepository.save(quiz);
    }

    /** Flips the showAnswers flag and returns the updated quiz. */
    @Transactional
    public Quiz toggleShowAnswers(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quiz.setShowAnswers(!quiz.getShowAnswers());
        return quizRepository.save(quiz);
    }

    // ── Questions ────────────────────────────────────────────────────────────

    @Transactional
    public Question addQuestion(Quiz quiz, Question.QuestionType type,
                                String text, String correctAnswer) {
        return addQuestion(quiz, type, text, correctAnswer, 1.0);
    }

    @Transactional
    public Question addQuestion(Quiz quiz, Question.QuestionType type,
                                String text, String correctAnswer, Double points) {
        Question q = new Question();
        q.setQuiz(quiz);
        q.setType(type);
        q.setText(text);
        q.setCorrectAnswer(correctAnswer);
        q.setQIndex(quiz.getQuestions().size());
        q.setPoints(points != null ? points : 1.0);
        Question saved = questionRepository.save(q);
        updateQuizTotalPoints(quiz.getId());
        return saved;
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text,
                                           List<String> choiceTexts,
                                           String correctChoiceText) {
        return addQuestionWithChoices(quiz, text, choiceTexts, correctChoiceText, 1.0);
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text,
                                           List<String> choiceTexts,
                                           String correctChoiceText, Double points) {
        Question q = new Question();
        q.setQuiz(quiz);
        q.setType(Question.QuestionType.MCQ);
        q.setText(text);
        q.setQIndex(quiz.getQuestions().size());
        q.setPoints(points != null ? points : 1.0);
        q = questionRepository.save(q);

        for (String choiceText : choiceTexts) {
            if (choiceText != null && !choiceText.trim().isEmpty()) {
                Choice c = new Choice();
                c.setQuestion(q);
                c.setText(choiceText.trim());
                c.setCorrect(choiceText.trim().equalsIgnoreCase(
                        correctChoiceText != null ? correctChoiceText.trim() : ""));
                choiceRepository.save(c);
            }
        }
        updateQuizTotalPoints(quiz.getId());
        return q;
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Optional<Question> opt = questionRepository.findById(questionId);
        if (opt.isPresent()) {
            Long quizId = opt.get().getQuiz().getId();
            questionRepository.deleteById(questionId);
            questionRepository.flush();
            updateQuizTotalPoints(quizId);
        }
    }

    private void updateQuizTotalPoints(Long quizId) {
        quizRepository.updateTotalPoints(quizId);
    }

    // ── Attempts & Submission ─────────────────────────────────────────────────

    @Transactional
    public Attempt submitQuiz(Quiz quiz, User student,
                              Map<String, String> answers) {
        Attempt attempt = new Attempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt = attemptRepository.save(attempt);

        double totalScore = 0;

        for (Question question : quiz.getQuestions()) {
            String givenAnswer = answers.get("q_" + question.getId());

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;
            double qPoints = question.getPoints() != null ? question.getPoints() : 1.0;

            switch (question.getType()) {
                case MCQ:
                    if (givenAnswer != null && !givenAnswer.trim().isEmpty()) {
                        try {
                            Long choiceId = Long.parseLong(givenAnswer);
                            Optional<Choice> choiceOpt = choiceRepository.findById(choiceId);
                            if (choiceOpt.isPresent()) {
                                Choice selected = choiceOpt.get();
                                answer.setChoice(selected);
                                isCorrect = selected.getCorrect();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    break;

                case TF:
                case IDENT:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = normalize(givenAnswer).equals(normalize(question.getCorrectAnswer()));
                    }
                    break;

                case CODING:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = compareCode(givenAnswer, question.getCorrectAnswer());
                    }
                    break;

                case ESSAY:
                    answer.setGivenText(givenAnswer);
                    isCorrect = false; // requires manual grading
                    break;
            }

            answer.setCorrect(isCorrect);
            answerRepository.save(answer);
            if (isCorrect) totalScore += qPoints;
        }

        attempt.setScore(totalScore);
        return attemptRepository.save(attempt);
    }

    public List<Attempt> getQuizAttempts(Long quizId) {
        return attemptRepository.findByQuizId(quizId);
    }

    public double calculateAverageScore(Long quizId) {
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);
        if (attempts.isEmpty()) return 0.0;

        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isEmpty()) return 0.0;

        double totalPoints = quizOpt.get().getTotalPoints();
        if (totalPoints == 0) return 0.0;

        double sum = attempts.stream()
                .mapToDouble(a -> (a.getScore() != null ? a.getScore() : 0.0) / totalPoints * 100)
                .sum();
        return sum / attempts.size();
    }

    public boolean hasStudentAttempted(Long quizId, Long studentId) {
        return attemptRepository.existsByQuizIdAndStudentId(quizId, studentId);
    }

    public Optional<Attempt> getStudentLatestAttempt(Long quizId, Long studentId) {
        return attemptRepository.findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, studentId);
    }

    public Optional<Attempt> getAttemptById(Long attemptId) {
        return attemptRepository.findById(attemptId);
    }

    // ── Essay Grading ─────────────────────────────────────────────────────────

    @Transactional
    public void gradeEssayAnswer(Long answerId, Double score) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found: " + answerId));

        Question question = answer.getQuestion();
        if (question.getType() != Question.QuestionType.ESSAY) {
            throw new RuntimeException("Only ESSAY answers can be manually graded");
        }

        double maxPoints = question.getPoints() != null ? question.getPoints() : 1.0;
        if (score < 0 || score > maxPoints) {
            throw new RuntimeException("Score must be between 0 and " + maxPoints);
        }

        answer.applyEssayGrade(score);
        answerRepository.save(answer);
        recalculateAttemptScore(answer.getAttempt().getId());
    }

    @Transactional
    public void recalculateAttemptScore(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) return;

        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        double total = 0.0;

        for (Answer ans : answers) {
            Question q = ans.getQuestion();
            double qPoints = q.getPoints() != null ? q.getPoints() : 1.0;

            if (q.getType() == Question.QuestionType.ESSAY) {
                if (ans.getEssayScore() != null) total += ans.getEssayScore();
            } else {
                if (Boolean.TRUE.equals(ans.getCorrect())) total += qPoints;
            }
        }

        attempt.setScore(total);
        attemptRepository.save(attempt);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private boolean compareCode(String student, String correct) {
        return normalizeCode(student).equals(normalizeCode(correct));
    }

    private String normalizeCode(String code) {
        if (code == null) return "";
        code = code.replaceAll("//.*?(\r?\n|$)", "\n");
        code = code.replaceAll("/\\*.*?\\*/", "");
        code = code.replaceAll("[ \\t]+", " ");
        code = code.replaceAll("\\s*([{};(),=+\\-*/<>!&|])\\s*", "$1");
        String[] lines = code.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) sb.append(t);
        }
        return sb.toString().toLowerCase();
    }
}