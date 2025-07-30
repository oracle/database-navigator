package com.dbn.database.common.statement;

import lombok.Getter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

@Getter
public class BooleanValue implements CallableStatementOutput {
    private boolean value;

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        statement.registerOutParameter(1, Types.CHAR);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        String result = statement.getString(1);
        value = "Y".equalsIgnoreCase(result != null ? result.trim() : "");
    }
}
