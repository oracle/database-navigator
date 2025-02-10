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

package com.dbn.common.ui.util;

import javax.swing.JPanel;
import java.awt.Component;

public interface Borderless {

    static void markBorderless(Component component) {
        ClientProperty.NO_BORDER.set(component, Boolean.TRUE);
    }

    static boolean isBorderless(Component component) {
        Boolean borderless = ClientProperty.NO_BORDER.get(component);
        if (borderless != null) return borderless;
        if (component instanceof JPanel) return true;
        if (component instanceof Borderless) return true;

        return false;
    }
}
