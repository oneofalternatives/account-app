package com.oneofalternatives.accountapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(value = "account-app.currency-converter.mock.enabled", havingValue = "false", matchIfMissing = true)
public class CurrencyConversionIntegrationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyConversionIntegrationConfig.class);

    @Bean
    public CurrencyConversionClient exchangeRateHostClient(
        RestTemplate exchangeRateHostRestTemplate,
        ExchangeRateHostIntegrationSettings settings,
        ObjectMapper objectMapper
    ) {
        LOGGER.info("Using real currency exchange client implementation");

        return
            new ExchangeRateHostClient(
                exchangeRateHostRestTemplate,
                settings,
                objectMapper
            );
    }

    @Bean
    public RestTemplate exchangeRateHostRestTemplate(
        RestTemplateBuilder builder,
        ExchangeRateHostIntegrationSettings settings
    ) {
        return
            builder
                .interceptors(new ExchangeRateHostAuthInterceptor(settings.accessKey()))
                .errorHandler(new NonOperationalResponseErrorHandler())
                .setConnectTimeout(settings.connectionTimeout())
                .setReadTimeout(settings.readTimeout())
                .build();
    }
}
