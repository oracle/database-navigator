/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.profile.wizard;

import com.dbn.common.list.FilteredList;
import com.dbn.common.ui.util.Listeners;
import com.dbn.object.DBDataset;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AvailableDatasetsTableModel implements TableModel {

    private List<DBDataset> datasets = new ArrayList<>();
    private final AvailableDatasetsFilter filter = new AvailableDatasetsFilter();
    private final Listeners<TableModelListener> listeners = Listeners.create();

    public void setDatasets(List<DBDataset> datasets) {
        this.datasets = FilteredList.stateful(filter, datasets);
    }

    @Override
    public int getRowCount() {
        return datasets.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public @Nls String getColumnName(int columnIndex) {
        return "Dataset";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return DBDataset.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return datasets.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void notifyDataChanges() {
        listeners.notify(l -> {
            l.tableChanged(new TableModelEvent(this));
        });
    }
}
