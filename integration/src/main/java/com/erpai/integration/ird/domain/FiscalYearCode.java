package com.erpai.integration.ird.domain;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing a Nepalese fiscal year code.
 * UI format: "2083/84" -> CBMS dot format: "2083.084".
 */
public final class FiscalYearCode {

    private static final Pattern UI_PATTERN = Pattern.compile("(\\d{4})/(\\d{2})");
    private static final Pattern CBMS_PATTERN = Pattern.compile("(\\d{4})\\.(\\d{3})");

    private final String uiFormat;
    private final String cbmsFormat;

    private FiscalYearCode(String uiFormat, String cbmsFormat) {
        this.uiFormat = uiFormat;
        this.cbmsFormat = cbmsFormat;
    }

    public static FiscalYearCode fromUi(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Fiscal year code must not be null");
        }
        Matcher m = UI_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Fiscal year code must be in UI format 'YYYY/YY', got: " + raw);
        }
        String year = m.group(1);
        String suffix = m.group(2);
        String cbms = year + "." + "0" + suffix;
        return new FiscalYearCode(raw.trim(), cbms);
    }

    public static FiscalYearCode fromCbms(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Fiscal year code must not be null");
        }
        Matcher m = CBMS_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Fiscal year code must be in CBMS format 'YYYY.YYY', got: " + raw);
        }
        String year = m.group(1);
        String suffix = m.group(2);
        String ui = year + "/" + suffix.substring(1);
        return new FiscalYearCode(ui, raw.trim());
    }

    public String uiFormat() {
        return uiFormat;
    }

    public String cbmsFormat() {
        return cbmsFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FiscalYearCode that)) return false;
        return Objects.equals(cbmsFormat, that.cbmsFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cbmsFormat);
    }

    @Override
    public String toString() {
        return cbmsFormat;
    }
}
