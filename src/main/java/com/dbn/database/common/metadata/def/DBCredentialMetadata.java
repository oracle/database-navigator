package com.dbn.database.common.metadata.def;

import com.dbn.database.common.metadata.DBObjectMetadata;

import java.sql.SQLException;

public interface DBCredentialMetadata extends DBObjectMetadata {

    String getCredentialName() throws SQLException;

    String getCredentialType() throws SQLException;

    String getUserName() throws SQLException;

    String getComments() throws SQLException;

    boolean isEnabled() throws SQLException;

}
