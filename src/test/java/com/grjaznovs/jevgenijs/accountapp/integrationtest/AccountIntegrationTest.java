package com.grjaznovs.jevgenijs.accountapp.integrationtest;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.grjaznovs.jevgenijs.accountapp.util.AccountTestFactory.accountProjectionFor;
import static com.grjaznovs.jevgenijs.accountapp.util.CreateAccountProjectionFactory.createAccountProjection;
import static com.grjaznovs.jevgenijs.accountapp.util.Currencies.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class AccountIntegrationTest {

    @Autowired
    private TestAccountAppRestClient rest;

    @Test
    void shouldReturnEmptyListWhenAccountsDoNotExist() {
        var listOfAccounts = rest.getAccountsFor(getMaxClientId() + 1);

        assertThat(listOfAccounts).isEmpty();
    }

    @Test
    void shouldReturnAccountsByClientId() {
        var clientOne = getMaxClientId() + 1;
        var clientTwo = clientOne + 1;

        var eurAccount = rest.putAccountSuccess(createAccountProjection(clientOne, "ACC-0001", 1000.00, EUR));
        var usdAccount = rest.putAccountSuccess(createAccountProjection(clientTwo, "ACC-0002", 0900.00, USD));
        var audAccount = rest.putAccountSuccess(createAccountProjection(clientTwo, "ACC-0003", 0800.00, AUD));

        var listOfClientOneAccounts = rest.getAccountsFor(clientOne);

        assertThat(listOfClientOneAccounts)
            .singleElement()
            .isEqualTo(accountProjectionFor(eurAccount));

        var listOfClientTwoAccounts = rest.getAccountsFor(clientTwo);

        assertThat(listOfClientTwoAccounts)
            .containsExactlyInAnyOrder(
                accountProjectionFor(usdAccount),
                accountProjectionFor(audAccount)
            );
    }


    private int getMaxClientId() {
        return
            rest
                .getAllAccounts().stream()
                .map(Account::getClientId)
                .max(Integer::compareTo)
                .orElse(0);
    }
}
