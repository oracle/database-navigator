package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBProfileMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBProfileMetadataImpl extends DBObjectMetadataBase implements DBProfileMetadata {

    public DBProfileMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getProfileName() throws SQLException {
        return getString("PROFILE_NAME");
    }

    public String getCredentialName() throws SQLException {
        return getString("CREDENTIAL_NAME");
    }

    @Override
    public String getProvider() throws SQLException {
        return getString("PROVIDER");
    }

    @Override
    public String getModel() throws SQLException {
        return getString("MODEL");
    }

    @Override
    public String getObjectList() throws SQLException {
        return getString("OBJECT_LIST");
    }

    @Override
    public String getDescription() throws SQLException {
        return getString("DESCRIPTION");
    }

    @Override
    public double getTemperature() throws SQLException {
        return resultSet.getDouble("TEMPERATURE");
    }

    @Override
    public boolean isEnabled() throws SQLException {
        return isYesFlag("IS_ENABLED");
    }
}
