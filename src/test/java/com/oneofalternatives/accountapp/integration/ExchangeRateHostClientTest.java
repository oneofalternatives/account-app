package com.oneofalternatives.accountapp.integration;

import com.oneofalternatives.accountapp.error.CurrencyExchangeResultInterpretationError;
import com.oneofalternatives.accountapp.error.CurrencyExchangeServiceError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.stream.Stream;

import static com.oneofalternatives.accountapp.util.Currencies.EUR;
import static com.oneofalternatives.accountapp.util.Currencies.USD;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@RestClientTest(
    components = ExchangeRateHostClient.class,
    properties = "account-app.currency-converter.mock.enabled=false")
@Import({CurrencyConversionIntegrationConfig.class, ExchangeRateHostClientTest.Configuration.class})
class ExchangeRateHostClientTest {

    private final MockRestServiceServer server;
    private final CurrencyConversionClient client;

    @Autowired
    public ExchangeRateHostClientTest(
        MockRestServiceServer server,
        CurrencyConversionClient exchangeRateHostClient
    ) {
        this.server = server;
        this.client = exchangeRateHostClient;
    }

    @ParameterizedTest
    @MethodSource("samplesFor_errorResponses")
    void getSupportedCurrencies_shouldThrowException(
        ResponseCreator responseCreator,
        String errorMessage
    ) {
        server.expect(requestTo("http://api.exchangerate.host/list?access_key=ACC-KEY-123"))
            .andRespond(responseCreator);

        assertThatThrownBy(client::getSupportedCurrencies)
            .isInstanceOf(CurrencyExchangeServiceError.class)
            .hasMessage(errorMessage);
    }

    @Test
    void getSupportedCurrencies_shouldThrowException_whenListIsEmpty() {
        server
            .expect(requestTo("http://api.exchangerate.host/list?access_key=ACC-KEY-123"))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": true,
                          "terms": "https:\\/\\/currencylayer.com\\/terms",
                          "privacy": "https:\\/\\/currencylayer.com\\/privacy",
                          "currencies": { }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        assertThatThrownBy(client::getSupportedCurrencies)
            .isInstanceOf(CurrencyExchangeResultInterpretationError.class);
    }

    @Test
    void getSupportedCurrencies_shouldReturnCurrencies() {
        server
            .expect(requestTo("http://api.exchangerate.host/list?access_key=ACC-KEY-123"))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": true,
                          "terms": "https:\\/\\/currencylayer.com\\/terms",
                          "privacy": "https:\\/\\/currencylayer.com\\/privacy",
                          "currencies": {
                            "BOB": "Bolivian Boliviano",
                            "BRL": "Brazilian Real",
                            "BSD": "Bahamian Dollar",
                            "BTC": "Bitcoin"
                          }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        var supportedCurrencies = client.getSupportedCurrencies();

        assertThat(supportedCurrencies)
            .map(Currency::getCurrencyCode)
            .containsExactlyInAnyOrder("BOB", "BRL", "BSD");
    }

    @ParameterizedTest
    @MethodSource("samplesFor_errorResponses")
    void getDirectRate_shouldThrowException(
        ResponseCreator responseCreator,
        String expectedErrorMessage
    ) {
        server.expect(requestTo(
                "http://api.exchangerate.host/live" +
                    "?source=EUR&currencies=USD&access_key=ACC-KEY-123"
            ))
            .andRespond(responseCreator);

        assertThatThrownBy(() -> client.getDirectRate(EUR, USD))
            .isInstanceOf(CurrencyExchangeServiceError.class)
            .hasMessage(expectedErrorMessage);
    }

    @Test
    void getDirectRate_shouldPassError() {
        server.expect(requestTo(
                "http://api.exchangerate.host/live" +
                    "?source=EUR&currencies=USD&access_key=ACC-KEY-123"
            ))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": false,
                          "error": {
                            "code": 201,
                            "type": "invalid_source_currency",
                            "info": "You have supplied an invalid Source Currency. [Example: source=EUR]"
                          }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        var exception = catchException(() -> client.getDirectRate(Currency.getInstance("EUR"), Currency.getInstance("USD")));

        assertThat(exception)
            .isInstanceOf(CurrencyExchangeServiceError.class)
            .hasMessage(
                "Currency exchange service failed to return quotes. " +
                    "Reason code: 201. " +
                    "Reason description: You have supplied an invalid Source Currency. [Example: source=EUR]"
            );
    }

    @Test
    void getDirectRate_shouldThrowException_whenMoreThanOneCurrencyQuoteReturned() {
        server.expect(requestTo(
                "http://api.exchangerate.host/live" +
                    "?source=EUR&currencies=USD&access_key=ACC-KEY-123"
            ))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": true,
                          "terms": "https:\\/\\/currencylayer.com\\/terms",
                          "privacy": "https:\\/\\/currencylayer.com\\/privacy",
                          "historical": true,
                          "date": "2024-01-14",
                          "timestamp": 1705267263,
                          "source": "EUR",
                          "quotes": {
                            "EURUSD": 1.0952,
                            "EURAUD": 1.2345
                          }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        var exception = catchException(() -> client.getDirectRate(Currency.getInstance("EUR"), Currency.getInstance("USD")));

        assertThat(exception)
            .isInstanceOf(CurrencyExchangeResultInterpretationError.class)
            .hasMessage("Currency exchange service returned more than one quote");
    }

    @Test
    void getDirectRate_shouldReturnRate() {
        server.expect(requestTo(
                "http://api.exchangerate.host/live" +
                    "?source=EUR&currencies=USD&access_key=ACC-KEY-123"
            ))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": true,
                          "terms": "https:\\/\\/currencylayer.com\\/terms",
                          "privacy": "https:\\/\\/currencylayer.com\\/privacy",
                          "historical": true,
                          "date": "2024-01-14",
                          "timestamp": 1705267263,
                          "source": "EUR",
                          "quotes": {
                            "EURUSD": 1.0952
                          }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        var exchangeRate = client.getDirectRate(Currency.getInstance("EUR"), Currency.getInstance("USD"));

        assertThat(exchangeRate).isEqualTo(BigDecimal.valueOf(1.0952));
    }

    private static Stream<Arguments> samplesFor_errorResponses() {
        // @formatter:off
            return Stream.of(
                arguments(withStatus(SERVICE_UNAVAILABLE), "Currency exchange service responded with status 503 SERVICE_UNAVAILABLE"),
                arguments(withStatus(BAD_REQUEST),         "Currency exchange service responded with status 400 BAD_REQUEST"        ),
                arguments(withStatus(MULTIPLE_CHOICES),    "Currency exchange service responded with status 300 MULTIPLE_CHOICES"   ),
                arguments(withStatus(CONTINUE),            "Currency exchange service responded with status 100 CONTINUE"           ),
                arguments(withSuccess(),                   "Currency exchange service response body is missing"                     )
            );
            // @formatter:on
    }

    @TestConfiguration
    static class Configuration {

        @Bean
        public ExchangeRateHostIntegrationSettings exchangeRateHostIntegrationSettings() {
            return new ExchangeRateHostIntegrationSettings(
                "http://api.exchangerate.host",
                "ACC-KEY-123",
                Duration.of(1, SECONDS),
                Duration.of(1, SECONDS)
            );
        }
    }
}
