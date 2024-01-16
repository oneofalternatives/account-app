package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// TODO find out how to create this immutable bean using @Bean
@ConfigurationProperties(prefix = "account-app.currency-converter.service.exchangeratehost")
@ConditionalOnProperty(value = "account-app.currency-converter.mock.enabled", havingValue = "false", matchIfMissing = true)
public record ExchangeRateHostIntegrationSettings(
    String rootUrl,
    String accessKey,
    Duration connectionTimeout,
    Duration readTimeout
) { }
