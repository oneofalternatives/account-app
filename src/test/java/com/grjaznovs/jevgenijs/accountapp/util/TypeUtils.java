package com.grjaznovs.jevgenijs.accountapp.util;

import java.math.BigDecimal;

import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.ROUNDING_MODE;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.SCALE;

public class TypeUtils {

    public static BigDecimal scaledBigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE);
    }
}
