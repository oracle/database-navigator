package com.dbn.common.ui.dialog;

import com.dbn.common.util.NotificationStatus;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ui.ConnectionPresentationChangeListener;
import com.intellij.util.messages.Topic;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.EventListener;
import java.util.EventObject;

public interface DialogNotificationListener extends EventListener {
    Topic<DialogNotificationListener> TOPIC =
            Topic.create("Dialog Notification Events", DialogNotificationListener.class);

    @Getter
    public static class NotificationStatusEvent extends EventObject {

        private final String name;
        private final NotificationStatus status;

        public NotificationStatusEvent(Object source, String name, NotificationStatus status) {
            super(source);
            this.name = name;
            this.status = status;
        }

        public boolean isMoreSevereThan(NotificationStatusEvent event) {
            return this.getStatus().compareTo(event.getStatus()) > 0;
        }

        public boolean isEqualOrMoreThan(NotificationStatusEvent event) {
            return this.getStatus().compareTo(event.getStatus()) >= 0;
        }
    }
    void fireNotificatonStatusEvent(NotificationStatusEvent event);

}
