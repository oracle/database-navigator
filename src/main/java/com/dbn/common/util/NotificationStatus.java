package com.dbn.common.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
@Getter
public class NotificationStatus implements Comparable<NotificationStatus> {

    /**
     * The severity of the status.  Order is important.
     */
    public enum Severity {
        /**
         * A special status that indicates "ALL CLEAR" for
         * a specific status type
         */
        NONE,
        /**
         * There is no error. Message is informational and
         * can be ignored or potentially dismissed.
         */
        INFO,
        /**
         * There is a warning condition.  The message contains
         * information about a problem that may exist but which
         * might be safely ignored.
         */
        WARNING,
        /**
         * There is an error condition.  The message contains
         * information about an issue that the user should not
         * ignore.  The user may need to apply remedial action.
         */
        ERROR,
        /**
         * A severe error has occurred that has rendered some
         * subsystem non-working.  The user should definitely
         * address any advisor in the message and follow it up
         * with remedial action.
         */
        FATAL;
    }

    /**
     * Constant for the NONE status.
     * This is a special severity because it signals clearing all status of a particular type.
     */
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
