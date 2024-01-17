package com.oneofalternatives.accountapp.util;

import java.math.BigDecimal;

import static com.oneofalternatives.accountapp.util.MoneyConstants.ROUNDING_MODE;
import static com.oneofalternatives.accountapp.util.MoneyConstants.SCALE;

public class TypeUtils {

    public static BigDecimal scaledBigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE);
    }
}
