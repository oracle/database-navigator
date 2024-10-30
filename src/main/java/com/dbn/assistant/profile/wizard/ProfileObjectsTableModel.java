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

import com.dbn.assistant.entity.ProfileDBObjectItem;
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
public class ProfileObjectsTableModel extends AbstractTableModel {


    private List<ProfileDBObjectItem> data;

    public static final int NAME_COLUMN_IDX = 0;
    public static final int OWNER_COLUMN_IDX = 1;

    private static final String[] columnNames = {
            txt("profile.mgmt.obj_table.header.name"),
            txt("profile.mgmt.obj_table.header.owner")
    };

    /**
     * Creates a new (empty) model
     */
    public ProfileObjectsTableModel() {
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
        return data.get(rowIndex);

/*
        ProfileDBObjectItem item = data.get(rowIndex);
        switch (columnIndex) {
            case NAME_COLUMN_IDX:
                return item.getName();
            case OWNER_COLUMN_IDX:
                return item.getOwner();
            default:
                return null;
        }
*/
    }

    @Override
    public String getColumnName(int column) {
        return "Dataset";
        //return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return ProfileDBObjectItem.class;
/*
        if (columnIndex == 1) {
            return String.class;
        }
        return String.class;
*/
    }


    /**
     * Adds a list of profile db object to the model
     * @param items the list of objects to be added
     */
    public void addItems(List<ProfileDBObjectItem> items) {
        if (log.isDebugEnabled())
            log.debug("ProfileObjectListTableModel.addItems: " + items);
        int curRow = data.size();
        data.addAll(items);
        log.debug(
                "ProfileObjectListTableModel.addItems triggered  fireTableRowsInserted on (" +
                        curRow + "/" + curRow + items.size() + ")");
        fireTableRowsInserted(curRow, curRow + items.size());
    }

    /**
     * Removed a  profile db object from the model
     * @param item the  object to be removed
     */
    public void removeItem(ProfileDBObjectItem item) {
        log.debug("ProfileObjectListTableModel.removeItem: " + item);
        int index = data.indexOf(item);
        removeItem(index);
    }

    public void removeItem(int index) {
        if (index >= 0) {
            data.remove(index);
            log.debug(
                    "ProfileObjectListTableModel.removeItem triggered  fireTableRowsDeleted on (" +
                            index + "/" + index + ")");
            fireTableRowsDeleted(index, index);
        }
    }

    /**
     * Replaces list of object in the model
     * @param items the new item list to be added to the model.
     */
    public void updateItems(List<ProfileDBObjectItem> items) {
        data.clear();
        data.addAll(items);
        if (log.isDebugEnabled())
            log.debug("ProfileObjectListTableModel.updateItems: " + items);
        fireTableDataChanged();
    }

    // Method to get data

    /**
     * Gets the list of items of that model
     * @return the list of profile DB object
     */
    public List<ProfileDBObjectItem> getData() {
        return data;
    }


    /**
     * Gets an item from the model at a given index
     * @param rowIndex the index within the model
     * @return the item at given index
     * @throws IndexOutOfBoundsException if index is not valid
     */
    public ProfileDBObjectItem getItemAt(int rowIndex) throws IndexOutOfBoundsException{
        return data.get(rowIndex);
    }
}
