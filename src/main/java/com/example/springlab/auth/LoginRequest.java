package com.example.springlab.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String userId, @NotBlank String password) {}
