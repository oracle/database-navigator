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

@Getter

/**
 * OCI AI provider credential type
 */
public class OciCredential extends Credential {
  private String userTenancyOCID;
  private String privateKey;
  private String fingerprint;

  public OciCredential(String credentialName, String userOcid, boolean enabled, String comments) {
    super(credentialName, DBCredentialType.OCI, userOcid, enabled, comments);
  }

  public OciCredential(String credentialName, String userOcid, String tenancyOcid, String privateKey, String fingerprint) {
    super(credentialName, DBCredentialType.OCI, userOcid, true, null);
    this.userTenancyOCID = tenancyOcid;
    this.privateKey = privateKey;
    this.fingerprint = fingerprint;
    validate();
  }

  /**
   * validate that the fields aren't empty and that they don't contain "'"
   *
   * @throws IllegalArgumentException when the rules of validation are not respected
   */
  @Override
  public void validate() {
/*    if (credentialName.isEmpty() || username.isEmpty() || userTenancyOCID.isEmpty() || privateKey.isEmpty() || fingerprint.isEmpty())
      throw new IllegalArgumentException("Please don't leave empty fields");
    if (credentialName.contains("'") || username.contains("'") || userTenancyOCID.contains("'") || privateKey.contains("'") || fingerprint.contains("'"))
      throw new IllegalArgumentException("Please don't use ' in fields");*/
  }

  /**
   * Give us a format suitable to be injected in our pl/sql calls
   *
   * @return string of attributes
   */
  @Override
  public String toAttributeMap() {
    return String.format(
        "credential_name => '%s', \n" +
            "user_ocid => '%s', \n" +
            "tenancy_ocid => '%s', \n" +
            "private_key => '%s', \n" +
            "fingerprint => '%s'",
            name,
            userName,
        userTenancyOCID,
        privateKey,
        fingerprint
    );
  }

  @Override
  public List<String> toUpdatingAttributeList() {
    List<String> output = new ArrayList<>();

    if (!Objects.equals(userName, "")) output.add(toAttributeFormat("user_ocid", userName));
    if (!Objects.equals(userTenancyOCID, "")) output.add(toAttributeFormat("user_tenancy_ocid", userTenancyOCID));
    if (!Objects.equals(privateKey, "")) output.add(toAttributeFormat("private_key", privateKey));
    if (!Objects.equals(fingerprint, "")) output.add(toAttributeFormat("fingerprint", fingerprint));
    return output;
  }

  public Map<String, String> getAttributes() {
    return Map.of(
            "user_ocid", userName,
            "user_tenancy_ocid", userTenancyOCID,
            "private_key", privateKey,
            "fingerprint", fingerprint);
  }
}

