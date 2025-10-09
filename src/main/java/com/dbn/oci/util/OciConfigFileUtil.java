/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.oci.util;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.util.Strings;
import com.dbn.diagnostics.Diagnostics;
import com.oracle.bmc.ConfigFileReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OciConfigFileUtil {

    @Workaround
    public static List<String> getConfigProfileNames(String configFilePath) {
        if (Strings.isEmpty(configFilePath)) return Collections.emptyList();

        try {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configFilePath);

            //configFile.accumulator.configurationsByProfile
            Object accumulator = Reflection.getFieldValue(configFile, "accumulator");
            Map<String, ?> configurations = Reflection.getFieldValue(accumulator, "configurationsByProfile");
            return new ArrayList<>(configurations.keySet());
        } catch (Exception e) {
            Diagnostics.conditionallyLog(e);
            return Collections.emptyList();
        }

    }
}
