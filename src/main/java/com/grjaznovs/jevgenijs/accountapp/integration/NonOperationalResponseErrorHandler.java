package com.grjaznovs.jevgenijs.accountapp.integration;

import jakarta.annotation.Nonnull;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class NonOperationalResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(@Nonnull ClientHttpResponse httpResponse) {
        return false;
    }

    @Override
    public void handleError(@Nonnull ClientHttpResponse httpResponse) { }
}
