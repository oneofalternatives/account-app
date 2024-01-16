package com.oneofalternatives.accountapp.integrationtest;

import com.oneofalternatives.accountapp.api.AccountProjection;
import com.oneofalternatives.accountapp.api.CreateAccountProjection;
import com.oneofalternatives.accountapp.api.PageProjection;
import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection;
import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.model.Transaction;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.*;

@Repository
public class TestAccountAppRestClient {

    private static final UriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    private final TestRestTemplate testRestTemplate;

    public TestAccountAppRestClient(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    public List<Account> getAllAccounts() {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/account")
                .build();

        return
            assertOkAndGetBody(
                testRestTemplate.exchange(url, GET, null, new ParameterizedTypeReference<>() { })
            );
    }

    public List<AccountProjection> getAccountsFor(int clientId) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/client/" + clientId + "/account")
//                .pathSegment("/account")
                .build();

        return
            assertOkAndGetBody(
                testRestTemplate.exchange(url, GET, null, new ParameterizedTypeReference<>() { })
            );
    }

    public Account putAccountSuccess(CreateAccountProjection createAccountRequest) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/account")
                .build();

        return
            assertOkAndGetBody(
                testRestTemplate.exchange(url, PUT, new HttpEntity<>(createAccountRequest), Account.class)
            );
    }

    public PageProjection<TransactionHistoryRecordProjection> getTransactionHistoryFor(
        int accountId,
        Paging paging
    ) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/transaction/history")
                .queryParam("accountId", accountId)
                .queryParam("offset", paging.offset())
                .queryParam("limit", paging.limit())
                .build();

        return assertOkAndGetBody(testRestTemplate.exchange(url, GET, null, new ParameterizedTypeReference<>() { }));
    }

    public Transaction postFundTransferSuccess(
        int senderAccountId,
        int receiverAccountId,
        double amount
    ) {
        return
            assertOkAndGetBody(
                postFundTransfer(
                    senderAccountId,
                    receiverAccountId,
                    amount,
                    Transaction.class
                )
            );
    }

    public ResponseEntity<String> postFundTransferFail(
        int senderAccountId,
        int receiverAccountId,
        double amount
    ) {
        return
            postFundTransfer(
                senderAccountId,
                receiverAccountId,
                amount,
                String.class
            );
    }

    private <T> ResponseEntity<T> postFundTransfer(
        int senderAccountId,
        int receiverAccountId,
        double amount,
        Class<T> responseBodyType
    ) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/transaction/fund-transfer")
                .queryParam("senderAccountId", senderAccountId)
                .queryParam("receiverAccountId", receiverAccountId)
                .queryParam("amount", BigDecimal.valueOf(amount))
                .build();

        return testRestTemplate.exchange(url, POST, null, responseBodyType);
    }

    private <T> T assertOkAndGetBody(
        ResponseEntity<T> responseEntity
    ) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        var body = responseEntity.getBody();
        assertThat(body).isNotNull();

        return body;
    }

    public record Paging(int offset, int limit) {

        public static Paging of(int offset, int limit) {
            return new Paging(offset, limit);
        }
    }
}
