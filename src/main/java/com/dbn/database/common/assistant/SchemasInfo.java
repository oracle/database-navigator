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

import com.dbn.database.common.statement.CallableStatementOutput;
import lombok.Getter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
/**
 * CallableStatementOutput to get information about schemas
 *
 * @deprecated replaced with DBN "native" DBSchema object lookup
 */
public class SchemasInfo implements CallableStatementOutput {
  private List<String> schemaList;

  @Override
  public void registerParameters(CallableStatement statement) throws SQLException {
  }

  @Override
  public void read(CallableStatement statement) throws SQLException {
    schemaList = new ArrayList<>();

    ResultSet rs = statement.executeQuery();
    while (rs.next()) {
      String owner = rs.getString("OWNER");
      schemaList.add(owner);
    }
  }
}
