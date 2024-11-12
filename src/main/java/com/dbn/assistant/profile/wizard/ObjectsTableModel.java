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

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * profile object table model.
 * A model that manipulate list of <code>ProfileDBObjectItem</code>
 */
@Slf4j
@Getter
public class ObjectsTableModel extends AbstractTableModel {


    private final List<DBObjectRef<DBObject>> data;

    public static final int NAME_COLUMN_IDX = 0;
    public static final int OWNER_COLUMN_IDX = 1;

    private static final String[] columnNames = {
            txt("profile.mgmt.obj_table.header.name"),
            txt("profile.mgmt.obj_table.header.owner")
    };

    /**
     * Creates a new (empty) model
     */
    public ObjectsTableModel() {
        this.data = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
        //return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DBObjectRef ref = data.get(rowIndex);
        return ref.get();
    }

    @Override
    public String getColumnName(int column) {
        return "Dataset";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return DBObject.class;
    }

    /**
     * Adds a list of profile db object to the model
     * @param items the list of objects to be added
     */
    public void addItems(List<DBObject> items) {
        int curRow = data.size();
        data.addAll(DBObjectRef.from(items));
        fireTableRowsInserted(curRow, curRow + items.size());
    }

    public void removeItem(int index) {
        if (index >= 0) {
            data.remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    public void removeItem(DBObjectRef<DBObject> item) {
        data.remove(item);
    }

    /**
     * Replaces list of object in the model
     * @param items the new item list to be added to the model.
     */
    public void updateItems(List<DBObjectRef<DBObject>> items) {
        data.clear();
        data.addAll(items);
        fireTableDataChanged();
    }
}
