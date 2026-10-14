package com.expensetracker.web;

import com.expensetracker.dto.AuthDtos.AuthResponse;
import com.expensetracker.dto.AuthDtos.LoginRequest;
import com.expensetracker.dto.AuthDtos.RegisterRequest;
import com.expensetracker.dto.AuthDtos.UserResponse;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return auth.me(CurrentUser.id());
    }
}
