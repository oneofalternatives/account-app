package com.grjaznovs.jevgenijs.accountapp.integration;

import com.grjaznovs.jevgenijs.accountapp.settings.CurrencyConverterMockSettings;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Repository
public class CurrencyConversionClientMock implements CurrencyConversionClient {

    private final CurrencyConverterMockSettings settings;

    public CurrencyConversionClientMock(CurrencyConverterMockSettings settings) {
        this.settings = settings;
    }

    @Override
    public Set<String> getSupportedCurrencies() {
        return settings.supportedCurrencies();
    }

    @Override
    public BigDecimal convert(
        BigDecimal amount,
        String sourceCurrency,
        String targetCurrency,
        LocalDate date
    ) {
        return
            settings
                .exchangeRates()
                .get(sourceCurrency + targetCurrency)
                .multiply(amount);
    }
}