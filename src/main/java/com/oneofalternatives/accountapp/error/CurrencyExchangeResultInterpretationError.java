package com.oneofalternatives.accountapp.error;

public class CurrencyExchangeResultInterpretationError extends RuntimeException {

    public CurrencyExchangeResultInterpretationError(String message) {
        super(message);
    }

    public CurrencyExchangeResultInterpretationError(Throwable cause) {
        super(cause);
    }
}
