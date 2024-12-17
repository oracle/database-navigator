/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection.config.tns;

import com.dbn.common.util.FileContentCache;
import com.dbn.common.util.Strings;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Commons.coalesce;

public class TnsNamesParser {
    public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(true, false, false, false, false, false).
            withTitle("Select TNS Names File").
            withDescription("Select a valid Oracle tnsnames.ora file").
            withFileFilter(virtualFile -> Objects.equals(virtualFile.getExtension(), "ora"));

    private static final FileContentCache<TnsNames> cache = new FileContentCache<>() {
        @Override
        protected TnsNames load(File file) {
            return parse(file);
        }
    };

    public static TnsNames get(File file) throws Exception {
        return cache.get(file);
    }


    @SneakyThrows
    public static TnsNames parse(File file) {
        List<TnsProfile> tnsProfiles = new ArrayList<>();
        Path filePath = Paths.get(file.getPath());
        String tnsContent = Files.readString(filePath);

        Pattern pattern = TnsProfilePattern.INSTANCE.get();
        Matcher matcher = pattern.matcher(tnsContent);

        int start = 0;
        while (matcher.find(start)) {
            String descriptor = matcher.group("descriptor");
            String schema = matcher.group("schema");
            String protocol = coalesce(
                    () -> matcher.group("protocol1"),
                    () -> matcher.group("protocol2"),
                    () -> matcher.group("protocol3"));

            String host = coalesce(
                    () -> matcher.group("host1"),
                    () -> matcher.group("host2"),
                    () -> matcher.group("host3"));

            String port = coalesce(
                    () -> matcher.group("port1"),
                    () -> matcher.group("port2"),
                    () -> matcher.group("port3"));

            String server         = matcher.group("server");
            String sid            = matcher.group("sid");
            String serviceName    = matcher.group("servicename");
            String globalName     = matcher.group("globalname");
            String failover       = matcher.group("failover");
            String failoverType   = matcher.group("failovertype");
            String failoverMethod = matcher.group("failovermethod");
            start = matcher.end();

            if (Strings.isNotEmpty(schema)) {
                TnsProfile tnsProfile = new TnsProfile(
                        descriptor,
                        schema,
                        protocol,
                        host,
                        port,
                        server,
                        sid,
                        serviceName,
                        globalName,
                        failover,
                        failoverType,
                        failoverMethod);
                tnsProfiles.add(tnsProfile);
            }
        }
        return new TnsNames(file, tnsProfiles);
    }
}
