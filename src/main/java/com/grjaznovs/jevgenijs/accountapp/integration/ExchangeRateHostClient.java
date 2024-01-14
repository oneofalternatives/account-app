package com.grjaznovs.jevgenijs.accountapp.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeServiceError;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExchangeRateHostClient implements CurrencyConversionClient {

    private static final UriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    private final RestTemplate restTemplate;
    private final ExchangeRateHostIntegrationSettings settings;

    public ExchangeRateHostClient(
        RestTemplate exchangeRateHostRestTemplate,
        ExchangeRateHostIntegrationSettings settings
    ) {
        this.restTemplate = exchangeRateHostRestTemplate;
        this.settings = settings;
    }

    @Override
    public Set<Currency> getSupportedCurrencies() {
        var response =
            restTemplate
                .exchange(
                    URI_BUILDER_FACTORY
                        .uriString(settings.rootUrl())
                        .path("/list")
                        .build(),
                    HttpMethod.GET,
                    null,
                    SupportedCurrencyListProjection.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CurrencyExchangeServiceError("Currency exchange service responded with status " + response.getStatusCode());
        }

        var body = response.getBody();

        if (body == null) {
            throw new CurrencyExchangeServiceError("Currency exchange service response body is missing");
        }

        if (!body.success()) {
            throw new CurrencyExchangeServiceError("Currency exchange service failed to list supported currencies");
        }

        return
            body.currencies().keySet()
                .stream()
                .filter(currencyCode ->
                    Currency.getAvailableCurrencies()
                        .stream()
                        .map(Currency::getCurrencyCode)
                        .anyMatch(currencyCode::equals)
                )
                .map(Currency::getInstance)
                .collect(Collectors.toSet());
    }

    @Override
    public BigDecimal getDirectRate(Currency fromCurrency, Currency toCurrency, LocalDate date) {
        var response =
            restTemplate
                .exchange(
                    URI_BUILDER_FACTORY
                        .uriString(settings.rootUrl())
                        .path("/historical")
                        .queryParam("source", fromCurrency)
                        .queryParam("currencies", toCurrency)
                        .queryParam("date", date)
                        .build(),
                    HttpMethod.GET,
                    null,
                    QuotesProjection.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CurrencyExchangeServiceError("Currency exchange service responded with status " + response.getStatusCode());
        }

        var body = response.getBody();

        if (body == null) {
            throw new CurrencyExchangeServiceError("Currency exchange service response body is missing");
        }

        if (!body.success()) {
            throw new CurrencyExchangeServiceError("Currency exchange service failed to return quotes");
        }

        return
            body.quotes().values().stream().findAny()
                .orElseThrow(() -> new CurrencyExchangeServiceError("Currency exchange service did not return quotes"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SupportedCurrencyListProjection(Boolean success, Map<String, String> currencies) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuotesProjection(Boolean success, Map<String, BigDecimal> quotes) { }
}
