package com.dbn.object.management.adapter.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.object.DBAIModel;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.type.DBObjectType.AI_MODEL;

public class DBAIModelManagementAdapter extends ObjectManagementAdapterBase<DBAIModel> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return array(AI_MODEL);
    }

    @Override
    protected void createObject(ConnectionHandler connection, DBNConnection conn, DBAIModel object) throws SQLException {
//        DatabaseVectorInterface databaseInterface = connection.getAssistantInterface();
//        String profileName = object.getName(true);
//        String profileOwner = object.getSchemaName(true);
//        String description = object.getDescription();
//
//        String attributes = object.getAttributesJson();
//        databaseInterface.createProfile(conn, profileName, attributes, description);
//
//        // update status
//        Unsafe.warned(() -> {
//            if (object.isEnabled())
//                databaseInterface.enableProfile(conn, profileOwner, profileName);
//            else
//                databaseInterface.disableProfile(conn, profileOwner, profileName);
//        });
    }

    @Override
    protected void updateObject(ConnectionHandler connection, DBNConnection conn, DBAIModel object) throws SQLException {
//        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
//        String profileName = object.getName(true);
//        String profileOwner = object.getSchemaName(true);
//
//        String attributes = object.getAttributesJson();
//        databaseInterface.updateProfile(conn, profileName, attributes);
//
//        Unsafe.warned(() -> {
//            if (object.isEnabled())
//                databaseInterface.enableProfile(conn, profileOwner, profileName); else
//                databaseInterface.disableProfile(conn, profileOwner, profileName);
//        });
    }

    @Override
    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, DBAIModel object) throws SQLException {
        DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
        vectorInterface.dropModel(conn,
                object.getSchemaName(true),
                object.getName(true));
    }

    @Override
    protected void enableObject(ConnectionHandler connection, DBNConnection conn, DBAIModel object) throws SQLException {
//        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
//        databaseInterface.enableProfile(conn,
//                object.getSchemaName(true),
//                object.getName(true));
    }

    @Override
    protected void disableObject(ConnectionHandler connection, DBNConnection conn, DBAIModel object) throws SQLException {
//        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
//        databaseInterface.disableProfile(conn,
//                object.getSchemaName(true),
//                object.getName(true));
    }
}
