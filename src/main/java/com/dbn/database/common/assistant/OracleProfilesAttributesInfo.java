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

import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.entity.ProfileDBObjectItem;
import com.dbn.assistant.provider.ProviderModel;
import com.dbn.assistant.provider.ProviderType;
import com.dbn.database.common.statement.CallableStatementOutput;
import com.google.gson.JsonParseException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Class to fetch profile attributes from profile tables and views
 * @see OracleAssistantInterface.listProfiles()
 */
@Slf4j
@Getter
public class OracleProfilesAttributesInfo implements CallableStatementOutput {

  private List<Profile> profileList = new ArrayList<>();

  public OracleProfilesAttributesInfo() {
  }

  @Override
  public void registerParameters(CallableStatement statement) throws SQLException {
  }

  @Override
  public void read(CallableStatement statement) throws SQLException {
      ResultSet rs = statement.executeQuery();
      profileList = buildProfilesFromResultSet(rs);
  }

  /**
   * Reads a resultSet and build a list of profile
   * Since the result set has each attribute in a separate row, it was read accordingly
   * @param rs resultSet that contains all profile attributes
   * @return list of profiles
   * @throws SQLException if parsing of data has failed
   */
  private List<Profile> buildProfilesFromResultSet(ResultSet rs) throws SQLException {
    Map<String, Profile> profileBuildersMap = new HashMap<>();
    List <String> faultyProfilesNames = new ArrayList<>();
    while (rs.next()) {
      String profileName=null;
      try {
        profileName = rs.getString("PROFILE_NAME");
        String status = rs.getString("STATUS");
        String description = rs.getString("DESCRIPTION");
        String attributeName = rs.getString("ATTRIBUTE_NAME");
        Clob attributeValue = rs.getClob("ATTRIBUTE_VALUE");
        String finalProfileName = profileName;
        Profile currProfile = profileBuildersMap.computeIfAbsent(profileName, k -> Profile.builder().profileName(finalProfileName).build());
        currProfile.setEnabled(status.equals("ENABLED"));
        currProfile.setDescription(description);

        Object attributeObject = currProfile.clobToObject(attributeName, attributeValue);

        if ("object_list".equals(attributeName) && attributeObject instanceof List) {
          @SuppressWarnings("unchecked")
          List<ProfileDBObjectItem> objectList = (List<ProfileDBObjectItem>) attributeObject;
          currProfile.setObjectList(objectList);
        } else if (attributeObject instanceof String) {
          String attribute = (String) attributeObject;
          if (Objects.equals(attributeName, "temperature"))
            currProfile.setTemperature(Float.parseFloat(attribute));
          else if (Objects.equals(attributeName, "provider"))
            currProfile.setProvider(ProviderType.forId(attribute.toUpperCase()));
          else if (Objects.equals(attributeName, "credential_name")) currProfile.setCredentialName(attribute);
          else if (Objects.equals(attributeName, "model")) {
            try {
              currProfile.setModel(ProviderModel.forApiName(attribute));
            } catch (IllegalArgumentException e) {
              log.error("malformed model name  [" + attribute + "], dropping profile");
              faultyProfilesNames.add(profileName);
            }
          }
        }
      } catch (IOException e) {
        // This one is a real one.
        throw new SQLException(e);
      } catch (JsonParseException ee) {
        // this is surely because of DB tables sending us bad values.
        log.error("malformed profile [" + profileName + "], dropping it");
        faultyProfilesNames.add(profileName);
      }
    }
    // now remove all faulty ones
    profileBuildersMap.keySet().removeAll(faultyProfilesNames);
    return new ArrayList<>(profileBuildersMap.values());

  }
}
