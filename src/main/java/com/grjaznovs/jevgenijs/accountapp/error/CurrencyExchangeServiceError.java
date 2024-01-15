package com.grjaznovs.jevgenijs.accountapp.error;

public class CurrencyExchangeServiceError extends RuntimeException {

    public CurrencyExchangeServiceError() {
        super();
    }

    public CurrencyExchangeServiceError(String message) {
        super(message);
    }
}
