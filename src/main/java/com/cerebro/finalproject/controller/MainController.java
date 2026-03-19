package com.cerebro.finalproject.controller;

import com.cerebro.finalproject.model.User;
import com.cerebro.finalproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MainController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            // Check if email already exists
            if (userService.existsByEmail(user.getEmail())) {
                return "redirect:/register?error";
            }

            // All registered users are teachers
            user.setRole(User.Role.TEACHER);

            // Save user (password will be encoded in service)
            userService.registerUser(user);

            return "redirect:/login?success";
        } catch (Exception e) {
            return "redirect:/register?error";
        }
    }
}