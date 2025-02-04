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

package com.dbn.common.ui;

import lombok.experimental.UtilityClass;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

@UtilityClass
public class Layouts {

    /**
     * Sets the layout of the specified JPanel to be vertically oriented
     * by using the BoxLayout with the Y_AXIS alignment.
     *
     * @param panel the JPanel whose layout is to be set to vertical.
     *              Must not be null.
     */
    public static void verticalBoxLayout(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    }

    /**
     * Sets the layout of the specified JPanel to be horizontally oriented
     * using the BoxLayout with the X_AXIS alignment.
     *
     * @param panel the JPanel whose layout is to be set to horizontal.
     *              Must not be null.
     */
    public static void horizontalBoxLayout(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    }
}
