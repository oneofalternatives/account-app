package com.grjaznovs.jevgenijs.accountapp;

import com.grjaznovs.jevgenijs.accountapp.model.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AccountTestFactory {

    public static Account accountWith(
        int clientId,
        String accountNumber,
        double balance,
        String currency
    ) {
        var account = new Account();
        account.setClientId(clientId);
        account.setNumber(accountNumber);
        account.setBalance(BigDecimal.valueOf(balance).setScale(10, RoundingMode.HALF_UP));
        account.setCurrency(currency);
        return account;
    }
}
