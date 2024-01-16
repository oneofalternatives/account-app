package com.oneofalternatives.accountapp.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.RoundingMode;

@ConfigurationProperties(prefix = "account-app.money")
public record MoneySettings(
    int scale,
    RoundingMode roundingMode
) { }
