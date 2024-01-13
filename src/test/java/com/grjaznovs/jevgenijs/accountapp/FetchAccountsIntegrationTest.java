package com.grjaznovs.jevgenijs.accountapp;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.util.List;
import java.util.Set;

import static com.grjaznovs.jevgenijs.accountapp.AccountTestFactory.accountWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
// TODO Find out why @ActiveProfiles(profiles = {"common"}) didn't work
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class FetchAccountsIntegrationTest {

    private static final UriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    private final AccountRepository accountRepository;

    private final TestRestTemplate testRestTemplate;

    @Autowired
    public FetchAccountsIntegrationTest(
        AccountRepository accountRepository,
        TestRestTemplate testRestTemplate
    ) {
        this.accountRepository = accountRepository;
        this.testRestTemplate = testRestTemplate;
    }

    @BeforeEach
    void beforeEach() {
        // TODO Find out how to recreate database (@DirtiesContext causes Liquibase error)
        accountRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyListWhenAccountsDoNotExist() {
        var listOfAccounts = restGetAccountsForClientId(1);

        assertThat(listOfAccounts).isEmpty();
    }

    @Test
    void shouldReturnAccountsByClientId() {
        var eurAccount = accountWith(1, "ACC-0001", 1000.00, "EUR");
        var usdAccount = accountWith(2, "ACC-0002", 0900.00, "USD");
        var audAccount = accountWith(2, "ACC-0003", 0800.00, "AUD");

        accountRepository.saveAllAndFlush(Set.of(eurAccount, usdAccount, audAccount));

        var listOfClient1Accounts = restGetAccountsForClientId(1);

        assertThat(listOfClient1Accounts)
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(eurAccount);

        var listOfClient2Accounts = restGetAccountsForClientId(2);

        assertThat(listOfClient2Accounts)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(usdAccount, audAccount);
    }

    private List<Account> restGetAccountsForClientId(int clientId) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/account")
                .queryParam("clientId", clientId)
                .build();

        var responseEntity =
            testRestTemplate
                .exchange(
                    url,
                    GET,
                    null,
                    new ParameterizedTypeReference<List<Account>>() { }
                );
        return responseEntity.getBody();
    }
}
