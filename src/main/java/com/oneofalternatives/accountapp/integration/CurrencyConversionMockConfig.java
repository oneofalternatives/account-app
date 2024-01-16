package com.oneofalternatives.accountapp.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "account-app.currency-converter.mock.enabled", havingValue = "true")
public class CurrencyConversionMockConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyConversionMockConfig.class);

    @Bean
    public CurrencyConversionClient currencyConversionClientMock(
        CurrencyConverterMockSettings currencyConverterMockSettings
    ) {
        LOGGER.info("Using mocked currency exchange client implementation");

        return new CurrencyConversionClientMock(currencyConverterMockSettings);
    }
}
