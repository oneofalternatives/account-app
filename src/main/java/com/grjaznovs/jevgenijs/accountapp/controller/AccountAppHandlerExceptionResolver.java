package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeResultInterpretationError;
import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeServiceError;
import com.grjaznovs.jevgenijs.accountapp.error.FundTransferValidationError;
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
