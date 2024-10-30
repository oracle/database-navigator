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

import com.dbn.assistant.provider.ProviderModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is made for when we want to update a profile and implements a different version of toAttributeMap
 */
public class ProfileUpdate extends Profile {

  /**
   * Copy constructor
   * @param initialProfile
   */
  public ProfileUpdate(Profile initialProfile) {
    super(initialProfile);
  }

  @Override
  public String toAttributeMap() throws IllegalArgumentException {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(ProviderModel.class, new ProviderModelSerializer())
        .create();

    String attributesJson = gson.toJson(this).replace("'", "''");
    return String.format(
        "profile_name => '%s',\n" +
            "attributes => '%s'\n"
        ,
        profileName,
        attributesJson);

  }
}
