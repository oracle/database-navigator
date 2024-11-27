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

package com.dbn.object.common.ui;

import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.DBObject;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.util.ArrayList;

public class ObjectDetailsForm {
    private JLabel connectionLabel;
    private JPanel objectPanel;
    private JPanel mainPanel;
    private DBObject object;

    public ObjectDetailsForm(DBObject object) {
        this.object = object;
        objectPanel.setLayout(new BoxLayout(objectPanel, BoxLayout.X_AXIS));
        ConnectionHandler connection = object.getConnection();
        connectionLabel.setText(connection.getName());
        connectionLabel.setIcon(connection.getIcon());
        

        java.util.List<DBObject> chain = new ArrayList<>();
        while (object != null) {
            chain.add(0, object);
            object = object.getParentObject();
        }

        for (int i=0; i<chain.size(); i++) {
            object = chain.get(i);
            if ( i > 0) objectPanel.add(new JLabel(" > "));

            JLabel objectLabel = new JLabel(object.getName(), object.getIcon(), SwingConstants.LEFT);
            if (object == this.object) {
                Font font = Fonts.deriveFont(objectLabel.getFont(), Font.BOLD);
                objectLabel.setFont(font);
            }
            objectPanel.add(objectLabel);
        }

    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
