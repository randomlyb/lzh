package com.travlejava.ai.auth.controller;

import com.travlejava.ai.auth.service.AuthService;
import com.travlejava.ai.pojo.auth.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userinfo")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // In a real application, you might fetch more details or a specific DTO
        // For now, let's assume username is enough to fetch the User object
        // However, AuthService.getUserInfo currently takes an ID, so we need to adjust or create a new method
        // For simplicity, we will just return a dummy user or modify AuthService for now.
        // A better approach would be to have user ID in JWT or fetch by username.

        // Let's modify AuthService to get user by username for now for simplicity
        User user = authService.getUserInfoByUsername(userDetails.getUsername()); // Assuming this method exists or will be added
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}
