package com.grjaznovs.jevgenijs.accountapp.api;

import java.math.BigDecimal;
import java.util.Currency;

public record AccountProjection(
    Integer id,
    String number,
    BigDecimal balance,
    Currency currency
) { }
