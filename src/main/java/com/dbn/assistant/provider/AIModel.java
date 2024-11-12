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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.util.Lists.first;

/**
 * AI models
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Getter
public final class AIModel implements Presentable {
    private final AIProvider provider;
    private final String id;

    //How this is named in profile API
    private final String apiName;

    AIModel(AIProvider provider, String id, String apiName) {
        this.provider = provider;
        this.id = id;
        this.apiName = apiName;
    }

    @Override
    public @NotNull String getName() {
        return id; // TODO presentable profile names
    }

    @Nullable
    public static AIModel forId(String id) {
        List<AIProvider> providers = AIProvider.values();
        for (AIProvider provider : providers) {
            AIModel model = provider.getModel(id);
            if (model != null) return model;
        }

        return null;
    }

    @Nullable
    public static AIModel forApiName(String apiName) {
        List<AIProvider> providers = AIProvider.values();
        for (AIProvider provider : providers) {
            List<AIModel> models = provider.getModels();
            AIModel model = first(models, m -> m.getApiName().equals(apiName));
            if (model != null) return model;
        }

        return null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
