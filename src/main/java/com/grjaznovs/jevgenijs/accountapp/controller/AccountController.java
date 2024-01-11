package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public Iterable<Account> findAccountsByClientId(@RequestParam(required = false) Integer clientId) {
        if (clientId == null) {
            return accountRepository.findAll();
        } else {
            return accountRepository.findAllByClientId(clientId);
        }
    }
}
