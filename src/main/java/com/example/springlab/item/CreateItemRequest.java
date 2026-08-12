package com.example.springlab.item;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateItemRequest(
    @NotBlank String name, @NotNull @Min(0) Integer price, @NotBlank String description) {}
