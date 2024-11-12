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

package com.dbn.assistant.provider;

import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * This enum is for listing the possible credential providers we have
 * And the associated list of AI module they support
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Getter
@Setter
public final class AIProvider implements Presentable {

  private final String id;
  private final String name;
  private final String host;
  private final boolean main;
  private final boolean experimental;

  private List<AIModel> models;
  private Map<ProviderUrlType, String> urls;

  AIProvider(String id, String name, String host, boolean main, boolean experimental) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.main = main;
    this.experimental = experimental;
  }

  public static List<AIProvider> values() {
    return LanguageModelDefinition.providers();
  }

  public static AIProvider forId(String id) {
      return Lists.first(values(),  p -> p.getId().equals(id));
  }

  public AIModel getModel(String id) {
    return Lists.first(models, p -> p.getId().equals(id));
  }

  public AIModel getDefaultModel() {
    return Lists.firstElement(models);
  }

  public String getUrl(ProviderUrlType type) {
    return urls.get(type);
  }

  @Override
  public String toString() {
    return getName();
  }
}
