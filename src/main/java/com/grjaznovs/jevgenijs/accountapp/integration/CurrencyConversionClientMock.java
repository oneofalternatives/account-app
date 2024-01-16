package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

@Repository
@ConditionalOnProperty(value = "account-app.currency-converter.mock.enabled", havingValue = "true")
public class CurrencyConversionClientMock implements CurrencyConversionClient {

    private final CurrencyConverterMockSettings settings;

    public CurrencyConversionClientMock(CurrencyConverterMockSettings settings) {
        this.settings = settings;
    }

    @Override
    public Set<Currency> getSupportedCurrencies() {
        return settings.supportedCurrencies();
    }

    @Override
    public BigDecimal getDirectRate(Currency fromCurrency, Currency toCurrency) {
        return settings.exchangeRates().get(fromCurrency.getCurrencyCode() + toCurrency.getCurrencyCode());
    }
}
