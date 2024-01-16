package com.grjaznovs.jevgenijs.accountapp.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "account-app.currency-converter.service.exchangeratehost")
public record ExchangeRateHostIntegrationSettings(
    String rootUrl,
    String accessKey,
    Duration connectionTimeout,
    Duration readTimeout
) { }
