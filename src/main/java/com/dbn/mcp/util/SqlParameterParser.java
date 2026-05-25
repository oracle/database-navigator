package com.dbn.mcp.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParameterParser {
    private static final Pattern PARAM = Pattern.compile(":(\\w+)");
    private static final Pattern COMMENT = Pattern.compile("--[^\\r\\n]*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");

    private SqlParameterParser() {}

    public static List<String> parseOccurrences(String sql) {
        List<String> result = new ArrayList<>();
        if (sql == null) return result;
        Matcher m = PARAM.matcher(stripComments(sql));
        while (m.find()) result.add(m.group(1));
        return result;
    }

    public static List<String> uniqueInOrder(List<String> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    public static String stripColon(String param) {
        if (param == null) return "";
        return param.startsWith(":") ? param.substring(1) : param;
    }

    public static String stripComments(String sql) {
        return sql != null ? COMMENT.matcher(sql).replaceAll("") : "";
    }

    public static RewrittenSql rewriteToJdbc(String sql) {
        if (sql == null) return new RewrittenSql("", List.of());

        StringBuilder out = new StringBuilder();
        List<String> params = new ArrayList<>();
        int pos = 0;

        Matcher comments = COMMENT.matcher(sql);
        while (comments.find()) {
            rewriteSegment(sql.substring(pos, comments.start()), out, params);
            out.append(comments.group());
            pos = comments.end();
        }
        rewriteSegment(sql.substring(pos), out, params);

        return new RewrittenSql(out.toString(), params);
    }

    private static void rewriteSegment(String segment, StringBuilder out, List<String> params) {
        Matcher m = PARAM.matcher(segment);
        int pos = 0;
        while (m.find()) {
            out.append(segment, pos, m.start()).append("?");
            params.add(m.group(1));
            pos = m.end();
        }
        out.append(segment.substring(pos));
    }

    public static class RewrittenSql {
        private final String sql;
        private final List<String> paramOrder;

        public RewrittenSql(String sql, List<String> paramOrder) {
            this.sql = sql;
            this.paramOrder = paramOrder;
        }

        public String getSql() { return sql; }
        public List<String> getParamOrder() { return paramOrder; }
    }
}
