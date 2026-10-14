package com.expensetracker.service;

import com.expensetracker.domain.User;
import com.expensetracker.dto.AuthDtos.AuthResponse;
import com.expensetracker.dto.AuthDtos.LoginRequest;
import com.expensetracker.dto.AuthDtos.RegisterRequest;
import com.expensetracker.dto.AuthDtos.UserResponse;
import com.expensetracker.exception.ConflictException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.exception.UnauthorizedException;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CategoryService categoryService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.displayName().trim()));
        categoryService.createDefaults(user.getId());
        return new AuthResponse(jwtService.issue(user.getId(), user.getEmail()), UserResponse.of(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return new AuthResponse(jwtService.issue(user.getId(), user.getEmail()), UserResponse.of(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId).map(UserResponse::of).orElseThrow(() -> new NotFoundException("User"));
    }
}
