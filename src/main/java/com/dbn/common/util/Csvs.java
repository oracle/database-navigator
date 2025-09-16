/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.util;

import com.dbn.common.data.Data;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.dbn.common.util.Unsafe.cast;
import static java.util.Collections.emptyList;

@UtilityClass
public class Csvs {

    public static <T> String valuesToCsv(List<T> values, Function<T, String> converter) {
        if (values == null) return "";
        if (values.isEmpty()) return "";
        if (values.get(0) instanceof String) return stringsToCsv(cast(values));

        return Lists.toCsv(values, converter);
    }

    public static <T> List<T> csvToValues(String csv, Function<String, T> converter) {
        if (csv == null) return emptyList();
        if (csv.isEmpty()) return emptyList();
        if (converter.apply("1") instanceof String) {
            return cast(csvToStrings(csv));
        }

        return Lists.fromCsv(csv, converter);

    }


    public static String stringsToCsv(List<String> strings) {
        if (strings == null) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) builder.append(",");
            String string = strings.get(i);
            if (string == null) {
                builder.append(Data.NULL);
            } else {
                builder.append('"');
                for (char c : string.toCharArray()) {
                    if (c == '"') builder.append("\"");
                    builder.append(c);
                }
                builder.append('"');
            }
        }
        return builder.toString();
    }


    public static List<String> csvToStrings(String csv) {
        if (csv == null || csv.isEmpty()) return new ArrayList<>();

        List<String> strings = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean inQuotes = false;
        boolean isNullCandidate = true;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        builder.append('"');
                        i++; // skip second quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    builder.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    isNullCandidate = false;
                } else if (c == ',') {
                    String string = builder.toString();
                    if (isNullCandidate && string.equals(Data.NULL)) {
                        strings.add(null);
                    } else {
                        strings.add(string);
                    }
                    builder.setLength(0);
                    isNullCandidate = true;
                } else {
                    builder.append(c);
                }
            }
        }

        String field = builder.toString();
        if (isNullCandidate && field.equals(Data.NULL)) {
            strings.add(null);
        } else {
            strings.add(field);
        }


        return strings;
    }

}
