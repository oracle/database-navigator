/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.ui.file;

import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Lists;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;

public class VirtualFileListModel implements ListModel<VirtualFile> {
    private final Listeners<ListDataListener> listeners = Listeners.create();
    private final @Getter List<VirtualFile> files;

    public VirtualFileListModel(List<VirtualFile> files) {
        this.files = new ArrayList<>(files);
    }

    public void add(VirtualFile file) {
        int index = this.files.size();
        this.files.add(file);
        this.listeners.notify(l -> {
            ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, index, files.size());
            l.contentsChanged(event);
        });
    }

    public List<String> getFilePaths() {
        return Lists.convert(files, f -> f.getPath());
    }

    public void addAll(Collection<VirtualFile> files) {
        int index = this.files.size();
        this.files.addAll(files);

        this.listeners.notify(l -> {
            ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, index, files.size());
            l.contentsChanged(event);
        });
    }

    @Override
    public int getSize() {
        return files.size();
    }

    @Override
    public VirtualFile getElementAt(int index) {
        return files.get(index);
    }

    public void moveRowsUp(int[] indices) {
        if (indices.length == 0) return;
        if (indices[0] == 0) return;

        for (int index : indices) {
            swap(index, index - 1);
        }

        listeners.notify(l -> {
            ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0] -1, indices[indices.length - 1]);
            l.contentsChanged(event);
        });

    }

    public void moveRowsDown(int[] indices) {
        if (indices.length == 0) return;
        if (indices[indices.length - 1] >= getSize()) return;

        for (int i = indices.length - 1; i >= 0; i--) {
            swap(indices[i], indices[i] + 1);
        }
        listeners.notify(l -> {
            ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], indices[indices.length - 1] + 1);
            l.contentsChanged(event);
        });
    }

    private void swap(int index1, int index2) {
        if (index2 == -1) return;

        VirtualFile file1 = files.get(index1);
        VirtualFile file2 = files.get(index2);
        files.set(index2, file1);
        files.set(index1, file2);
    }

    public void removeRows(int[] indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            int index = indices[i];
            if (index >= 0 && index < files.size()) {
                files.remove(index);
            }
        }

        listeners.notify(l -> {
            ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], files.size());
            l.contentsChanged(event);
        });
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }
}
