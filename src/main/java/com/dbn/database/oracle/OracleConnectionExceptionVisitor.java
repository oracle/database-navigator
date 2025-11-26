package com.dbn.database.oracle;

import com.dbn.common.lookup.Visitor;
import com.dbn.diagnostics.Diagnostics;

import java.net.BindException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.*;

/**
 * A visitor that assembles information about a connection exception
 * that allows decision making about extra processing of the error such
 * as bug workarounds.
 */
public class OracleConnectionExceptionVisitor implements Visitor<Throwable> {
    private boolean hasBindException;
    private Set<Integer> oraErrorCodes;
    private LinkedHashMap<Integer, String> oraErrorCodeMessages;

    /**
     * Pulls the ORA failure code out of thie message of a SQLException that was
     * thrown by an Oracle driver.
     */
    private static Pattern ORA_ERROR_MESSAGE = Pattern.compile("^ORA-(\\d+)(.*)");

    /**
     *
     * @param element
     */
    @Override
    public void visit(Throwable element) {
        if (element instanceof SQLException) {
            SQLException sqlExcp = (SQLException) element;
            // try to pull the error code right out of the exception.
            addOraErrorCode(sqlExcp.getErrorCode());
            // try to derive the error code from the message
            String localizedMessage = sqlExcp.getLocalizedMessage();
            if (localizedMessage != null) {
                Matcher matchOraError = ORA_ERROR_MESSAGE.matcher(localizedMessage.trim());
                if (matchOraError.matches()) {
                    addOraErrorCodeMessages(matchOraError.group(1), matchOraError.group(0));
                }
            }
        }
        // a bind exception can occur if we do an interactive token authentication
        // and the expected call back port is already bound.
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

    public boolean containsOraErrorCodes(final Set<Integer> theseCodes) {
        if (this.oraErrorCodes != null) {
             if (theseCodes.stream().anyMatch(checkCode -> this.oraErrorCodes.contains(checkCode))) {
                 return true;
             }
        }
        if (this.oraErrorCodeMessages != null) {
            if (theseCodes.stream().anyMatch(checkCode -> this.oraErrorCodes.contains(checkCode))) {
                return true;
            }
        }
        return false;
    }
}
