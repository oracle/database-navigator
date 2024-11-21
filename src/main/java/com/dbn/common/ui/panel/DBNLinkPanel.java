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

package com.dbn.common.ui.panel;

import com.dbn.common.ui.util.Cursors;
import com.intellij.ui.JBColor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

public class DBNLinkPanel extends JPanel{
    private final JLabel label;
    public DBNLinkPanel(String text) {
        label = new JLabel(text);
        label.setForeground(JBColor.BLUE);
        setCursor(Cursors.handCursor());
        label.setCursor(Cursors.handCursor());
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }
    
    public void setLabel(String text) {
        label.setText(text);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
    }
}
