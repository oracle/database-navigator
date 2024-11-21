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

package com.dbn.common.locale.options.ui;

import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Strings;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Getter
public class LocaleOption implements Presentable{
    public static final List<LocaleOption> ALL = new ArrayList<>();
    static {
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            if (Strings.isNotEmptyOrSpaces(locale.getDisplayName()))
                ALL.add(new LocaleOption(locale));
        }
        ALL.sort(Comparator.comparing(LocaleOption::getName));
    }


    private final Locale locale;
    private final String name;

    public LocaleOption(Locale locale) {
        this.locale = locale;
        this.name = getName(locale).intern();
    }

    private static String getName(Locale locale) {
        return locale.equals(Locale.getDefault()) ?
                locale.getDisplayName() + " - System default" :
                locale.getDisplayName();
    }

    @Nullable
    public static LocaleOption get(Locale locale) {
        for (LocaleOption localeOption : ALL) {
            if (localeOption.locale.equals(locale)) {
                return localeOption;
            }
        }
        return null;
    }
}
