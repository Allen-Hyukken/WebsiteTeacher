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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @GetMapping
    public String teacherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User teacher = userRepository.findByIdWithTeacherClasses(userDetails.getUser().getId())
                .orElse(userDetails.getUser());
        List<Classroom> classes = classroomService.findByTeacherId(teacher.getId());
        model.addAttribute("classes", classes);
        return "teacher";
    }

    @PostMapping("/create")
    public String createClass(@RequestParam("name") String name,
                              @RequestParam(value = "banner", required = false) MultipartFile banner,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        User teacher = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (teacher != null) {
            classroomService.createClass(name, teacher, banner);
        }
        return "redirect:/teacher";
    }

    @GetMapping("/class/{id}")
    public String viewClass(@PathVariable Long id, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(id);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Classroom classroom = classroomOpt.get();
        List<Quiz> quizzes = quizService.findByClassRoomId(id);

        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizzes);
        return "teacher_classlist";
    }

    @GetMapping("/class/{classId}/create_quiz")
    public String showCreateQuizForm(@PathVariable Long classId, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        model.addAttribute("classRoom", classroomOpt.get());
        model.addAttribute("quiz", new Quiz());
        return "create_quiz";
    }

    @PostMapping("/class/{classId}/save_quiz")
    public String saveQuiz(@PathVariable Long classId,
                           @RequestParam("title") String title,
                           @RequestParam(value = "description", required = false) String description,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {

        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        User teacher = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (teacher == null) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizService.createQuiz(title, description, classroomOpt.get(), teacher);
        return "redirect:/teacher/quiz/" + quiz.getId() + "/edit";
    }

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
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        Question.QuestionType type = Question.QuestionType.valueOf(typeStr);
        Double questionPoints = (points != null && points > 0) ? points : 1.0;

        if (type == Question.QuestionType.MCQ) {
            List<String> choices = List.of(choice1, choice2, choice3, choice4);
            quizService.addQuestionWithChoices(quiz, text, choices, correct, questionPoints);
        } else {
            quizService.addQuestion(quiz, type, text, correct, questionPoints);
        }

        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{id}/edit")
    public String editQuiz(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        model.addAttribute("quiz", quizOpt.get());
        return "teacher_quizedit";
    }

    @PostMapping("/quiz/{id}/update")
    public String updateQuiz(@PathVariable Long id,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description) {

        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quizService.updateQuiz(quiz);

        return "redirect:/teacher/class/" + quiz.getClassRoom().getId();
    }

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

    @GetMapping("/quiz/{quizId}/question/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{id}/results")
    public String viewQuizResults(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        List<Attempt> attempts = quizService.getQuizAttempts(id);
        double averageScore = quizService.calculateAverageScore(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        model.addAttribute("averageScore", averageScore);

        if (!attempts.isEmpty() && quiz.getTotalPoints() != null && quiz.getTotalPoints() > 0) {
            double totalPoints = quiz.getTotalPoints();
            double maxScore = 0.0;
            double minScore = totalPoints;

            for (Attempt attempt : attempts) {
                if (attempt.getScore() != null) {
                    if (attempt.getScore() > maxScore) maxScore = attempt.getScore();
                    if (attempt.getScore() < minScore) minScore = attempt.getScore();
                }
            }

            long excellentCount = 0, goodCount = 0, averageCount = 0, poorCount = 0;
            for (Attempt attempt : attempts) {
                if (attempt.getScore() != null) {
                    double percentage = (attempt.getScore() / totalPoints) * 100;
                    if (percentage >= 90) excellentCount++;
                    else if (percentage >= 80) goodCount++;
                    else if (percentage >= 70) averageCount++;
                    else poorCount++;
                }
            }

            model.addAttribute("maxScore", maxScore);
            model.addAttribute("minScore", minScore);
            model.addAttribute("excellentCount", excellentCount);
            model.addAttribute("goodCount", goodCount);
            model.addAttribute("averageCount", averageCount);
            model.addAttribute("poorCount", poorCount);
        } else {
            model.addAttribute("maxScore", 0.0);
            model.addAttribute("minScore", 0.0);
            model.addAttribute("excellentCount", 0L);
            model.addAttribute("goodCount", 0L);
            model.addAttribute("averageCount", 0L);
            model.addAttribute("poorCount", 0L);
        }

        return "teacher_insidequiz_result";
    }

    @GetMapping("/attempt/{attemptId}/review")
    public String reviewAttempt(@PathVariable Long attemptId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Attempt> attemptOpt = quizService.getAttemptById(attemptId);
        if (attemptOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Attempt not found");
            return "redirect:/teacher";
        }

        Attempt attempt = attemptOpt.get();
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", answers);
        model.addAttribute("quiz", attempt.getQuiz());

        return "teacher_review_attempt";
    }

    @PostMapping("/answer/{answerId}/grade")
    public String gradeEssayAnswer(@PathVariable Long answerId,
                                   @RequestParam("score") Double score,
                                   @RequestParam("attemptId") Long attemptId,
                                   RedirectAttributes redirectAttributes) {
        try {
            quizService.gradeEssayAnswer(answerId, score);
            redirectAttributes.addFlashAttribute("success", "Answer graded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to grade answer: " + e.getMessage());
        }
        return "redirect:/teacher/attempt/" + attemptId + "/review";
    }
}