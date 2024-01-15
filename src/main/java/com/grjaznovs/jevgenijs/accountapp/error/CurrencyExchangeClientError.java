package com.grjaznovs.jevgenijs.accountapp.error;

public class CurrencyExchangeClientError extends RuntimeException {

    public CurrencyExchangeClientError(String message) {
        super(message);
    }

    public CurrencyExchangeClientError(Throwable cause) {
        super(cause);
    }
}
