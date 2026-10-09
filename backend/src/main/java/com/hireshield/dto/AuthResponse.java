package com.hireshield.dto;


public record AuthResponse(String token, long expiresInSeconds, UserResponse user) {
}
