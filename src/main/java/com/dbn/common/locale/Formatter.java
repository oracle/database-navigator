/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.locale;

import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.sign.Signed;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.data.value.ValueAdapter;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;


@Getter
@Setter
@EqualsAndHashCode
public class Formatter implements Cloneable, Signed {
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    private DateFormat dateTimeFormat;
    private DecimalFormat numberFormat;
    private DecimalFormat integerFormat;

    private String dateFormatPattern;
    private String timeFormatPattern;
    private String datetimeFormatPattern;
    private String numberFormatPattern;
    private String integerFormatPattern;

    private int signature;

    private static final ThreadLocal<Formatter> localFormatter = new ThreadLocal<>();

    private Formatter() {
    }

    public Formatter(int signature, @NotNull Locale locale, DBDateFormat dateFormatOption, DBNumberFormat numberFormatOption) {
        this.signature = signature;
        int dFormat = dateFormatOption.getFormat();
        dateFormat = SimpleDateFormat.getDateInstance(dFormat, locale);
        timeFormat = SimpleDateFormat.getTimeInstance(dFormat, locale);
        dateTimeFormat = SimpleDateFormat.getDateTimeInstance(dFormat, dFormat, locale);


        boolean groupingUsed = numberFormatOption == DBNumberFormat.GROUPED;

        integerFormat = (DecimalFormat) NumberFormat.getIntegerInstance(locale);
        integerFormat.setGroupingUsed(groupingUsed);

        numberFormat = (DecimalFormat) DecimalFormat.getInstance(locale);
        numberFormat.setGroupingUsed(groupingUsed);
        numberFormat.setMaximumFractionDigits(20);
        numberFormat.setParseBigDecimal(true);

        dateFormatPattern = ((SimpleDateFormat) dateFormat).toPattern();
        timeFormatPattern = ((SimpleDateFormat) timeFormat).toPattern();
        datetimeFormatPattern = ((SimpleDateFormat) dateTimeFormat).toPattern();
        numberFormatPattern = numberFormat.toPattern();
        integerFormatPattern = integerFormat.toPattern();
    }

    public Formatter(int signature, @NotNull Locale locale, String dateFormatPattern, String timeFormatPattern, String numberFormatPattern) {
        this.signature = signature;
        if (Strings.isEmptyOrSpaces(dateFormatPattern)) throw new IllegalArgumentException("Date format pattern empty.");
        if (Strings.isEmptyOrSpaces(timeFormatPattern)) throw new IllegalArgumentException("Time format pattern empty.");
        if (Strings.isEmptyOrSpaces(numberFormatPattern)) throw new IllegalArgumentException("Number format pattern empty.");
        this.dateFormatPattern = dateFormatPattern;
        this.timeFormatPattern = timeFormatPattern;
        this.datetimeFormatPattern = dateFormatPattern + ' ' + timeFormatPattern;
        this.numberFormatPattern = numberFormatPattern;

        int fractionIndex = numberFormatPattern.lastIndexOf('.');
        if (fractionIndex > -1) {
            this.integerFormatPattern = numberFormatPattern.substring(0, fractionIndex);
        } else {
            this.integerFormatPattern = numberFormatPattern;
        }

        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
        dateFormat = new SimpleDateFormat(this.dateFormatPattern, dateFormatSymbols);
        timeFormat = new SimpleDateFormat(this.timeFormatPattern, dateFormatSymbols);
        dateTimeFormat = new SimpleDateFormat(this.datetimeFormatPattern, dateFormatSymbols);

        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
        numberFormat = new DecimalFormat(this.numberFormatPattern, decimalFormatSymbols);
        integerFormat = new DecimalFormat(this.integerFormatPattern, decimalFormatSymbols);
        integerFormat.setMaximumFractionDigits(0);
    }

    public static Formatter create(RegionalSettings settings) {
        return settings.createFormatter();
    }

    public static Formatter getInstance(@NotNull Project project) {
        Formatter localFormatter = Formatter.localFormatter.get();
        Formatter baseFormatter = RegionalSettings.getInstance(project).getBaseFormatter();
        if (localFormatter == null || localFormatter.getSignature() != baseFormatter.getSignature()) {
            localFormatter = baseFormatter.clone();
            Formatter.localFormatter.set(localFormatter);
        }
        return localFormatter;
    }

    public String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public Date parseDate(String string) throws ParseException {
        return dateFormat.parse(string);
    }

    public String formatTime(Date date) {
        return timeFormat.format(date);
    }

    public Date parseTime(String string) throws ParseException {
        return timeFormat.parse(string);
    }

    public String formatDateTime(Date date) {
        return dateTimeFormat.format(date);
    }

    public Date parseDateTime(String string) throws ParseException {
        try {
            return dateTimeFormat.parse(string);
        } catch (ParseException e) {
            conditionallyLog(e);
            return dateFormat.parse(string);
        }
    }


    public String formatNumber(Number number) {
        return numberFormat.format(number);
    }

    public Number parseNumber(String string) throws ParseException {
        return numberFormat.parse(string);
    }

    public String formatInteger(Number number) {
        return integerFormat.format(number);
    }

    public Number parseInteger(String string) throws ParseException {
        return integerFormat.parse(string);
    }

    public String formatObject(Object object) {
        if (object != null) {
            return
                object instanceof Number ? formatNumber((Number) object) :
                object instanceof Date ? formatDateTime((Date) object) :
                object instanceof String ? (String) object :
                object instanceof ValueAdapter ? ((ValueAdapter) object).getDisplayValue() :
                object.toString();
        } else {
            return null;
        }
    }

    public Object parseObject(Class clazz, String string) throws ParseException {
        if (Date.class.isAssignableFrom(clazz)) {
            return parseDateTime(string);
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return parseNumber(string);
        }
        return string;
    }

    @Override
    public Formatter clone() {
        Formatter clone = new Formatter();
        clone.dateFormat = (DateFormat) dateFormat.clone();
        clone.timeFormat = (DateFormat) timeFormat.clone();
        clone.dateTimeFormat = (DateFormat) dateTimeFormat.clone();
        clone.numberFormat = (DecimalFormat) numberFormat.clone();
        clone.integerFormat = (DecimalFormat) integerFormat.clone();

        clone.dateFormatPattern = dateFormatPattern;
        clone.timeFormatPattern = timeFormatPattern;
        clone.datetimeFormatPattern = datetimeFormatPattern;
        clone.numberFormatPattern = numberFormatPattern;
        clone.integerFormatPattern = integerFormatPattern;
        return clone;
    }
}
