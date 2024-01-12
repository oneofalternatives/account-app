package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Repository
public interface CurrencyConversionClient {

    Set<String> getSupportedCurrencies();

    BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency, LocalDate date);
}
