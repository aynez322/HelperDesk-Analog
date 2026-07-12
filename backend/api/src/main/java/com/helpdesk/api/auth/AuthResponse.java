package com.helpdesk.api.auth;

public record AuthResponse(String token, String email, String role) {
}
