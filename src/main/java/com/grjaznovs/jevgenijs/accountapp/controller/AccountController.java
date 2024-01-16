package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Operation(
        summary = "List accounts by client ID",
        description = "Returns plain account objects. No pagination.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "List of accounts, may be empty if no transactions",
            content = @Content(mediaType = "application/json", array = @ArraySchema(items = @Schema(implementation = Account.class))))
    })
    @GetMapping
    public Iterable<Account> findAccountsByClientId(
        @Parameter(description = "client ID, leave empty to list accounts for all clients")
        @RequestParam(required = false) Integer clientId
    ) {
        if (clientId == null) {
            return accountRepository.findAll();
        } else {
            return accountRepository.findAllByClientId(clientId);
        }
    }
}
