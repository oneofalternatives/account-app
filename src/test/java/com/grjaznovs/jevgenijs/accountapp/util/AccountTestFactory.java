package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.api.AccountProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Account;

import java.util.Currency;

import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.ROUNDING_MODE;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.SCALE;
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

    public static AccountProjection accountProjectionFor(Account account) {
        return
            new AccountProjection(
                account.getId(),
                account.getNumber(),
                account.getBalance().setScale(SCALE, ROUNDING_MODE),
                account.getCurrency()
            );
    }
}
