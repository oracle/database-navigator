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

package com.dbn.editor.session;

import com.dbn.common.icon.Icons;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum SessionBrowserFilterType {
    USER(txt("app.sessionBrowser.const.SessionBrowserFilterType_USER"), Icons.SB_FILTER_USER),
    HOST(txt("app.sessionBrowser.const.SessionBrowserFilterType_HOST"), Icons.SB_FILTER_SERVER),
    STATUS(txt("app.sessionBrowser.const.SessionBrowserFilterType_STATUS"), Icons.SB_FILTER_STATUS);

    private final @Nls String name;
    private final Icon icon;

    SessionBrowserFilterType(@Nls String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

}
