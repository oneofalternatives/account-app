package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.model.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AccountTestFactory {

    public static Account accountWith(
        int accountId,
        int clientId,
        String accountNumber,
        double balance,
        String currency
    ) {
        var account = new Account();
        account.setId(accountId);
        account.setClientId(clientId);
        account.setNumber(accountNumber);
        account.setBalance(BigDecimal.valueOf(balance).setScale(10, RoundingMode.HALF_UP));
        account.setCurrency(currency);
        return account;
    }

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

    public static Account accountWith(
        int accountId,
        String currency
    ) {
        var account = new Account();
        account.setId(accountId);
        account.setCurrency(currency);
        return account;
    }

    public static Account accountWith(int accountId) {
        var account = new Account();
        account.setId(accountId);
        return account;
    }
}
