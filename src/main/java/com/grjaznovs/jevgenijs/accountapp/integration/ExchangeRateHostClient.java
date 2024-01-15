package com.grjaznovs.jevgenijs.accountapp.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeClientError;
import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeServiceError;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
    private final ObjectMapper objectMapper;

    public ExchangeRateHostClient(
        RestTemplate exchangeRateHostRestTemplate,
        ExchangeRateHostIntegrationSettings settings,
        ObjectMapper objectMapper
    ) {
        this.restTemplate = exchangeRateHostRestTemplate;
        this.settings = settings;
        this.objectMapper = objectMapper;
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
                    ObjectNode.class
                );

        var supportedCurrencies =
            handleError(
                response,
                "Currency exchange service failed to return list of supported currencies",
                SupportedCurrencyListProjection.class
            )
                .currencies();

        if (supportedCurrencies.isEmpty()) {
            throw new CurrencyExchangeClientError("Currency exchange service did not return any supported currencies");
        }

        return
            supportedCurrencies.keySet()
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
                    ObjectNode.class
                );

        var quotes =
            handleError(
                response,
                "Currency exchange service failed to return quotes",
                QuotesProjection.class
            )
                .quotes();

        if (quotes.size() > 1) {
            throw new CurrencyExchangeClientError("Currency exchange service returned more than one quote");
        }

        return
            quotes.values().stream().findAny()
                .orElseThrow(() -> new CurrencyExchangeServiceError("Currency exchange service did not return any quotes"));
    }

    private <T> T handleError(
        ResponseEntity<ObjectNode> response,
        String messageForErrorCode,
        Class<T> successBodyType
    ) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CurrencyExchangeServiceError("Currency exchange service responded with status " + response.getStatusCode());
        }

        var bodyJson = response.getBody();

        if (bodyJson == null) {
            throw new CurrencyExchangeServiceError("Currency exchange service response body is missing");
        }

        if (isFailure(bodyJson)) {
            var error = fromJson(bodyJson, ErrorProjection.class).error();

            throw new CurrencyExchangeServiceError(
                String.format(
                    "%s. Reason code: %s. Reason description: %s",
                    messageForErrorCode,
                    error.code(),
                    error.info()
                )
            );
        }

        return fromJson(bodyJson, successBodyType);
    }

    private static boolean isFailure(ObjectNode bodyJson) {
        //noinspection PointlessBooleanExpression
        return bodyJson.get("success").asBoolean() == false;
    }

    private <T> T fromJson(ObjectNode jsonObject, Class<T> type) {
        try {
            return objectMapper.treeToValue(jsonObject, type);
        } catch (JsonProcessingException e) {
            throw new CurrencyExchangeClientError(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SupportedCurrencyListProjection(Boolean success, Map<String, String> currencies) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuotesProjection(Map<String, BigDecimal> quotes) { }

    private record ErrorProjection(ErrorDetails error) {

        private record ErrorDetails(Integer code, String info) { }
    }
}
