package com.devlab.taskboard.dto.response;

public record AuthResponse(
        String jwtToken,
        String tokenType,
        long expiresInSeconds
) {

    public static AuthResponse bearer(String jwtToken, long expiresInSeconds) {
        return new AuthResponse(jwtToken, "Bearer", expiresInSeconds);
    }
}
