package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(value = "account-app.currency-converter.service.exchangeratehost.enabled", havingValue = "true")
public class CurrencyConversionIntegrationConfig {

    @Bean
    public CurrencyConversionClient exchangeRateHostClient(
        RestTemplate exchangeRateHostRestTemplate,
        ExchangeRateHostIntegrationSettings settings
    ) {
        return new ExchangeRateHostClient(
            exchangeRateHostRestTemplate,
            settings
        );
    }

    @Bean
    public RestTemplate exchangeRateHostRestTemplate(
        RestTemplateBuilder builder,
        ExchangeRateHostIntegrationSettings settings
    ) {
        return
            builder
                .additionalInterceptors()
                .interceptors(new ExchangeRateHostAuthInterceptor(settings.accessKey()))
                .build();
    }
}
