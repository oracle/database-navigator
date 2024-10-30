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

package com.dbn.database.common.assistant;

import com.dbn.assistant.entity.DBObjectItem;
import com.dbn.assistant.entity.DatabaseObjectType;
import com.dbn.database.common.statement.CallableStatementOutput;
import lombok.Getter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
/**
 * CallableStatementOutput to get list of tables and views
 * @see OracleAssistantInterface.listObjectListItems()
 *
 * @deprecated this space should not have references to any classes outside "com.dbn.database" (replaced with DBN "native" DBTable / DBView object lookup)
 */
public class TableAndViewListInfo implements CallableStatementOutput {
  private List<DBObjectItem> DBObjectListItems;
  private String schemaName;
  private final DatabaseObjectType type;

  private final String OBJ_OWNER_COLUMN_NANE = "OWNER";


  public TableAndViewListInfo(String schemaName, DatabaseObjectType type) {
    this.schemaName = schemaName;
    this.type = type;
  }

  @Override
  public void registerParameters(CallableStatement statement) throws SQLException {
  }

  @Override
  public void read(CallableStatement statement) throws SQLException {
    DBObjectListItems = new ArrayList<>();

    ResultSet rs = statement.executeQuery();
    while (rs.next()) {
      //String owner = rs.getString(OBJ_OWNER_COLUMN_NANE);
      String tableName = rs.getString(type.getColumnName());
      DBObjectListItems.add(new DBObjectItem(this.schemaName, tableName, type));
    }
  }
}
