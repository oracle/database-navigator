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

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom exception class for credential management that enriches SQLExceptions with more user-friendly messages.
 */
public class CredentialManagementException extends DatabaseOperationException {

  public CredentialManagementException(String message, SQLException cause) {
    super(createCustomMessage(message, cause), cause);
  }

  /**
   * Enhances the error message based on the SQL error code.
   * @param baseMessage The original message provided for the exception.
   * @param cause The SQLException that triggered this exception.
   * @return A string combining the original message with a custom message based on the SQL error code.
   */
  private static String createCustomMessage(String baseMessage, SQLException cause) {
    String customMessage = mapErrorCodeToMessage(cause.getErrorCode(), cause.getMessage());
    return String.format("%s - %s (SQL Error Code: %d)", baseMessage, customMessage, cause.getErrorCode());
  }

  /**
   * Maps SQL error codes to custom error messages.
   * @param errorCode The SQL error code.
   * @param defaultMessage The default message in case no error code is recognized
   * @return A custom message for the given error code.
   */
  private static String mapErrorCodeToMessage(int errorCode, String defaultMessage) {
    switch (errorCode) {
      case 20010:
        return "Missing Credential Name";
      case 20020:
        return "Missing credential attribute";
      case 20022:
        String regex = "ORA-20022:([^\\n]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(defaultMessage);

        if (matcher.find()) {
          return matcher.group(1).trim();
        }
      default:
        return defaultMessage;
    }
  }
}
