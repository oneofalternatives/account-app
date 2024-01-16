package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

@Repository
public interface CurrencyConversionClient {

    Set<Currency> getSupportedCurrencies();

    BigDecimal getDirectRate(Currency fromCurrency, Currency toCurrency);
}
