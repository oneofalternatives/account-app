package com.oneofalternatives.accountapp.integration;

import jakarta.annotation.Nonnull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

public class ExchangeRateHostAuthInterceptor implements ClientHttpRequestInterceptor {

    private static final String ACCESS_KEY = "access_key";

    private final String accessKey;

    public ExchangeRateHostAuthInterceptor(String accessKey) { this.accessKey = accessKey; }

    @Nonnull
    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        @Nonnull byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {
        var uriWithAccessKey =
            UriComponentsBuilder
                .fromUri(request.getURI())
                .replaceQueryParam(ACCESS_KEY, accessKey)
                .build()
                .toUri();

        return
            execution
                .execute(
                    new HttpRequestWrapper(request) {
                        @Nonnull
                        @Override
                        public URI getURI() {
                            return uriWithAccessKey;
                        }
                    },
                    body
                );
    }
}
