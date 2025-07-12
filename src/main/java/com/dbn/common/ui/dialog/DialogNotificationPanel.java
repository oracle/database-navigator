package com.dbn.common.ui.dialog;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Nullifier;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.NotificationStatus;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/**
 * A JPanel that can be added to a dialog (maybe other header forms?) that
 * will subscribe to {@link DialogNotificationListener#TOPIC} on a project's
 * IntelliJ MessageBus and displays customizable JPanels based on an event that
 * has a name and a  {@link NotificationStatus}.  For each event name, messages
 * are priorized based on the {@link NotificationStatus}'s severity which allows
 * for multiple events to be queued when more than one is possible.
 *
 * Use the builder to construct a new instance.  Project, parentDisposable and
 * at least one component are required for construction to succeed, else an
 * exception will be thrown.  You can check builder's readiness by calling
 * {@link Builder#validate()}. Use {@link Builder#build()} to construct a new instance.
 *
 * At construction, notifications are turned off.  To start reacting to the
 * the {@link com.intellij.util.messages.MessageBus} topic, you must call
 * {@link #enableNotifications()}.  Use {@link #disableNotifications()} to turn
 * them off.
 */
@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
public final class DialogNotificationPanel extends JPanel implements StatefulDisposable {
    private final CardLayout cardLayout;
    private final Project project;
    private @NotNull Disposable parentDisposable;
    private final LinkedHashMap<String, NotificationStatusPanel> statusComponents = new LinkedHashMap<>();
    private final PriorityQueue<SeverityPrioritizedStack> statusEvents =
            new PriorityQueue<>();
    private final AtomicBoolean notificationsEnabled = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private DialogNotificationListener.NotificationStatusEvent curEvent;

    @Override
    public boolean isDisposed() {
        return this.disposed.get();
    }

    @Override
    public void setDisposed(boolean disposed) {
        this.disposed.set(disposed);
    }

    @Override
    public void disposeInner() {
        Nullifier.nullify(this);
    }

    /**
     * Used to encapsulate a notification panel and a related event key to
     * use it with.
     *
     */
    private static class NotificationUI {
        private final NotificationStatusPanel uiComponent;
        private final String key;

        /**
         *
         * @param key the event key this uiComponent will be used for.
         * @param notificationStatusPanel the panel
         */
        public NotificationUI(@NotNull String key, NotificationStatusPanel notificationStatusPanel) {
            this.uiComponent = notificationStatusPanel;
            this.key = key;
        }
    }

    /**
     * Builder pattern used to construct new instances of {@link DialogNotificationPanel}
     */
    public final static class Builder {
        private Color bgColor;
        private Project project;
        private Disposable parentDisposable;
        private final LinkedHashMap<String, NotificationUI> components = new LinkedHashMap<>();

        /**
         * Add a default notification panel with the event key
         * @param key
         * @return this
         */
        public Builder addComponent(String key) {
            addComponent(null, key);
            return this;
        }

        /**
         * Add a {@link NotificationStatusPanel} under the associated event key
         * @param component
         * @param key
         * @return this
         */
        public Builder addComponent(NotificationStatusPanel component, String key) {
            if (key == null) {
                throw new IllegalArgumentException("key can't be null");
            }
            components.put(key, new NotificationUI(key, component));
            return this;
        }

        /**
         * The background color for the {@link DialogNotificationPanel}
         * @param bgColor the color
         * @return this
         */
        public Builder backgroundColor(Color  bgColor) {
            this.bgColor = bgColor;
            return this;
        }

        /**
         * The project scope of the panel.
         *
         * @param project
         * @return this
         */
        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        /**
         * A parent disposable to use for the panel and the associated {@link com.intellij.util.messages.MessageBus}
         * event subscription
         *
         * @param parentDisposable
         * @return
         */
        public Builder parentDisposable(Disposable parentDisposable) {
            this.parentDisposable = parentDisposable;
            return this;
        }

        /**
         * Create a new instance of {@link DialogNotificationPanel} with this builder's info
         * @return
         */
        public DialogNotificationPanel build() {
            // make sure we have the minimum attributes
            Optional<String> result = validate();
            result.ifPresent(message -> {
                throw new IllegalArgumentException(message);
            });

            DialogNotificationPanel instance = new DialogNotificationPanel(this.project);
            if (this.bgColor != null) {
                instance.setBackground(this.bgColor);
            }
            instance.setParentDisposable(this.parentDisposable);
            for (NotificationUI component : components.values()) {
                instance.addNotificationComponent(component.key,
                        component.uiComponent != null ? component.uiComponent :
                                new DefaultNotificationStatusPanel(Colors.getLabelForeground()));
            }

            return instance;
        }

        /**
         * @return an empty {@link Optional} if this builder is valid for calling {@link #build()},
         * false otherwise.
         */
        public Optional<String> validate() {
            if (project == null) {
                return Optional.of("Must specify a project");
            }
            if (parentDisposable == null) {
                return Optional.of("Must specify a parentDisposable");
            }
            if (components.isEmpty()) {
                return Optional.of("Must specify at least one component");
            }
            return Optional.empty();
        }
    }

    /**
     * @param project The project to listen to message events in.
     */
    protected DialogNotificationPanel(@NotNull Project project) {
        this.project  = project;
        this.cardLayout = new CardLayout();
        setLayout(this.cardLayout);
    }

    private void addNotificationComponent(String key, NotificationStatusPanel uiComponent) {
        this.statusComponents.put(key, uiComponent);
        this.cardLayout.addLayoutComponent(uiComponent, key);
        add(uiComponent);
    }

    /**
     * Stop this panel from reacting to {@link com.intellij.util.messages.MessageBus} events.
     */
    public void disableNotifications() {
        this.notificationsEnabled.set(false);
    }

    /**
     * Start this panel from reacting to {@link com.intellij.util.messages.MessageBus} events
     */
    public void enableNotifications() {
        this.notificationsEnabled.set(true);
    }

    /**
     * Call this initialize the panel for use.  This immediately subscribe to the
     * {@link com.intellij.util.messages.MessageBus} TOPIC and, if {@link #enableNotifications()}
     * has been called, the panel will start displaying notification events on the bus.
     */
    public void init() {
        Disposer.register(parentDisposable, this);
        /**
         * TODO: move this to a new Subscriber class?
         */
        ProjectEvents.subscribe(this.project, parentDisposable, DialogNotificationListener.TOPIC,
                event -> {
                    // TODO: disconnect instead?
                    if (disposed.get() || !notificationsEnabled.get()) {
                        return;
                    }
                    boolean changeUI = false;
                    // sending a None severity means clear this event condition
                    // TODO: should we separate severity from the clear condition?
                    if (event.getStatus().getSeverity() == NotificationStatus.Severity.NONE) {
                        // clear all events with this name from the priority queue
                        searchForEvents(event.getName(), (it, e) -> {
                            it.remove();
                            return Boolean.TRUE;  //continue
                        });
                        // get the next event by priority if there is one.
                        if (curEvent != null && curEvent.getName().equals(event.getName())) {
                            //clear curEvent if same as a clear event with matching name
                            curEvent = null;
                            while (!statusEvents.isEmpty()) {
                                SeverityPrioritizedStack spStack = statusEvents.peek();
                                if (!spStack.eventStack.isEmpty()) {
                                    curEvent = spStack.eventStack.pop();
                                    break;
                                }
                                else {
                                    // remove if empty
                                    statusEvents.remove(spStack);
                                }
                            }
                            changeUI = true;
                        }
                        // if no event, hide the panel
                        if (curEvent == null) {
                            changeUI = true;
                        }
                    }
                    else {
                        // if not NONE see if the new event is a higher priority than the current
                        // one.
                        if (curEvent == null) {
                            curEvent = event;
                            changeUI = true;
                        }
                        else {
                            // if equal severity give the newest event priority
                            if (event.isEqualOrMoreThan(curEvent)) {
                                pushEvent(curEvent);
                                curEvent = event;
                                changeUI = true;
                            }
                            // if new event is lower priority, push it and keep the current one.
                            else {
                                pushEvent(event);
                            }
                        }
                    }

                    if (changeUI && curEvent != null &&
                            curEvent.getStatus().getSeverity() != NotificationStatus.Severity.NONE) {
                        String name = curEvent.getName();
                        NotificationStatusPanel notificationStatusPanel = statusComponents.get(name);
                        if (notificationStatusPanel != null) {
                            notificationStatusPanel.updateStatus(curEvent.getStatus());
                            cardLayout.show(DialogNotificationPanel.this, name);
                            DialogNotificationPanel.this.setVisible(true);
                        }
                    } else {
                        if (curEvent == null) {
                            DialogNotificationPanel.this.setVisible(false);
                        }
                    }
                    DialogNotificationPanel.this.invalidate();
                    DialogNotificationPanel.this.getParent().doLayout();
                });
    }

    /**
     *
     * @param eventName
     * @param searchVisitor
     * @return true if at least on event was found, regardless of whether searchVisitor processed it.
     */
    public boolean searchForEvents(String eventName,
                                BiFunction<ListIterator<?>, DialogNotificationListener.NotificationStatusEvent, Boolean> searchVisitor) {
        boolean found = false;
        OUTER_LOOP: for (SeverityPrioritizedStack stack : statusEvents) {
            ListIterator<?> iterator = stack.eventStack.listIterator();
            while (iterator.hasNext()) {
                DialogNotificationListener.NotificationStatusEvent event = (DialogNotificationListener.NotificationStatusEvent) iterator.next();
                if (event.getName().equals(eventName)) {
                    found = true;
                    Boolean shouldContinue = searchVisitor.apply(iterator, event);
                    if (!shouldContinue.booleanValue()) {
                        break OUTER_LOOP;
                    }
                }
            }
        }
        return found;
    }

    public boolean searchForEvents(String eventName) {
        return searchForEvents(eventName, (s,e) -> {
            // quit as soon as we find one
            return false;
        });
    }
    private void pushEvent(DialogNotificationListener.NotificationStatusEvent event) {
        final NotificationStatus.Severity severity = event.getStatus().getSeverity();
        Optional<SeverityPrioritizedStack> stackOptional =  statusEvents.stream()
                .filter(p -> p.getSeverity() == severity)
                .findFirst();
        SeverityPrioritizedStack stack = stackOptional.orElseGet( () -> {
            SeverityPrioritizedStack newStack = new SeverityPrioritizedStack(new Stack<>(), severity);
            statusEvents.add(newStack);
            return newStack;
        });
        stack.eventStack.push(event);
    }

    /**
     * Each notification event type can be associated with it's own
     * panel.  Clients can extend this class or sub-class {@link DefaultNotificationStatusPanel}
     */
    public static abstract class NotificationStatusPanel extends JPanel {
        protected abstract void initUI();

        public abstract void updateStatus(NotificationStatus status);
    }

    /**
     * A default status panel implementation.
     */
    public static class DefaultNotificationStatusPanel extends NotificationStatusPanel {
        private final Color fgColor;
        private JBLabel errorLabel;

        /**
         * The foreground color for the panel.
         *
         * @param fgColor
         */
        public DefaultNotificationStatusPanel(Color fgColor) {
            this.fgColor = fgColor;
            initUI();
        }

        protected void initUI() {
            setLayout(new BorderLayout());
            this.errorLabel = new JBLabel();
            this.errorLabel.setForeground(fgColor);
            add(this.errorLabel, BorderLayout.PAGE_START);
        }

        public void updateStatus(NotificationStatus status) {
            NotificationStatus.Severity severity = status.getSeverity();
            Icon icon = null;
            switch (severity) {
                case INFO:
                    icon = Icons.DIALOG_INFORMATION;
                    break;
                case WARNING:
                    icon = Icons.DIALOG_WARNING;
                    break;
                case ERROR:
                case FATAL:
                    icon = Icons.DIALOG_ERROR;
                    break;
            }
            this.errorLabel.setIcon(icon);
            this.errorLabel.setText(status.getMessage());
        }
    }

    @AllArgsConstructor
    @Getter
    private static class SeverityPrioritizedStack implements Comparable<SeverityPrioritizedStack> {
        private final Stack<DialogNotificationListener.NotificationStatusEvent>  eventStack;
        private final NotificationStatus.Severity severity;

        @Override
        public int compareTo(@NotNull DialogNotificationPanel.SeverityPrioritizedStack other) {
            // enums are "greater" if they are further right in the definition but
            // the queue makes the "smallest" value the highest priority.
            return severity.compareTo(other.getSeverity()) * -1;
        }
    }
}
