package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.model.Account;

import java.util.Currency;

import static com.grjaznovs.jevgenijs.accountapp.util.TypeUtils.scaledBigDecimal;

public class AccountTestFactory {

    public static Account accountWith(
        int accountId,
        int clientId,
        String accountNumber,
        double balance,
        Currency currency
    ) {
        var account = new Account();
        account.setId(accountId);
        account.setClientId(clientId);
        account.setNumber(accountNumber);
        account.setBalance(scaledBigDecimal(balance));
        account.setCurrency(currency);
        return account;
    }

    public static Account accountWith(
        int clientId,
        String accountNumber,
        double balance,
        Currency currency
    ) {
        var account = new Account();
        account.setClientId(clientId);
        account.setNumber(accountNumber);
        account.setBalance(scaledBigDecimal(balance));
        account.setCurrency(currency);
        return account;
    }

    public static Account accountWith(
        int accountId,
        Currency currency
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

    public static Account copy(Account account) {
        var copy = new Account();
        copy.setId(account.getId());
        copy.setClientId(account.getClientId());
        copy.setNumber(account.getNumber());
        copy.setBalance(account.getBalance());
        copy.setCurrency(account.getCurrency());
        return copy;
    }
}
