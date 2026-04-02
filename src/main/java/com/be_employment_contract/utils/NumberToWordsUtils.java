package com.be_employment_contract.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberToWordsUtils {

    private static final String[] BELOW_TWENTY = {
            "", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười",
            "mười một", "mười hai", "mười ba", "mười bốn", "mười năm", "mười sáu", "mười bảy", "mười tám", "mười chín"
    };
    private static final String[] TENS = {
            "", "", "hai mươi", "ba mươi", "bốn mươi", "năm mươi", "sáu mươi", "bảy mươi", "tám mươi", "chín mươi"
    };

    private NumberToWordsUtils() {
    }

    public static String moneyToWords(BigDecimal amount) {
        if (amount == null) {
            return "Việt Nam đồng";
        }

        long integerPart = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (integerPart == 0L) {
            return "Việt Nam đồng";
        }

        return convert(integerPart).trim() + " đồng";
    }

    private static String convert(long number) {
        if (number < 20) {
            return BELOW_TWENTY[(int) number];
        }
        if (number < 100) {
            return TENS[(int) (number / 10)] + (number % 10 != 0 ? " " + convert(number % 10) : "");
        }
        if (number < 1_000) {
            return convert(number / 100) + " trăm" + (number % 100 != 0 ? " " + convert(number % 100) : "");
        }
        if (number < 1_000_000) {
            return convert(number / 1_000) + " nghìn" + (number % 1_000 != 0 ? " " + convert(number % 1_000) : "");
        }
        if (number < 1_000_000_000) {
            return convert(number / 1_000_000) + " triệu"
                    + (number % 1_000_000 != 0 ? " " + convert(number % 1_000_000) : "");
        }
        return convert(number / 1_000_000_000) + " tỷ"
                + (number % 1_000_000_000 != 0 ? " " + convert(number % 1_000_000_000) : "");
    }
}

