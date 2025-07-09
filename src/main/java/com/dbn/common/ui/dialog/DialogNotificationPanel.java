package com.dbn.common.ui.dialog;

import com.dbn.common.color.Colors;
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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
public class DialogNotificationPanel extends JPanel implements  Disposable {
    public static final String DEFAULT_NOTIFICATION_NAME = "default";

    private Color backgroundColor;
    private final CardLayout cardLayout;
    private final Project project;
    private @Nullable Disposable parentDisposable;
    private @Nullable String defaultComponent;
    private LinkedHashMap<String, NotificationStatusPanel> statusComponents = new LinkedHashMap<>();
    private PriorityQueue<SeverityPrioritizedStack> statusEvents =
            new PriorityQueue<>();
    private DialogNotificationListener.NotificationStatusEvent curEvent;
    private final AtomicBoolean notificationsEnabled = new AtomicBoolean();
    private final AtomicBoolean disposed = new AtomicBoolean();

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            statusComponents.clear();
            backgroundColor = null;
            statusEvents.clear();
            statusEvents = null;
            curEvent = null;
        }
    }

    public void disableNotifications() {
        this.notificationsEnabled.set(false);
    }

    public void enableNotifications() {
        this.notificationsEnabled.set(true);
    }

    protected static class NotificationUI {
        public NotificationUI(@NotNull String key, NotificationStatusPanel uiComponent) {
            this.uiComponent = uiComponent;
            this.key = key;
        }
        private final NotificationStatusPanel uiComponent;
        private final String key;
    }
    public final static class Builder {
        private Color bgColor;
        private Project project;
        private Disposable parentDisposable;
        private final LinkedHashMap<String, NotificationUI> components = new LinkedHashMap<>();
        private String defaultComponent;

        public Builder() {

        }

        public Builder addComponent(String key) {
            addComponent(null, key);
            return this;
        }
        public Builder addComponent(NotificationStatusPanel component, String key) {
            if (key == null) {
                throw new IllegalArgumentException("key can't be null");
            }
            if (DEFAULT_NOTIFICATION_NAME.equals(key)) {
                throw new IllegalArgumentException("Cannot overiride DEFAULT_NOTIFICATION_NAME key:"+key);
            }
            components.put(key, new NotificationUI(key, component));
            return this;
        }

        public Builder defaultComponent(String key) {
            if (key == null || (!components.containsKey(key) && !DEFAULT_NOTIFICATION_NAME.equals(key))) {
                throw new IllegalArgumentException("key must have a corresponding notification component: "+key);
            }
            this.defaultComponent = key;
            return this;
        }

        public Builder backgroundColor(Color  bgColor) {
            this.bgColor = bgColor;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder parentDisposable(Disposable parentDisposable) {
            this.parentDisposable = parentDisposable;
            return this;
        }
        public DialogNotificationPanel build() {
            // make sure we have the minimum attributes
            Optional<String> result = validate();
            result.ifPresent(message -> {
                throw new IllegalArgumentException(message);
            });

            DialogNotificationPanel instance = new DialogNotificationPanel(this.project);
            instance.setBackground(this.bgColor);
            instance.setParentDisposable(this.parentDisposable);
            for (NotificationUI component : components.values()) {
                instance.addNotificationComponent(component.key,
                        component.uiComponent != null ? component.uiComponent :
                                new DefaultNotificationStatusPanel(Colors.getLabelForeground()));
                if (component.key.equals(defaultComponent)) {
                    instance.setDefaultComponent(defaultComponent);
                }
            }
            // if the caller didn't override the default event panel,
            // add the "default default"
            DefaultNotificationStatusPanel defaultStatusPanel =
                    new DefaultNotificationStatusPanel(Colors.getLabelForeground());
            if (components.isEmpty()) {
                instance.addNotificationComponent(DEFAULT_NOTIFICATION_NAME,defaultStatusPanel);
            }
            return instance;
        }

        public Optional<String> validate() {
            if (project == null) {
                return Optional.of("Must specify a project");
            }
            return Optional.empty();
        }
    }
    protected DialogNotificationPanel(Project project) {
        this.project  = project;
        this.cardLayout = new CardLayout();
        setLayout(this.cardLayout);
    }

    private void addNotificationComponent(String key, NotificationStatusPanel uiComponent) {
        this.statusComponents.put(key, uiComponent);
        this.cardLayout.addLayoutComponent(uiComponent, key);
        add(uiComponent);
    }

    public void init() {
        if (defaultComponent != null) {
            this.cardLayout.show(this, defaultComponent);
        }
        ProjectEvents.subscribe(this.project, parentDisposable, DialogNotificationListener.TOPIC,
                event -> {
                    // TODO: disconnect instead?
                    if (!notificationsEnabled.get()) {
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
                                BiFunction<ListIterator, DialogNotificationListener.NotificationStatusEvent, Boolean> searchVisitor) {
        boolean found = false;
        OUTER_LOOP: for (SeverityPrioritizedStack stack : statusEvents) {
            ListIterator iterator = stack.eventStack.listIterator();
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

    public static abstract class NotificationStatusPanel extends JPanel {
        protected abstract void initUI();

        public abstract void updateStatus(NotificationStatus status);
    }
    public static class DefaultNotificationStatusPanel extends NotificationStatusPanel {
        private final Color fgColor;
        private JBLabel errorLabel;

        public DefaultNotificationStatusPanel(Color fgColor) {
            this.fgColor = fgColor;
            initUI();
        }

        protected void initUI() {
            setLayout(new BorderLayout());
            this.errorLabel = new JBLabel();
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
