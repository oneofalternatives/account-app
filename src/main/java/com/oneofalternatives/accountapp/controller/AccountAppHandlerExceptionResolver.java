package com.oneofalternatives.accountapp.controller;

import com.oneofalternatives.accountapp.error.CurrencyExchangeResultInterpretationError;
import com.oneofalternatives.accountapp.error.CurrencyExchangeServiceError;
import com.oneofalternatives.accountapp.error.FundTransferValidationError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class AccountAppHandlerExceptionResolver extends ResponseEntityExceptionHandler {

    @ExceptionHandler(FundTransferValidationError.class)
    protected ResponseEntity<Object> handleFundTransferException(
        FundTransferValidationError exception,
        WebRequest request
    ) {
        return
            handleExceptionInternal(
                exception,
                exception.getMessage(),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request
            );
    }

    @ExceptionHandler({CurrencyExchangeServiceError.class, CurrencyExchangeResultInterpretationError.class})
    protected ResponseEntity<Object> currencyExchangeServiceError(
        RuntimeException exception,
        WebRequest request
    ) {
        return
            handleExceptionInternal(
                exception,
                exception.getMessage(),
                new HttpHeaders(),
                HttpStatus.SERVICE_UNAVAILABLE,
                request
            );
    }
}
