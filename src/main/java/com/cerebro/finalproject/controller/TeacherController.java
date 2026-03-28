package com.cerebro.finalproject.controller;

import com.cerebro.finalproject.model.*;
import com.cerebro.finalproject.repository.AnswerRepository;
import com.cerebro.finalproject.repository.UserRepository;
import com.cerebro.finalproject.security.CustomUserDetails;
import com.cerebro.finalproject.service.ClassroomService;
import com.cerebro.finalproject.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired private ClassroomService classroomService;
    @Autowired private QuizService quizService;
    @Autowired private UserRepository userRepository;
    @Autowired private AnswerRepository answerRepository;

    // ── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping
    public String teacherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        User teacher = userRepository.findByIdWithTeacherClasses(userDetails.getUser().getId())
                .orElse(userDetails.getUser());
        List<Classroom> classes = classroomService.findByTeacherId(teacher.getId());
        model.addAttribute("classes", classes);
        return "teacher";
    }

    // ── Classroom ─────────────────────────────────────────────────────────────

    @PostMapping("/create")
    public String createClass(@RequestParam("name") String name,
                              @RequestParam(value = "banner", required = false) MultipartFile banner,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        User teacher = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (teacher != null) classroomService.createClass(name, teacher, banner);
        return "redirect:/teacher";
    }

    @GetMapping("/class/{id}")
    public String viewClass(@PathVariable Long id, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findByIdWithStudents(id);
        if (classroomOpt.isEmpty()) return "redirect:/teacher";

        Classroom classroom = classroomOpt.get();
        List<Quiz> quizzes = quizService.findByClassRoomId(id);

        List<User> students = new ArrayList<>(classroom.getStudents());
        students.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("students", students);
        return "teacher_classlist";
    }

    @PostMapping("/class/{classId}/remove-student/{studentId}")
    public String removeStudent(@PathVariable Long classId,
                                @PathVariable Long studentId,
                                RedirectAttributes ra) {
        classroomService.removeStudent(classId, studentId);
        ra.addFlashAttribute("success", "Student removed from classroom.");
        return "redirect:/teacher/class/" + classId;
    }

    // ── Quiz — create ─────────────────────────────────────────────────────────

    @GetMapping("/class/{classId}/create_quiz")
    public String showCreateQuizForm(@PathVariable Long classId, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) return "redirect:/teacher";
        model.addAttribute("classRoom", classroomOpt.get());
        model.addAttribute("quiz", new Quiz());
        return "create_quiz";
    }

    @PostMapping("/class/{classId}/save_quiz")
    public String saveQuiz(@PathVariable Long classId,
                           @RequestParam("title") String title,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "timeLimitMinutes", required = false) Integer timeLimitMinutes,
                           @RequestParam(value = "deadline", required = false) String deadlineStr,
                           @RequestParam(value = "showAnswers", required = false) Boolean showAnswers) {

        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) return "redirect:/teacher";

        // NOTE: teacher is not needed by the form but kept for record
        // We use a dummy lookup to stay consistent with the existing pattern.
        // If you need teacher info, inject @AuthenticationPrincipal here.
        Quiz quiz = quizService.createQuiz(
                title, description, classroomOpt.get(), null,
                sanitizeTimeLimit(timeLimitMinutes),
                parseDeadline(deadlineStr),
                showAnswers != null && showAnswers
        );
        return "redirect:/teacher/quiz/" + quiz.getId() + "/edit";
    }

    // ── Quiz — edit ───────────────────────────────────────────────────────────

    @GetMapping("/quiz/{id}/edit")
    public String editQuiz(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) return "redirect:/teacher";
        model.addAttribute("quiz", quizOpt.get());
        return "teacher_quizedit";
    }

    @PostMapping("/quiz/{id}/update")
    public String updateQuiz(@PathVariable Long id,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "timeLimitMinutes", required = false) Integer timeLimitMinutes,
                             @RequestParam(value = "deadline", required = false) String deadlineStr,
                             @RequestParam(value = "showAnswers", required = false) Boolean showAnswers) {

        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) return "redirect:/teacher";

        Quiz quiz = quizOpt.get();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setTimeLimitMinutes(sanitizeTimeLimit(timeLimitMinutes));
        quiz.setDeadline(parseDeadline(deadlineStr));
        quiz.setShowAnswers(showAnswers != null && showAnswers);
        quizService.updateQuiz(quiz);

        return "redirect:/teacher/quiz/" + id + "/edit";
    }

    // ── Quiz — deploy / retract ───────────────────────────────────────────────

    /**
     * Deploys a DRAFT quiz so students can see and attempt it.
     */
    @PostMapping("/quiz/{id}/deploy")
    public String deployQuiz(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Quiz deployed = quizService.deployQuiz(id);
            ra.addFlashAttribute("success",
                    "\"" + deployed.getTitle() + "\" is now live! Students can see and attempt it.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not deploy quiz: " + e.getMessage());
        }
        return "redirect:/teacher/quiz/" + id + "/edit";
    }

    /**
     * Retracts an ACTIVE quiz back to DRAFT (hidden from students).
     */
    @PostMapping("/quiz/{id}/retract")
    public String retractQuiz(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Quiz retracted = quizService.retractQuiz(id);
            ra.addFlashAttribute("success",
                    "\"" + retracted.getTitle() + "\" has been retracted to draft.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not retract quiz: " + e.getMessage());
        }
        return "redirect:/teacher/quiz/" + id + "/edit";
    }

    // ── Quiz — show/hide answers toggle ──────────────────────────────────────

    @PostMapping("/quiz/{id}/toggle-answers")
    public String toggleShowAnswers(@PathVariable Long id, RedirectAttributes ra) {
        Quiz updated = quizService.toggleShowAnswers(id);
        String state = updated.getShowAnswers() ? "visible to students" : "hidden from students";
        ra.addFlashAttribute("success", "Answer feedback is now " + state + ".");
        return "redirect:/teacher/quiz/" + id + "/results";
    }

    // ── Quiz — delete ─────────────────────────────────────────────────────────

    @GetMapping("/quiz/{id}/delete")
    public String deleteQuiz(@PathVariable Long id) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isPresent()) {
            Long classId = quizOpt.get().getClassRoom().getId();
            quizService.deleteQuiz(id);
            return "redirect:/teacher/class/" + classId;
        }
        return "redirect:/teacher";
    }

    // ── Quiz — questions ──────────────────────────────────────────────────────

    @PostMapping("/quiz/{quizId}/add_question")
    public String addQuestion(@PathVariable Long quizId,
                              @RequestParam("type") String typeStr,
                              @RequestParam("text") String text,
                              @RequestParam(value = "correct", required = false) String correct,
                              @RequestParam(value = "points", required = false) Double points,
                              @RequestParam(value = "choice1", required = false) String choice1,
                              @RequestParam(value = "choice2", required = false) String choice2,
                              @RequestParam(value = "choice3", required = false) String choice3,
                              @RequestParam(value = "choice4", required = false) String choice4) {

        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) return "redirect:/teacher";

        Quiz quiz = quizOpt.get();
        Question.QuestionType type = Question.QuestionType.valueOf(typeStr);
        Double qPoints = (points != null && points > 0) ? points : 1.0;

        if (type == Question.QuestionType.MCQ) {
            quizService.addQuestionWithChoices(quiz, text,
                    List.of(
                            choice1 != null ? choice1 : "",
                            choice2 != null ? choice2 : "",
                            choice3 != null ? choice3 : "",
                            choice4 != null ? choice4 : ""
                    ), correct, qPoints);
        } else {
            quizService.addQuestion(quiz, type, text, correct, qPoints);
        }

        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{quizId}/question/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long quizId,
                                 @PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    // ── Quiz — results ────────────────────────────────────────────────────────

    @GetMapping("/quiz/{id}/results")
    public String viewQuizResults(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) return "redirect:/teacher";

        Quiz quiz = quizOpt.get();
        List<Attempt> attempts = quizService.getQuizAttempts(id);
        double averageScore = quizService.calculateAverageScore(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        model.addAttribute("averageScore", averageScore);

        double maxScore = 0, minScore = quiz.getTotalPoints() != null ? quiz.getTotalPoints() : 0;
        long excellentCount = 0, goodCount = 0, averageCount = 0, poorCount = 0;
        double totalPoints = quiz.getTotalPoints() != null ? quiz.getTotalPoints() : 0;

        if (!attempts.isEmpty() && totalPoints > 0) {
            for (Attempt a : attempts) {
                if (a.getScore() == null) continue;
                if (a.getScore() > maxScore) maxScore = a.getScore();
                if (a.getScore() < minScore) minScore = a.getScore();
                double pct = (a.getScore() / totalPoints) * 100;
                if (pct >= 90) excellentCount++;
                else if (pct >= 80) goodCount++;
                else if (pct >= 70) averageCount++;
                else poorCount++;
            }
        }

        model.addAttribute("maxScore", maxScore);
        model.addAttribute("minScore", attempts.isEmpty() ? 0.0 : minScore);
        model.addAttribute("excellentCount", excellentCount);
        model.addAttribute("goodCount", goodCount);
        model.addAttribute("averageCount", averageCount);
        model.addAttribute("poorCount", poorCount);

        return "teacher_insidequiz_result";
    }

    // ── Attempt review & essay grading ────────────────────────────────────────

    @GetMapping("/attempt/{attemptId}/review")
    public String reviewAttempt(@PathVariable Long attemptId, Model model,
                                RedirectAttributes ra) {
        Optional<Attempt> attemptOpt = quizService.getAttemptById(attemptId);
        if (attemptOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Attempt not found");
            return "redirect:/teacher";
        }

        Attempt attempt = attemptOpt.get();
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);

        long pendingEssays = answers.stream()
                .filter(a -> a.getQuestion().getType() == Question.QuestionType.ESSAY
                        && a.getEssayScore() == null)
                .count();

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", answers);
        model.addAttribute("quiz", attempt.getQuiz());
        model.addAttribute("pendingEssays", pendingEssays);
        return "teacher_review_attempt";
    }

    @PostMapping("/answer/{answerId}/grade")
    public String gradeEssayAnswer(@PathVariable Long answerId,
                                   @RequestParam("score") Double score,
                                   @RequestParam("attemptId") Long attemptId,
                                   RedirectAttributes ra) {
        try {
            quizService.gradeEssayAnswer(answerId, score);
            ra.addFlashAttribute("success", "Essay graded successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Grading failed: " + e.getMessage());
        }
        return "redirect:/teacher/attempt/" + attemptId + "/review";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDateTime parseDeadline(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (Exception e) { return null; }
    }

    private Integer sanitizeTimeLimit(Integer v) {
        return (v != null && v > 0) ? v : null;
    }
}