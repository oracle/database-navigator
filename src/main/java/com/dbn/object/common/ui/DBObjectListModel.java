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

package com.dbn.object.common.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.util.Listeners;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.Disposable;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBObjectListModel<T extends DBObject> extends StatefulDisposableBase implements ListModel<T> {
    private final List<T> data = new ArrayList<>();
    private final Listeners<ListDataListener> listeners = Listeners.create(this);

    public DBObjectListModel(Disposable parent, List<T> data) {
        setData(data);
        Disposer.register(parent, this);
    }

    public static <T extends DBObject> DBObjectListModel<T> create(Disposable parent) {
        return create(parent, Collections.emptyList());
    }

    public static <T extends DBObject> DBObjectListModel<T> create(Disposable parent, List<T> data) {
        return new DBObjectListModel<>(parent, data);
    }

    public void setData(List<T> objects) {
        data.clear();
        data.addAll(objects);
        notifyListeners();
    }

    private void notifyListeners() {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, data.size());
        listeners.notify(l -> l.contentsChanged(event));
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public T getElementAt(int i) {
        return data.get(i);
    }

    public List<T> getElements() {
        return data;
    }

    @Override
    public void addListDataListener(ListDataListener listDataListener) {
        listeners.add(listDataListener);
    }

    @Override
    public void removeListDataListener(ListDataListener listDataListener) {
        listeners.remove(listDataListener);
    }

    @Override
    public void disposeInner() {
        data.clear();
    }
}
