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

package com.dbn.common.message;

import com.dbn.common.util.Titled;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Getter
@NoArgsConstructor
public class TitledMessage extends Message implements Titled {
    private String title;

    public TitledMessage(
            MessageType type,
            @DialogTitle String title,
            @DialogMessage String text) {
        super(type, text);
        this.title = title;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);

        Element titleElement = element.getChild("title");
        title = readCdata(titleElement);
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;
        super.writeState(element);

        Element titleElement = newElement(element, "title");
        writeCdata(titleElement, title);
    }
}
