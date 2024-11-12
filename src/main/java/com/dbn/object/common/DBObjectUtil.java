/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.object.common;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.convert;

@UtilityClass
public class DBObjectUtil {
    private static final Gson GSON = new GsonBuilder().create();

    public static void refreshUserObjects(ConnectionId connectionId, DBObjectType objectType) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return;

        String userName = connection.getUserName();
        refreshObjects(connectionId, userName, objectType);
    }

    public static void refreshObjects(ConnectionId connectionId, @Nullable String ownerName, DBObjectType objectType) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return;

        DBObjectList objectList;
        DBObjectBundle objectBundle = connection.getObjectBundle();
        if (ownerName == null) {
            objectList = objectBundle.getObjectList(objectType);
        } else {
            DBSchema schema = objectBundle.getSchema(ownerName);
            objectList = schema == null ? null : schema.getChildObjectList(objectType);
        }

        if (objectList == null) return;
        objectList.markDirty();
    }


    public static Map<String, String> objectToAttributes(DBObjectRef object) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("owner", nvl(object.getSchemaName(), ""));
        map.put("name", nvl(object.getObjectName(), ""));
        return map;
    }

    public static String objectListToJson(List<DBObjectRef<DBObject>> objects) {
        return GSON.toJson(convert(objects, o -> objectToAttributes(o)));
    }

    public static List<DBObjectRef<?>> jsonToObjectList(ConnectionId connectionId, String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();

        List<DBObjectRef<?>> objects = new ArrayList<>();
        List<Map<String, String>> list = GSON.fromJson(json, List.class);
        for (Map<String, String> map : list) {
            String ownerName = map.get("owner");
            String objectName = map.get("name");
            if (ownerName == null) continue;

            DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, DBObjectType.SCHEMA, ownerName);
            if (objectName == null) {
                objects.add(schema);
            } else {
                DBObjectRef<?> schemaObject = new DBObjectRef<>(schema, DBObjectType.ANY, objectName);
                objects.add(schemaObject);
            }
        }
        return objects;
    }
}
