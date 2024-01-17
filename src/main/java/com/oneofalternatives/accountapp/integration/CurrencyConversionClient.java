package com.oneofalternatives.accountapp.integration;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

public interface CurrencyConversionClient {

    Set<Currency> getSupportedCurrencies();

    BigDecimal getDirectRate(Currency fromCurrency, Currency toCurrency);
}
