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

package com.dbn.object.dependency;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import lombok.Getter;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum ObjectDependencyType implements Presentable{
    INCOMING(txt("app.objects.const.ObjectDependencyType_INCOMING"), Icons.DBO_INCOMING_REF, Icons.DBO_INCOMING_REF_SOFT),
    OUTGOING(txt("app.objects.const.ObjectDependencyType_OUTGOING"), Icons.DBO_OUTGOING_REF, Icons.DBO_OUTGOING_REF_SOFT);

    private final String name;
    private final Icon icon;
    private final Icon softIcon;

    ObjectDependencyType(String name, Icon icon, Icon softIcon) {
        this.name = name;
        this.icon = icon;
        this.softIcon = softIcon;
    }
}
