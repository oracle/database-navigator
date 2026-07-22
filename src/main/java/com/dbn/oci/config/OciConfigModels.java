/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.oci.config;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.thread.Background;
import com.dbn.diagnostics.Diagnostics;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;

public class OciConfigModels implements PersistentStateElement {
    private final Map<String, List<String>> namesByRegion = new ConcurrentHashMap<>();
    private final Set<String> loadingRegions = ConcurrentHashMap.newKeySet();
    private final Set<String> staleRegions = ConcurrentHashMap.newKeySet();

    @Nullable
    public List<String> getModelNames(OciConfig config) {
        String regionId = config.getRegionId();
        if (isEmpty(regionId)) return null;

        List<String> modelNames = namesByRegion.get(regionId);
        if (modelNames == null) {
            reloadModelNames(config.clone());
        } else if (staleRegions.contains(regionId)) {
            reloadModelNames(config.clone());
        }
        return modelNames;
    }

    private void reloadModelNames(OciConfig config) {
        String regionId = config.getRegionId();
        if (!loadingRegions.add(regionId)) return;

        Background.run(() -> {
            try {
                namesByRegion.put(regionId, OciConfigUtil.getModelNames(config));
                staleRegions.remove(regionId);
            } catch (Exception e) {
                Diagnostics.conditionallyLog(e);
            } finally {
                loadingRegions.remove(regionId);
            }
        });
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        namesByRegion.clear();
        staleRegions.clear();
        for (Element regionElement : childrenOf(element, "region")) {
            String regionId = stringAttribute(regionElement, "id");
            if (isEmpty(regionId)) continue;

            List<String> modelNames = new ArrayList<>();
            for (Element modelElement : childrenOf(regionElement, "model")) {
                String modelName = stringAttribute(modelElement, "name");
                if (isNotEmpty(modelName)) {
                    modelNames.add(modelName);
                }
            }
            namesByRegion.put(regionId, modelNames);
            staleRegions.add(regionId);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        for (Map.Entry<String, List<String>> entry : namesByRegion.entrySet()) {
            Element regionElement = newElement(element, "region");
            setStringAttribute(regionElement, "id", entry.getKey());
            for (String modelName : entry.getValue()) {
                Element modelElement = newElement(regionElement, "model");
                setStringAttribute(modelElement, "name", modelName);
            }
        }
    }
}
