package com.dbn.execution.common.input;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Provides JSON-style encoding / decoding for collections of Strings.
 */
@Slf4j
public final class StringCollectionPayloadMapper {
    private static final StringCollectionPayloadMapper INSTANCE = new StringCollectionPayloadMapper();

    private StringCollectionPayloadMapper() {
        // no-op
    }
    public static StringCollectionPayloadMapper getInstance() {
        return INSTANCE;
    }
    /* Primary encoder */
    public String encode(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < values.size(); i++) {
                sb.append('"')
                        .append(escape(values.get(i)))
                        .append('"');
                if (i < values.size() - 1) sb.append(',');
        }

        sb.append(']');
        return sb.toString();
    }
    /* Primary decoder */
    public List<String> decodeToList(String encoded) {
        List<String> result = new ArrayList<>();
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        if(encoded != null && !encoded.isEmpty()) {
            for (int i = 1; i < encoded.length() - 1; i++) {
                char c = encoded.charAt(i);
                if (c == '\\') {
                    if (i + 1 < encoded.length()) {
                        char next = encoded.charAt(i + 1);
                        if (next == '\\' || next == '\"') {
                            current.append(next);
                            i++;
                        }
                    }
                } else if (c == '"') {
                    inString = !inString;
                    if (!inString) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                } else if (inString) {
                    current.append(c);
                }
            }
        }
        return result;
    }
    public String encode(String[] array) {
        if (array == null) return "[]";
        return encode(Arrays.asList(array));
    }
    public String[] decodeToArray(String encoded) {
        List<String> list = decodeToList(encoded);
        return list.toArray(new String[0]);
    }
    /* ----------  Internal helpers  ---------- */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}