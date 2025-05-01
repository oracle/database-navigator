package com.dbn.events.service;

import com.dbn.events.model.DataChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class HistoryService {
  private static final HistoryService INSTANCE = new HistoryService();

  private final Map<Long ,List<DataChangeEvent>> eventsByReg = new ConcurrentHashMap<>();
  private final Map<Long,List<Consumer<DataChangeEvent>>> listeners = new ConcurrentHashMap<>();

  private HistoryService() { }

  public static HistoryService getInstance() {
    return INSTANCE;
  }

  public synchronized void pushEvent(Long regId, DataChangeEvent event) {
    eventsByReg
            .computeIfAbsent(regId,k->new CopyOnWriteArrayList<>())
            .add(event);
    List<Consumer<DataChangeEvent>> listeners = this.listeners.get(regId);
    for (Consumer<DataChangeEvent> l : listeners) {
      l.accept(event);
    }
  }

  public synchronized List<DataChangeEvent> getEventsByReg() {
    return new ArrayList<>(eventsByReg);
  }

  public void registerListener(Consumer<DataChangeEvent> listener) {
    listeners.add(listener);
  }
}
