package com.oneofalternatives.accountapp.controller;

import com.oneofalternatives.accountapp.error.CurrencyExchangeResultInterpretationError;
import com.oneofalternatives.accountapp.error.CurrencyExchangeServiceError;
import com.oneofalternatives.accountapp.error.FundTransferValidationError;
import com.oneofalternatives.accountapp.service.TransactionService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
class TransactionControllerTest {

    private static final UriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @MethodSource("samplesFor_transferFunds_shouldReturnErrorCode_whenValidationErrorThrown")
    void transferFunds_shouldReturnErrorCode_whenValidationErrorThrown(
        Exception internalException,
        HttpStatus expectedHttpStatus,
        String expectedErrorMessage
    ) throws Exception {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/transaction/fund-transfer")
                .queryParam("senderAccountId", 1)
                .queryParam("receiverAccountId", 2)
                .queryParam("amount", BigDecimal.valueOf(10.00))
                .build();

        when(transactionService.transferFunds(1, 2, BigDecimal.valueOf(10.00)))
            .thenThrow(internalException);

        mockMvc
            .perform(MockMvcRequestBuilders.request(HttpMethod.POST, url))
            .andExpect(status().is(expectedHttpStatus.value()))
            .andExpect(MockMvcResultMatchers.content().string(expectedErrorMessage));
    }

    private static Stream<Arguments> samplesFor_transferFunds_shouldReturnErrorCode_whenValidationErrorThrown() {
        return Stream.of(
            arguments(
                new FundTransferValidationError("Validation message"),
                HttpStatus.BAD_REQUEST, "Validation message"),
            arguments(
                new CurrencyExchangeServiceError("Service error"),
                HttpStatus.SERVICE_UNAVAILABLE, "Service error"),
            arguments(
                new CurrencyExchangeResultInterpretationError("Service response error"),
                HttpStatus.SERVICE_UNAVAILABLE, "Service response error")
        );
    }
}