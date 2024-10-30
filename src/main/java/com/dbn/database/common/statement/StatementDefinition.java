package com.dbn.database.common.statement;

import com.dbn.common.util.TransientId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import lombok.Getter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.dbn.common.util.Commons.nvl;

@Getter
public class StatementDefinition {
    private static final String DBN_PARAM_PLACEHOLDER = "DBN_PARAM_PLACEHOLDER";
    private final String statementText;
    private final Integer[] placeholderIndexes;

    private final TransientId id = TransientId.create();
    private final boolean prepared;

    StatementDefinition(String statementText, String prefix, boolean prepared) {
        this.prepared = prepared;
        statementText = statementText.replaceAll("\\s+", " ").trim();
        if (prefix != null) {
            statementText = statementText.replaceAll("\\[PREFIX]", prefix);
        }

        StringBuilder buffer = new StringBuilder();
        List<Integer> placeholders = new ArrayList<>();
        int startIndex = statementText.indexOf('{');
        if (startIndex == -1) {
            buffer.append(statementText);
        } else {
            int endIndex = 0;
            while (startIndex > -1) {
                String segment = statementText.substring(endIndex, startIndex);
                buffer.append(segment).append(prepared ? "?" : DBN_PARAM_PLACEHOLDER);
                endIndex = statementText.indexOf('}', startIndex);
                String placeholder = statementText.substring(startIndex + 1, endIndex);

                placeholders.add(Integer.valueOf(placeholder));
                startIndex = statementText.indexOf('{', endIndex);
                endIndex = endIndex + 1;
            }
            if (endIndex < statementText.length()) {
                buffer.append(statementText.substring(endIndex));
            }
        }
        this.statementText = buffer.toString();
        this.placeholderIndexes = placeholders.toArray(new Integer[0]);
    }

    DBNPreparedStatement<?> prepareStatement(DBNConnection connection, Object[] arguments) throws SQLException {
        DBNPreparedStatement<?> preparedStatement = connection.prepareStatementCached(statementText);
        for (int i = 0; i < placeholderIndexes.length; i++) {
            Integer argumentIndex = placeholderIndexes[i];
            Object argumentValue = arguments[argumentIndex];
                preparedStatement.setObject(i + 1, argumentValue);
        }
        return preparedStatement;
    }

    String prepareStatementText(Object... arguments) {
        String statementText = this.statementText;
        for (Integer argumentIndex : placeholderIndexes) {
            String argumentValue = Matcher.quoteReplacement(nvl(arguments[argumentIndex], "").toString());
            statementText = statementText.replaceFirst(prepared ? "\\?" : DBN_PARAM_PLACEHOLDER, argumentValue);
        }
        return statementText;
    }


    @Override
    public String toString() {
        return statementText;
    }
}
