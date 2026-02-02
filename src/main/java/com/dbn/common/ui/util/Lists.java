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

package com.dbn.common.ui.util;

import com.intellij.openapi.ui.SelectFromListDialog;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Lists {

    public static void notifyListDataListeners(Object source, Listeners<ListDataListener> listeners, int fromIndex, int toIndex, int eventType) {
        try {
            ListDataEvent event = new ListDataEvent(source, eventType, fromIndex, toIndex);
            listeners.notify(l -> {
                switch (eventType) {
                    case ListDataEvent.INTERVAL_ADDED:   l.intervalAdded(event); break;
                    case ListDataEvent.INTERVAL_REMOVED: l.intervalRemoved(event); break;
                    case ListDataEvent.CONTENTS_CHANGED: l.contentsChanged(event); break;
                }
            });
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Error notifying actions model listeners", e);
        }
    }

    public static final SelectFromListDialog.ToStringAspect BASIC_TO_STRING_ASPECT = obj -> obj.toString();


    public static void onSelectionChange(JList list, Consumer<ListSelectionEvent> eventConsumer) {
        list.addListSelectionListener(e -> eventConsumer.accept(e));
    }

    public static void onModelChange(JList list, Consumer<ListDataEvent> eventConsumer) {
        list.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                eventConsumer.accept(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                eventConsumer.accept(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                eventConsumer.accept(e);
            }
        });
    }

    public static <T> Iterable<T> modelIterable(ListModel<T> model) {
        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < model.getSize();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return model.getElementAt(index++);
            }
        };
    }

    public static <T> Stream<T> modelStream(ListModel<T> model) {
        return IntStream.range(0, model.getSize())
                .mapToObj(model::getElementAt);
    }
}
