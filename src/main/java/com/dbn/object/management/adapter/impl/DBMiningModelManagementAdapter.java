package com.dbn.object.management.adapter.impl;

import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.object.DBMiningModel;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.management.ObjectManagementRequest;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.management.ObjectManagementRequest.NEW_NAME;
import static com.dbn.object.type.DBObjectType.MINING_MODEL;

public class DBMiningModelManagementAdapter extends ObjectManagementAdapterBase<DBMiningModel> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return array(MINING_MODEL);
    }

    @Override
    protected void renameObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
        DBMiningModel object = request.object();

        DatabaseMachineLearningInterface databaseInterface = request.getMachineLearningInterface();
        databaseInterface.renameModel(
                request.conn(),
                object.getSchemaName(),
                object.getName(),
                request.requireAttribute(NEW_NAME));
    }

    @Override
    protected void createObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
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
    protected void updateObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
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
    protected void deleteObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
        DBMiningModel object = request.object();

        DatabaseVectorInterface databaseInterface = request.getVectorInterface();
        databaseInterface.dropModel(request.conn(),
                object.getSchemaName(true),
                object.getName(true));
    }

    @Override
    protected void enableObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
//        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
//        databaseInterface.enableProfile(conn,
//                object.getSchemaName(true),
//                object.getName(true));
    }

    @Override
    protected void disableObject(ObjectManagementRequest<DBMiningModel> request) throws SQLException {
//        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
//        databaseInterface.disableProfile(conn,
//                object.getSchemaName(true),
//                object.getName(true));
    }
}
