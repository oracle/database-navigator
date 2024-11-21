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

package com.dbn.data.editor.ui;

import com.dbn.common.dispose.StatefulDisposable;

import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.Font;

public interface DataEditorComponent extends StatefulDisposable {

    JTextField getTextField();

    void setEditable(boolean editable);

    boolean isEditable();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    <T> UserValueHolder<T> getUserValueHolder();

    <T> void setUserValueHolder(UserValueHolder<T> userValueHolder);

    String getText();

    void setText(String text);

    void setFont(Font font);

    void setBorder(Border border);

    default void afterUpdate() {}
}
