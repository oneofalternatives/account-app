package com.grjaznovs.jevgenijs.accountapp.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountProjection(
    @NotNull
    Integer clientId,
    @NotNull
    @Size(max = 50)
    String number,
    @NotNull
    @Digits(integer = 6, fraction = 10)
    BigDecimal balance,
    @NotNull
    @Size(max = 3)
    String currency
) { }