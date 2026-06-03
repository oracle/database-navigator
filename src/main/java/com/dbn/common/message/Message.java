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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.state.PersistentStateElement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Message extends StatefulDisposableBase implements PersistentStateElement {
    protected MessageType type;
    protected @Nls String text;

    public Message(MessageType type, @Nls String text) {
        this.type = type;
        this.text = text;
    }

    public boolean isError() {
        return type == MessageType.ERROR;
    }

    public boolean isInfo() {
        return type == MessageType.INFO;
    }

    public Icon getDialogIcon() {
        return type.getDialogIcon();
    }

    @Override
    public void disposeInner() {
        nullify();
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        type = enumAttribute(element, "type", MessageType.class);

        Element textElement = element.getChild("text");
        text = readCdata(textElement);
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        setEnumAttribute(element, "type", type);

        Element textElement = newElement(element, "text");
        writeCdata(textElement, text);
    }
}
