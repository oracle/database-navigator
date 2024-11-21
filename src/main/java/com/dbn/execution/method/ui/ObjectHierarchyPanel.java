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

package com.dbn.execution.method.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Fonts;
import com.dbn.object.common.DBObject;
import com.intellij.ui.JBColor;
import com.intellij.ui.RowIcon;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class ObjectHierarchyPanel extends JPanel {
    private DBObject object;

    public ObjectHierarchyPanel(DBObject object) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Color color = new Color(255, 255, 239);
        setBackground(color);
        Border border = new CompoundBorder(
                new LineBorder(JBColor.GRAY, 1, false),
                new LineBorder(color, 4, false));
        setBorder(border);
        this.object = object;

        List<DBObject> chain = new ArrayList<>();
        while (object != null) {
            chain.add(0, object);
            object = object.getParentObject();
        }
        for (int i=0; i<chain.size(); i++) {
            object = chain.get(i);
            RowIcon icon = new RowIcon(i+1);
            icon.setIcon(object.getIcon(), i);
            if (i > 0) icon.setIcon(Icons.TREE_BRANCH, i-1);
            if (i > 1) {
                for (int j=0; j<i-1; j++) {
                    icon.setIcon(Icons.SPACE, j);
                }
            }
            JLabel label = new JLabel(object.getName(), icon, SwingConstants.LEFT);
            
            if (object == this.object) {
                Font font = Fonts.deriveFont(label.getFont(), Font.BOLD);
                label.setFont(font);
            } else {

            }
            add(label);
        }
    }
}
