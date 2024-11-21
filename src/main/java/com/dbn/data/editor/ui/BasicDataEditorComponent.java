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

import lombok.Getter;
import lombok.Setter;

import javax.swing.JTextField;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class BasicDataEditorComponent extends JTextField implements DataEditorComponent{
    private UserValueHolder userValueHolder;

    @Override
    public JTextField getTextField() {
        return this;
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        setEditable(enabled);
    }

    public <T> UserValueHolder<T> getUserValueHolder() {
        return cast(userValueHolder);
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    private boolean disposed;

    @Override
    public void disposeInner() {
        userValueHolder = null;
    }
}
