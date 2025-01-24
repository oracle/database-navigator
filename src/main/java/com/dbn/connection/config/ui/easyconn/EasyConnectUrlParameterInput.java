package com.dbn.connection.config.ui.easyconn;

import java.util.Collections;
import java.util.Map;

public class EasyConnectUrlParameterInput {

    private Map<String, String> parameters;

    public EasyConnectUrlParameterInput(Map<String,String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return an unmodifiable map of Parameter name/value pairs.
     */
    public Map<String, String> getExistingParameterValues() {
        return this.parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
