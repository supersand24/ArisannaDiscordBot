package dev.supersand24;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    public static String formatAsUSD(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        return formatter.format(amount);
    }

}
