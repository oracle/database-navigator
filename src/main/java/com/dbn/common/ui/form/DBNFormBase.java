package com.dbn.common.ui.form;

import com.dbn.common.action.DataProviders;
import com.dbn.common.dispose.ComponentDisposer;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.latent.Latent;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponentBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.util.HashSet;
import java.util.Set;

public abstract class DBNFormBase
        extends DBNComponentBase
        implements DBNForm, NotificationSupport {

    private boolean initialized;
    private final Set<JComponent> enabled = new HashSet<>();
    private final Latent<DBNFormFieldAdapter> fieldAdapter = Latent.basic(() -> DBNFormFieldAdapter.create(this));

    public DBNFormBase(@Nullable Disposable parent) {
        super(parent);
    }

    public DBNFormBase(@Nullable Disposable parent, @Nullable Project project) {
        super(parent, project);
    }

    protected DBNFormFieldAdapter getFieldAdapter() {
        return fieldAdapter.get();
    }

    @NotNull
    @Override
    public final JComponent getComponent() {
        if (!initialized) initialize();
        return getMainComponent();
    }

    /**
     * Passes on the runnable to the dispatch thread (Application.invokeLater) under full awareness of the component modality state
     * @param runnable the runnable to be sent to dispatch thread
     */
    protected void dispatch(Runnable runnable) {
        Dispatch.run(getMainComponent(), runnable);
    }

    /**
     * Allows invoking a task when the form is first shown.
     * Useful for delaying the execution of a given task when the modality state of the form is clarified.
     * Can also be helpful when component size decisions have to be taken based on surrounding container size.
     *
     * @param runnable the task to execute when the form is shown
     */
    protected void whenShown(Runnable runnable) {
        UserInterface.whenShown(getMainComponent(), runnable);
    }

    private void initialize() {
        initialized = true;
        JComponent mainComponent = getMainComponent();
        DataProviders.register(mainComponent, this);
        UserInterface.updateScrollPaneBorders(mainComponent);
        UserInterface.updateTitledBorders(mainComponent);
        UserInterface.updateSplitPanes(mainComponent);
        ApplicationEvents.subscribe(this, LafManagerListener.TOPIC, source -> lookAndFeelChanged());
        //GuiUtils.replaceJSplitPaneWithIDEASplitter(mainComponent);
    }

    protected void lookAndFeelChanged() {

    }

    protected abstract JComponent getMainComponent();

    public EnvironmentSettings getEnvironmentSettings(Project project) {
        return GeneralProjectSettings.getInstance(project).getEnvironmentSettings();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        return null;
    }

    @Override
    public void disposeInner() {
        JComponent component = getComponent();
        DataManager.removeDataProvider(component);
        ComponentDisposer.dispose(component);
        nullify();
    }

    public void freeze() {
        UserInterface.visitRecursively(getComponent(), c -> disable(c));
    }

    public void unfreeze() {
        UserInterface.visitRecursively(getComponent(), c -> enable(c));
    }

    private void disable(JComponent c) {
        if (c instanceof AbstractButton ||
                c instanceof JTextComponent ||
                c instanceof ActionToolbar ||
                c instanceof DBNButton) {

            if (c.isEnabled()) {
                enabled.add(c);
                c.setEnabled(false);
            }
        }
    }

    private void enable(JComponent c) {
        if (enabled.remove(c)) {
            c.setEnabled(true);
        }
    }
}
