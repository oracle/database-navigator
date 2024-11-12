package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBCredentialMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBCredentialMetadataImpl extends DBObjectMetadataBase implements DBCredentialMetadata {

    public DBCredentialMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getCredentialName() throws SQLException {
        return getString("CREDENTIAL_NAME");
    }

    public String getCredentialType() throws SQLException {
        return getString("CREDENTIAL_TYPE");
    }

    @Override
    public String getUserName() throws SQLException {
        return getString("USER_NAME");
    }

    @Override
    public String getComments() throws SQLException {
        return getString("COMMENTS");
    }

    @Override
    public boolean isEnabled() throws SQLException {
        return isYesFlag("IS_ENABLED");
    }
}
