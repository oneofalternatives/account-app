package com.grjaznovs.jevgenijs.accountapp.integration;

import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeServiceError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@RestClientTest(
    components = ExchangeRateHostClient.class,
    properties = "account-app.currency-converter.service.exchangeratehost.enabled=true")
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

    @Test
    void getSupportedCurrencies_shouldPassError() {
        server.expect(requestTo("http://api.exchangerate.host/list?access_key=ACC-KEY-123"))
            .andRespond(
                withSuccess(
                    """
                        {
                          "success": false,
                          "error": {
                            "code": 104,
                            "type": "some_error_code",
                            "info": "User has reached or exceeded his subscription plan's monthly API request allowance"
                          }
                        }
                        """,
                    MediaType.APPLICATION_JSON
                )
            );

        var exception = catchException(client::getSupportedCurrencies);

        assertThat(exception)
            .isInstanceOf(CurrencyExchangeServiceError.class)
            .hasMessage(
                "Currency exchange service failed to return list of supported currencies. " +
                    "Reason code: 104. " +
                    "Reason description: User has reached or exceeded his subscription plan's monthly API request allowance"
            );
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

    @Test
    void getDirectRate_shouldPassError() {
        server.expect(requestTo(
                "http://api.exchangerate.host/historical" +
                    "?source=EUR&currencies=USD&date=2024-01-14&access_key=ACC-KEY-123"
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

        var exception = catchException(() ->
            client
                .getDirectRate(
                    Currency.getInstance("EUR"),
                    Currency.getInstance("USD"),
                    LocalDate.parse("2024-01-14")
                )
        );

        assertThat(exception)
            .isInstanceOf(CurrencyExchangeServiceError.class)
            .hasMessage(
                "Currency exchange service failed to return quotes. " +
                    "Reason code: 201. " +
                    "Reason description: You have supplied an invalid Source Currency. [Example: source=EUR]"
            );
    }

    @Test
    void getDirectRate_shouldReturnRate() {
        server.expect(requestTo(
                "http://api.exchangerate.host/historical" +
                    "?source=EUR&currencies=USD&date=2024-01-14&access_key=ACC-KEY-123"
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

        var exchangeRate =
            client
                .getDirectRate(
                    Currency.getInstance("EUR"),
                    Currency.getInstance("USD"),
                    LocalDate.parse("2024-01-14")
                );

        assertThat(exchangeRate).isEqualTo(BigDecimal.valueOf(1.0952));
    }

    @TestConfiguration
    static class Configuration {

        @Bean
        public ExchangeRateHostIntegrationSettings exchangeRateHostIntegrationSettings() {
            return new ExchangeRateHostIntegrationSettings(
                "http://api.exchangerate.host",
                "ACC-KEY-123"
            );
        }
    }
}
