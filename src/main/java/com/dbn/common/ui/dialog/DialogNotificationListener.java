package com.dbn.common.ui.dialog;

import com.dbn.common.util.NotificationStatus;
import com.intellij.util.messages.Topic;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;
import java.util.EventObject;

/**
 * Defines a Project-level {@link com.intellij.util.messages.MessageBus} TOPIC.
 * This listener and associated TOPIC and event objects are meant to communicate new status
 * conditions to display in a {@link DialogNotificationPanel}.
 */
public interface DialogNotificationListener extends EventListener {
    /**
     * A {@link com.intellij.util.messages.MessageBus} TOPIC that the DialogNotification
     * Panel can subscribe to.
     */
    //@Topic.ProjectLevel
    Topic<DialogNotificationListener> TOPIC =
            Topic.create("Dialog Notification Events", DialogNotificationListener.class);

    /**
     * The event that a {@link Topic} subscribes to listen for
     */
    @Getter
    class NotificationStatusEvent extends EventObject {

        private final String name;
        private final NotificationStatus status;

        /**
         *
         * @param source the event source
         * @param name the name of the event
         * @param status the status condition
         */
        public NotificationStatusEvent(@NotNull Object source, @NotNull String name, @NotNull NotificationStatus status) {
            super(source);
            this.name = name;
            this.status = status;
        }

        /**
         * @param event another event compare to
         * @return true if this event's severity is strictly higher than event
         */
        public boolean isMoreSevereThan(NotificationStatusEvent event) {
            return this.getStatus().compareTo(event.getStatus()) > 0;
        }

        /**
         * @param event another event compare to.
         * @return true if this event's severity is higher or equal to event
         */
        public boolean isEqualOrMoreThan(NotificationStatusEvent event) {
            return this.getStatus().compareTo(event.getStatus()) >= 0;
        }
    }

    /**
     * Fire a {@link NotificationStatusEvent}
     * @param event the event to fire
     */
    void fireNotificationStatusEvent(NotificationStatusEvent event);

}
