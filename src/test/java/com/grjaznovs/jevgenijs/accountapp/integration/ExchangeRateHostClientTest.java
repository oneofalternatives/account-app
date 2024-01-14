package com.grjaznovs.jevgenijs.accountapp.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// TODO write tests for failure response
@ExtendWith(SpringExtension.class)
@RestClientTest(ExchangeRateHostClient.class)
@Import(ExchangeRateHostClientTest.Configuration.class)
class ExchangeRateHostClientTest {

    private final MockRestServiceServer server;
    private final ExchangeRateHostClient client;

    @Autowired
    public ExchangeRateHostClientTest(
        MockRestServiceServer server,
        ExchangeRateHostClient client
    ) {
        this.server = server;
        this.client = client;
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
            client.getDirectRate(Currency.getInstance("EUR"), Currency.getInstance("USD"), LocalDate.parse("2024-01-14"));

        assertThat(exchangeRate).isEqualTo(BigDecimal.valueOf(1.0952));
    }

    @TestConfiguration
    static class Configuration {

        @Bean
        public ExchangeRateHostClient exchangeRateHostClient(
            RestTemplate exchangeRateHostRestTemplate,
            ExchangeRateHostIntegrationSettings settings
        ) {
            return new ExchangeRateHostClient(exchangeRateHostRestTemplate, settings);
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

        @Bean
        public ExchangeRateHostIntegrationSettings exchangeRateHostIntegrationSettings() {
            return new ExchangeRateHostIntegrationSettings("http://api.exchangerate.host", "ACC-KEY-123");
        }
    }
}
