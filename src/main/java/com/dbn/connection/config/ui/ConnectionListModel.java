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

package com.dbn.connection.config.ui;

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.data.sorting.SortDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import javax.swing.DefaultListModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ConnectionListModel extends DefaultListModel<ConnectionSettings> implements StatefulDisposable {
    private static final Comparator<ConnectionSettings> ASC_COMPARATOR = Comparator.comparing(s -> s.getDatabaseSettings().getName());
    private static final Comparator<ConnectionSettings> DESC_COMPARATOR = (s1, s2) -> -s1.getDatabaseSettings().getName().compareTo(s2.getDatabaseSettings().getName());

    private boolean disposed;

    public ConnectionListModel(ConnectionBundleSettings connectionBundleSettings) {
        List<ConnectionSettings> connections = connectionBundleSettings.getConnections();
        for (ConnectionSettings connection : connections) {
            addElement(connection);
        }
    }

    public ConnectionSettings getConnectionConfig(@NonNls String name) {
        for (int i=0; i< getSize(); i++){
            ConnectionSettings connectionSettings = getElementAt(i);
            if (Objects.equals(connectionSettings.getDatabaseSettings().getName(), name)) {
                return connectionSettings;
            }
        }
        return null;
    }

    public void sort(SortDirection sortDirection) {
        List<ConnectionSettings> list = new ArrayList<>();
        for (int i=0; i< getSize(); i++){
            ConnectionSettings connectionSettings = getElementAt(i);
            list.add(connectionSettings);
        }

        switch (sortDirection) {
            case ASCENDING: list.sort(ASC_COMPARATOR); break;
            case DESCENDING: list.sort(DESC_COMPARATOR); break;
        }

        clear();
        for (ConnectionSettings connectionSettings : list) {
            addElement(connectionSettings);
        }
    }

    @Override
    public void disposeInner() {
        removeAllElements();
    }
}
