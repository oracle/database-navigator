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

package com.dbn.assistant.service.mock;

import com.dbn.assistant.entity.DBObjectItem;
import com.dbn.assistant.service.DatabaseService;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Mockup database service.
 * This service use data from JSON dump files.
 * Default location are
 *     /var/tmp/schemas.json
 *     /var/tmp/dbitems.json
 * Location can be overided by following system properties
 *  fake.services.schema.dump
 *  fake.services.dbitems.dump
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class FakeDatabaseService implements DatabaseService {

    Type SCHEMA_TYPE = new TypeToken<List<String>>() {
    }.getType();
    Type DBOJB_TYPE = new TypeToken<List<DBObjectItem>>() {
    }.getType();
    String schemasRepoFilename = System.getProperty("fake.services.schema.dump", "/var/tmp/schemas.json");
    String dbobjRepoFilename = System.getProperty("fake.services.dbitems.dump", "/var/tmp/dbitems.json");

    List<String> schemas = null;
    Map<String, List<DBObjectItem>> objs = null;

    @Override
    public CompletableFuture<List<String>> getSchemaNames() {
        if (schemas == null) {
            try {
                this.schemas = new Gson().fromJson(new FileReader(schemasRepoFilename), SCHEMA_TYPE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("cannot read schemas list " + e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(this.schemas);
    }

    @Override
    public CompletableFuture<List<DBObjectItem>> getObjectItemsForSchema(String schema) {
        if (objs == null) {
            this.objs = new HashMap<>();
            Gson g = new GsonBuilder().setLenient().create();
            // expected file format
            // <schema name>:<json array><CR>
            try {

                BufferedReader fr = new BufferedReader(new FileReader(dbobjRepoFilename));
                StringBuffer schemaName = new StringBuffer();
                boolean eofReached = false;
                boolean markerReached = false;
                // read schema
                while (!eofReached) {
                    int c = 0;
                    markerReached = false;
                    c = fr.read();
                    switch (c) {
                        case (int)':':
                            markerReached=true;
                            break;
                        case -1:
                            eofReached=true;
                            break;
                        default:
                            schemaName.append((char) c);
                    }
                    if (eofReached) {
                        break;
                    }
                    if (markerReached) {
                        log.debug("new schema :" + schemaName);
                        String jl = fr.readLine();
                        List<DBObjectItem> l = g.fromJson(jl, DBOJB_TYPE);
                        log.debug("new obj :" + l);
                        this.objs.put(schemaName.toString(), l);
                        schemaName.delete(0, schemaName.length());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("cannot read db obj list " + e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(this.objs.get(schema));
    }

    @Override
    public CompletionStage<Void> grantACLRights(String command) {
        return null;
    }

    @Override
    public CompletionStage<Void> grantPrivilege(String username) {
        return null;
    }

    @Override
    public CompletionStage<Void> isUserAdmin() {
        return null;
    }
}
