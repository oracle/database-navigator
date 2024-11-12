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

import com.dbn.common.exception.Exceptions;
import com.dbn.database.common.statement.CallableStatementOutput;
import lombok.Getter;

import java.io.BufferedReader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Callable statement output for reading a clob parameter
 * (POC on streamable AI responses to produce gradual output effect - unsuccessful so far)
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class AssistantQueryResponse implements CallableStatementOutput {

  private Clob response;

  @Override
  public void registerParameters(CallableStatement statement) throws SQLException {
    statement.registerOutParameter(1, Types.CLOB);
  }

  @Override
  public void read(CallableStatement statement) throws SQLException {
    response = statement.getClob(1);
  }

  public String read() throws SQLException {
      StringBuilder builder = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(response.getCharacterStream())){
          String line;
          while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
          }
      } catch (Throwable e) {
          throw Exceptions.toSqlException(e);
      }

      return builder.toString().trim();
  }
}
