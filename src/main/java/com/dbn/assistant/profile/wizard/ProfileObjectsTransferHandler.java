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

import com.dbn.object.DBDataset;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.designer.clipboard.SimpleTransferable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Transfer handler for Database object and profile database object.
 * This handler is used durin drag and drop between tables
 */

@Slf4j
public class ProfileObjectsTransferHandler extends TransferHandler {

    @Override
    public boolean canImport(TransferSupport info) {
        if (!info.isDrop()) return false;
        if (!isSelectAction(info) && !isDeselectAction(info)) return false;

        info.setShowDropLocation(true);
        return true;
    }


    @Override
    public boolean importData(TransferSupport info) {
        if (!canImport(info)) return false;

        if (info.isDataFlavorSupported(ProfileObjectsTransferable.REMOVE_FLAVOR)) return true;
        if (info.isDataFlavorSupported(ProfileObjectsTransferable.ADD_FLAVOR)) return selectData(info);

        return false;
    }

    private static boolean selectData(TransferSupport info) {
        try {
            Transferable transferable = info.getTransferable();
            List<DBObject> objects = cast(transferable.getTransferData(ProfileObjectsTransferable.ADD_FLAVOR));

            JTable table = (JTable) info.getComponent();
            ObjectsTableModel model = (ObjectsTableModel) table.getModel();
            model.addItems(objects);
            return true;
        } catch (Exception e) {
            log.warn("Failed to transfer data", e);
            return false;

        }
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        if (action != TransferHandler.MOVE) return;
        JTable table = (JTable) source;
        TableModel model = table.getModel();
        if (model instanceof ObjectsTableModel) {
            ObjectsTableModel objectModel = (ObjectsTableModel) model;
            List<DBObjectRef<DBObject>> objects = getSelectedItems(table);
            objects.forEach(o -> objectModel.removeItem(o));
        }

    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }


    @Nullable
    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable) c;
        TableModel model = table.getModel();

        if (model instanceof AvailableDatasetsTableModel) {
            List<DBDataset> datasets = getSelectedItems(table);
            return new ProfileObjectsTransferable(datasets);
        } else if (model instanceof ObjectsTableModel) {
            return new SimpleTransferable("NULL", ProfileObjectsTransferable.REMOVE_FLAVOR);
        }

        return null;
    }


    private static boolean isSelectAction(TransferSupport info) {
        return info.isDataFlavorSupported(ProfileObjectsTransferable.ADD_FLAVOR) &&
                getTableModel(info) instanceof ObjectsTableModel;
    }

    private static boolean isDeselectAction(TransferSupport info) {
        return info.isDataFlavorSupported(ProfileObjectsTransferable.REMOVE_FLAVOR) &&
                getTableModel(info) instanceof AvailableDatasetsTableModel;
    }

    private static TableModel getTableModel(TransferSupport info) {
        Component component = info.getComponent();
        if (!(component instanceof JTable)) return null;
        JTable table = (JTable) component;
        return table.getModel();
    }

    private static <T> List<T> getSelectedItems(JTable table) {
        int[] rows = table.getSelectedRows();
        List<T> items = new ArrayList<>();
        for (int row : rows) {
            TableModel model = table.getModel();
            T item = (T) model.getValueAt(row, 0);
            items.add(item);
        }
        return items;
    }

}
