package com.grjaznovs.jevgenijs.accountapp.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "account-app.currency-converter.mock")
public record CurrencyConverterMockSettings(
    Set<String> supportedCurrencies,
    Map<String, BigDecimal> exchangeRates
) {
}
