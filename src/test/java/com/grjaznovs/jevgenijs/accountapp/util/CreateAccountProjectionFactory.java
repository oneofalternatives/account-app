package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.api.CreateAccountProjection;

import java.math.BigDecimal;
import java.util.Currency;

public class CreateAccountProjectionFactory {

    public static CreateAccountProjection createAccountProjection(
        int clientId,
        String accountNumber,
        double balance,
        Currency currency
    ) {
        return
            new CreateAccountProjection(
                clientId,
                accountNumber,
                BigDecimal.valueOf(balance),
                currency.getCurrencyCode()
            );
    }
}
