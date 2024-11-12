package com.dbn.connection;

import com.intellij.util.messages.Topic;

import java.util.EventListener;

/**
 * Listener to console switch from the UI
 */
public interface ConsoleChangeListener extends EventListener {
    Topic<ConsoleChangeListener> TOPIC = Topic.create("Connection console changed", ConsoleChangeListener.class);

    /**
     * Console change event
     * @param connectionId the connectionId that is now active
     */
    void consoleChanged(ConnectionId connectionId);
}
