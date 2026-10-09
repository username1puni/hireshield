package com.hireshield.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.UserResponse;
import com.hireshield.dto.UserStatsResponse;
import com.hireshield.security.AuthUser;
import com.hireshield.service.AuthService;
import com.hireshield.service.StatsService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final AuthService authService;
    private final StatsService statsService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.currentUser(user.getId());
    }

    @GetMapping("/me/stats")
    public UserStatsResponse myStats(@AuthenticationPrincipal AuthUser user) {
        return statsService.userStats(user.getId());
    }
}
