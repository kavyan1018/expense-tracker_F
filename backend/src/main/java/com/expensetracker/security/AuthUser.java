package com.expensetracker.security;

/** The authenticated principal. The id comes from a verified JWT, never from the request. */
public record AuthUser(Long id, String email) {
}
