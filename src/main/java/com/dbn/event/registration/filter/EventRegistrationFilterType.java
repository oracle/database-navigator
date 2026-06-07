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

package com.dbn.event.registration.filter;

import com.dbn.common.filter.FilterOption;
import com.dbn.common.icon.Icons;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum EventRegistrationFilterType {
    USER(txt("app.eventRegistration.const.EventRegistrationFilterType_USER"), Icons.DBO_USER),
    TABLE(txt("app.eventRegistration.const.EventRegistrationFilterType_TABLE"), Icons.DBO_TABLE),
    STATUS(txt("app.eventRegistration.const.EventRegistrationFilterType_STATUS"), null),
    //...
    ;

    public static final FilterOption FILTER_STATUS_LISTENING = new FilterOption("LISTENING", txt("app.eventRegistration.const.FilterStatus_LISTENING"), Icons.COMMON_FILTER_ACTIVE);
    public static final FilterOption FILTER_STATUS_NOT_LISTENING = new FilterOption("NOT_LISTENING", txt("app.eventRegistration.const.FilterStatus_NOT_LISTENING"), Icons.COMMON_FILTER_INACTIVE);

    private final @Nls String name;
    private final Icon icon;

    EventRegistrationFilterType(@Nls String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }
}
