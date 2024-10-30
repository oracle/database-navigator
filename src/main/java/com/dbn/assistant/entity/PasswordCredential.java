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

package com.dbn.assistant.entity;

import com.dbn.object.type.DBCredentialType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User/password AI provider credential type
 */
@Getter
public class PasswordCredential extends Credential {
  private String password;

  public PasswordCredential(String credentialName, String username, boolean enabled, String comments) {
    super(credentialName, DBCredentialType.PASSWORD, username, enabled, comments);
  }
  public PasswordCredential(String credentialName, String username, String password) {
    super(credentialName, DBCredentialType.PASSWORD, username, true, null);
    this.password = password;
    validate();
  }

  /**
   * validate that the fields aren't empty and that they don't contain "'"
   *
   * @throws IllegalArgumentException when the rules of validation are not respected
   */
  @Override
  public void validate() {
    //TODO should we remove this method since we validate in view side, and this is stopping us from updating credentials properly
//    if (credentialName.isEmpty() || username.isEmpty() || password.isEmpty())
//      throw new IllegalArgumentException("Please don't leave empty fields");
//    if (credentialName.contains("'") || username.contains("'") || password.contains("'"))
//      throw new IllegalArgumentException("Please don't use ' in fields");
  }

  /**
   * Give us a format suitable to be injected in our pl/sql calls
   *
   * @return string of attributes
   */
  @Override
  public String toAttributeMap() {
    return String.format(
        "credential_name => '%s',\n" +
            "username => '%s',\n" +
            "password => '%s'",
            name,
            userName,
        password
    );
  }

  @Override
  public List<String> toUpdatingAttributeList() {
    List<String> output = new ArrayList<>();

    if (!Objects.equals(userName, "")) output.add(toAttributeFormat("username", userName));
    if (!Objects.equals(password, "")) output.add(toAttributeFormat("password", password));
    return output;
  }

  @Override
  public Map<String, String> getAttributes() {
    return Map.of(
          "username", userName,
          "password", password);
  }
}
