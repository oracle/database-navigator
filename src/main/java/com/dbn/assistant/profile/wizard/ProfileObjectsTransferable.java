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
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

/**
 * Transferable object for Database object and profile object
 * This class is used during drag and drop between tables.
 * This class maintain a list of <code>DBObjectItem</code> to be transferred
 */
public class ProfileObjectsTransferable implements Transferable {

    public static final DataFlavor ADD_FLAVOR = new DataFlavor(DBDataset.class, "Datasets");
    public static final DataFlavor REMOVE_FLAVOR = new DataFlavor(Void.class, "Remove datasets");

    private static final DataFlavor[] supportedFlavor = {ADD_FLAVOR};
    private final List<DBDataset> transferred;

    /**
     * Creates a new DatabaseObjectsTransferable with a given list of DB object.
     * @param transferred list of DB object that will be transferred
     */
    public ProfileObjectsTransferable(List<DBDataset> transferred) {
        this.transferred = transferred;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return supportedFlavor;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return supportedFlavor[0].equals(flavor);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return this.transferred;
    }

    @Override
    public String toString() {
        return "DatabaseObjectsTransferable{" +
                "transfered=" + transferred +
                '}';
    }
}
