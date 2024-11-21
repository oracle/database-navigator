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

import com.dbn.common.filter.Filter;
import com.dbn.common.list.FilteredList;
import com.dbn.common.util.Strings;
import lombok.Getter;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.filter;

@Getter
public class TnsNames {
    private final File file;
    private final List<TnsProfile> profiles;
    private final NamesFilter filter = new NamesFilter();

    public TnsNames() {
        this(null, Collections.emptyList());
    }

    public TnsNames(File file, List<TnsProfile> profiles) {
        this.file = file;
        Collections.sort(profiles);
        this.profiles = FilteredList.stateful(filter, profiles);
    }

    public List<String> getProfileNames() {
        return convert(FilteredList.unwrap(profiles), p -> p.getProfile());
    }

    public List<TnsProfile> getSelectedProfiles() {
        return filter(profiles, p -> p.isSelected());
    }

    public String getTnsFolder() {
        return file == null ? "" : file.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
    }

    public int size() {
        return profiles.size();
    }

    @Getter
    public static class NamesFilter implements Filter<TnsProfile> {
        private String text = "";

        @Override
        public boolean accepts(TnsProfile tnsProfile) {
            if (Strings.isEmptyOrSpaces(text)) return true;
            return matches(tnsProfile.getProfile()) ||
                    matches(tnsProfile.getProtocol()) ||
                    matches(tnsProfile.getHost()) ||
                    matches(tnsProfile.getPort()) ||
                    matches(tnsProfile.getSid()) ||
                    matches(tnsProfile.getServiceName()) ||
                    matches(tnsProfile.getGlobalName());
        }

        private boolean matches(String attribute) {
            return attribute != null && Strings.indexOfIgnoreCase(attribute, text, 0) > -1;
        }

        @Override
        public int getSignature() {
            return Objects.hashCode(text);
        }

        public boolean setText(String text) {
            if (!Objects.equals(this.text, text)) {
                this.text = text;
                return true;
            }
            return false;
        }
    }
}
