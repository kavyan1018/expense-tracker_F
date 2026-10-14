package com.expensetracker.dto;

import com.expensetracker.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 255) String email,
            @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
            @NotBlank(message = "Name is required") @Size(max = 100) String displayName) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record UserResponse(Long id, String email, String displayName, String currency) {
        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getCurrency());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}
