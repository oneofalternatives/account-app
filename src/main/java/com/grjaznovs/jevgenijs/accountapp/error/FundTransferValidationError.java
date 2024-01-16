package com.grjaznovs.jevgenijs.accountapp.error;

public class FundTransferValidationError extends RuntimeException {

    public FundTransferValidationError(String message) {
        super(message);
    }
}
