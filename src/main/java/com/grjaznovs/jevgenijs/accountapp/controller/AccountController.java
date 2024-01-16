package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.api.AccountProjection;
import com.grjaznovs.jevgenijs.accountapp.api.CreateAccountProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
        summary = "List all accounts",
        description = "Returns stored account entities. No pagination.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "List of accounts, if any exist",
            content = @Content(mediaType = "application/json", array = @ArraySchema(items = @Schema(implementation = Account.class))))
    })
    @GetMapping("/account")
    public List<Account> findAllAccounts() {
        return accountService.findAllAccounts();
    }

    @Operation(
        summary = "List accounts by client ID",
        description = "Returns account projections without clientId. No pagination.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "List of accounts, if any exist",
            content = @Content(mediaType = "application/json", array = @ArraySchema(items = @Schema(implementation = AccountProjection.class))))
    })
    @GetMapping("/client/{clientId}/account")
    public List<AccountProjection> findAccountsByClientId(
        @Parameter(description = "client ID, leave empty to list accounts for all clients")
        @PathVariable Integer clientId
    ) {
        return accountService.findAccountsByClientId(clientId);
    }

    @Operation(
        summary = "Create an account",
        description = "Returns stored account entity.")
    @PutMapping("/account")
    public Account createAccount(
        @RequestBody CreateAccountProjection account
    ) {
        return accountService.createAccount(account);
    }
}
