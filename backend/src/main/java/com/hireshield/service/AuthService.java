package com.hireshield.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.AuthResponse;
import com.hireshield.dto.LoginRequest;
import com.hireshield.dto.RegisterRequest;
import com.hireshield.dto.UserResponse;
import com.hireshield.entity.Role;
import com.hireshield.entity.User;
import com.hireshield.exception.ConflictException;
import com.hireshield.exception.NotFoundException;
import com.hireshield.repository.UserRepository;
import com.hireshield.security.AuthUser;
import com.hireshield.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
        userRepository.save(user);
        return tokenFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        AuthUser principal = (AuthUser) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return tokenFor(user);
    }

    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.createToken(new AuthUser(user));
        return new AuthResponse(token, jwtService.expirySeconds(), UserResponse.from(user));
    }
}
