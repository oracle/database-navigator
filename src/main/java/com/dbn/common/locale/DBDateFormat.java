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

package com.dbn.common.locale;

import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.DateFormat;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum DBDateFormat implements Presentable {
    FULL(txt("cfg.shared.const.DateFormat_FULL"), DateFormat.FULL),
    SHORT(txt("cfg.shared.const.DateFormat_SHORT"), DateFormat.SHORT),
    MEDIUM(txt("cfg.shared.const.DateFormat_MEDIUM"), DateFormat.MEDIUM),
    LONG(txt("cfg.shared.const.DateFormat_LONG"), DateFormat.LONG),
    CUSTOM(txt("cfg.shared.const.DateFormat_CUSTOM"), 0);

    private final String name;
    private final int format;
}
