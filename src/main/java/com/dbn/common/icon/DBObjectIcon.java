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

package com.dbn.common.icon;

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

public final class DBObjectIcon implements Icon {
    private final DBObjectRef<?> object;

    public DBObjectIcon(DBObjectRef<?> object) {
        this.object = object;
    }

    Icon getIcon() {
        Icon defaultIcon = this.object.getObjectType().getIcon();
        DBObject object = this.object.get();
        if (object == null) return defaultIcon;

        Icon icon = object.getIcon();
        return icon == null ? defaultIcon : icon;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int i, int i1) {
        getIcon().paintIcon(component, graphics, i, i1);
    }

    @Override
    public int getIconWidth() {
        return getIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return getIcon().getIconHeight();
    }
}
