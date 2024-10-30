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

package com.dbn.assistant.service.exception;

import lombok.Getter;

import java.sql.SQLException;

@Getter

/**
 * Custom exception class for database operation
 * TODO consider simplifying exception handling. DBN gets away with properly handling raw SQLException in all places (TBD).
 */
public class DatabaseOperationException extends SQLException {

  public DatabaseOperationException(String message, SQLException cause) {
    super(message, cause);
  }


  public DatabaseOperationException(String message) {
    super(message);
  }

}
