package com.dbn.connection;

import com.dbn.common.lookup.Visitor;
import com.dbn.diagnostics.Diagnostics;
import fleet.kernel.HackyNonBlockingChangeKt;

import java.net.BindException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.*;

public class ConnectionExceptionVisitor implements Visitor<Throwable> {
    private boolean hasBindException;
    private Set<Integer> oraErrorCodes;
    private LinkedHashMap<Integer, String> oraErrorCodeMessages;

    private static Pattern ORA_ERROR_MESSAGE = Pattern.compile("^ORA-(\\d+)(.*)");
    @Override
    public void visit(Throwable element) {
        if (element instanceof SQLException) {
            SQLException sqlExcp = (SQLException) element;
            addOraErrorCode(sqlExcp.getErrorCode());
            String localizedMessage = sqlExcp.getLocalizedMessage();
            if (localizedMessage != null) {
                Matcher matchOraError = ORA_ERROR_MESSAGE.matcher(localizedMessage.trim());
                if (matchOraError.matches()) {
                    addOraErrorCodeMessages(matchOraError.group(1), matchOraError.group(0));
                }
            }
        }
        if (element instanceof BindException) {
            this.hasBindException = true;
        }
    }

    private void addOraErrorCodeMessages(String errorCodeStr, String fullString) {
        try {
            Integer errorCode = Integer.valueOf(errorCodeStr);
            if (this.oraErrorCodeMessages == null){
                this.oraErrorCodeMessages = new LinkedHashMap<>();
            }
            this.oraErrorCodeMessages.put(errorCode, fullString);
        }
        catch (NumberFormatException nfe) {
            Diagnostics.conditionallyLog(nfe);
            return;
        }
    }
    private void addOraErrorCode(int errorCode) {
        if (this.oraErrorCodes == null) {
            this.oraErrorCodes = new HashSet<>();
        }
        oraErrorCodes.add(errorCode);
    }

    public boolean hasBindException() {
        return this.hasBindException;
    }

    public boolean containsOraErrorCodes(int theseCodes) {
        if (this.oraErrorCodes != null) {
            if (this.oraErrorCodes.contains(theseCodes)) {
                return true;
            }
        }
        if (this.oraErrorCodeMessages != null) {
            if (this.oraErrorCodeMessages.keySet().contains(theseCodes)) {
                return true;
            }
        }
        return false;
    }
}
