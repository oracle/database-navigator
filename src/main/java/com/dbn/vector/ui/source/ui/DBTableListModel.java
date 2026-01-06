package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.util.Listeners;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.List;

import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;

public class DBTableListModel implements ListModel<DbTableSource> {
  private final Listeners<ListDataListener> listeners = Listeners.create();
  private final @Getter List<DbTableSource> tables;

  public DBTableListModel(List<DbTableSource> tables) {
    this.tables = tables;
  }

  @Override
  public int getSize() {
    return tables.size();
  }

  @Override
  public DbTableSource getElementAt(int index) {
    return tables.get(index);
  }

  public void reset(List<DbTableSource> tables) {
    this.tables.clear();
    addAll(tables);
  }

  public void moveRowsUp(int[] indices) {
    if (indices.length == 0) return;
    if (indices[0] == 0) return;

    for (int index : indices) {
      swap(index, index - 1);
    }

    ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0] -1, indices[indices.length - 1]);
    listeners.notify(l -> l.contentsChanged(event));

  }

  public void moveRowsDown(int[] indices) {
    if (indices.length == 0) return;
    if (indices[indices.length - 1] >= getSize()) return;

    for (int i = indices.length - 1; i >= 0; i--) {
      swap(indices[i], indices[i] + 1);
    }

    ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], indices[indices.length - 1] + 1);
    listeners.notify(l -> l.contentsChanged(event));
  }

  public void removeRows(int[] indices) {
    for (int i = indices.length - 1; i >= 0; i--) {
      int index = indices[i];
      if (index >= 0 && index < tables.size()) {
        tables.remove(index);
      }
    }

    ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], tables.size());
    listeners.notify(l -> l.contentsChanged(event));
  }

  private void swap(int index1, int index2) {
    if (index2 == -1) return;

    DbTableSource table1 = tables.get(index1);
    DbTableSource table2 = tables.get(index2);
    tables.set(index2, table1);
    tables.set(index1, table2);
  }

  public void addAll(Collection<DbTableSource> tables) {
    int index = this.tables.size();
    int count = 0;
    for (DbTableSource table : tables) {
      if (!this.tables.contains(table)) {
        this.tables.add(table);
        count++;
      }
    }

    ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, index, index + count);
    this.listeners.notify(l -> l.contentsChanged(event));
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
