package com.dbn.common.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
@Getter
public class NotificationStatus implements Comparable<NotificationStatus> {

    public enum Severity {
        NONE,
        INFO,
        WARNING,
        ERROR,
        FATAL;
    }

    public final static NotificationStatus NONE = new NotificationStatus(Severity.NONE, "");

    private final Severity severity;
    private final String message;

    public NotificationStatus(@NotNull Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }
    @Override
    public int compareTo(@NotNull NotificationStatus notificationStatus) {
        return this.severity.compareTo(notificationStatus.severity);
    }

    public boolean equals(Object other) {
        if (other instanceof NotificationStatus) {
            return this.compareTo((NotificationStatus) other) == 0;
        }
        return false;
    }

    public int hashCode() {
        int code = message == null ? 67 : message.hashCode();
        code += this.severity.hashCode() * 71;
        return code;
    }
}
