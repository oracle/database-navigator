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

package com.dbn.oci.config;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Workaround;
import com.dbn.diagnostics.Diagnostics;
import com.oracle.bmc.ConfigFileReader;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Strings.isEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class OciConfigFileUtil {

    @Workaround
    public static List<String> getConfigProfileNames(String configFilePath) {
        if (isEmpty(configFilePath)) return emptyList();

        var config = readConfiguration(configFilePath);
        return new ArrayList<>(config.keySet());
    }

    @NonNls
    public static Map<String, String> getConfigProfileValues(String configFilePath, String profileName) {
        if (isEmpty(configFilePath)) return emptyMap();
        if (isEmpty(profileName)) return emptyMap();

        var config = readConfiguration(configFilePath);
        var profileConfig = config.get(profileName);
        return profileConfig == null ? emptyMap() : profileConfig;
    }

    private static Map<String, Map<String, String>> readConfiguration(String configFilePath) {
        try {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configFilePath);

            //configFile.accumulator.configurationsByProfile
            Object accumulator = Reflection.getFieldValue(configFile, "accumulator");
            return Reflection.getFieldValue(accumulator, "configurationsByProfile");
        } catch (Throwable t) {
            // TODO propagate exception to consumer
            Diagnostics.conditionallyLog(t);
            return emptyMap();
        }
    }


/*
    @Experimental
    public static List<String> getRegionNames() {
        AbstractAuthenticationDetailsProvider authProvider = createAuthProvider();
        try (IdentityClient client = IdentityClient.builder().build(authProvider)) {
            ListRegionsRequest request = ListRegionsRequest.builder().build();
            ListRegionsResponse response = client.listRegions(request);
            return response
                    .getItems()
                    .stream()
                    .map(r -> r.getName())
                    .collect(toList());
        }
    }

    @Experimental
    public static List<String> getModelNames(String regionName) {
        System.setProperty("javax.net.ssl.trustAllCertificates", "true");
        AbstractAuthenticationDetailsProvider authProvider = createAuthProvider();
        JerseyHttpProvider httpProvider = JerseyHttpProvider.getInstance();
        try (AnomalyDetectionClient client = AnomalyDetectionClient
                .builder()
                .httpProvider(httpProvider)
                .build(authProvider)) {
            Region region = Region.fromRegionCodeOrId(regionName);
            client.setRegion(region);
            ListModelsRequest request = ListModelsRequest.builder().compartmentId("ocid1.compartment.oc1..aaaaaaaayiilxjceby5tqp3b42qv2hmvckk2uuqrikhsbfuoepkgxpdfogpq").build();
            ListModelsResponse response = client.listModels(request);
            return response
                    .getModelCollection()
                    .getItems()
                    .stream()
                    .map(m -> m.getId())
                    .collect(toList());
        }
    }

    @SneakyThrows
    private static @NonNull AbstractAuthenticationDetailsProvider createAuthProvider() {
        return new ConfigFileAuthenticationDetailsProvider("/Users/dcioca/.oci/config", "OCI_GEN_AI");
    }*/
}
