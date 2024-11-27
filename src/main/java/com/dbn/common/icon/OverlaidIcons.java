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

package com.dbn.common.icon;

import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBRectangle;
import lombok.experimental.UtilityClass;

import javax.swing.Icon;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class OverlaidIcons {

    private static final Map<Icon, Icon> modifiedOverlayIcons = new ConcurrentHashMap<>();

    public static Icon addModifiedOverlay(Icon icon) {
        return modifiedOverlayIcons.computeIfAbsent(icon, i -> {
            JBRectangle area = new JBRectangle(3, 3, 7, 7);
            Icon watermark = IconUtil.cropIcon(AllIcons.General.Modified, area);
            LayeredIcon layeredIcon = new LayeredIcon(2);

            layeredIcon.setIcon(i, 0);
            layeredIcon.setIcon(watermark, 1, -(watermark.getIconWidth() / 2) - 3, 0);

            return JBUIScale.scaleIcon(layeredIcon);
        });
    }
}
