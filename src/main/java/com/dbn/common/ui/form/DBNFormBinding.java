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

package com.dbn.common.ui.form;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.ClientProperty;
import lombok.experimental.UtilityClass;

import javax.swing.JComponent;
import java.awt.Component;

/**
 * Utility class for binding a {@link DBNForm} to a {@link JComponent}
 *
 */
@UtilityClass
public class DBNFormBinding {

    public static void bindForm(Component component, DBNForm form) {
        ClientProperty.FORM.set(component, WeakRef.of(form));
    }

    public static <T extends DBNForm> T getForm(Component component) {
        return WeakRef.unwrap(ClientProperty.FORM.get(component));
    }
}
