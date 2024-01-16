package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.api.AccountProjection;
import com.grjaznovs.jevgenijs.accountapp.api.CreateAccountProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    public List<AccountProjection> findAccountsByClientId(@Nonnull Integer clientId) {
        return
            accountRepository.findAllByClientId(clientId).stream()
                .map(account ->
                    new AccountProjection(
                        account.getId(),
                        account.getNumber(),
                        account.getBalance(),
                        account.getCurrency()
                    )
                )
                .collect(Collectors.toList());
    }

    public Account createAccount(CreateAccountProjection createAccountProjection) {
        var account = new Account();
        account.setClientId(createAccountProjection.clientId());
        account.setNumber(createAccountProjection.number());
        account.setBalance(createAccountProjection.balance());
        account.setCurrency(Currency.getInstance(createAccountProjection.currency()));

        return accountRepository.save(account);
    }
}
