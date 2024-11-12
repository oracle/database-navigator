package com.dbn.common.action;

import com.dbn.nls.NlsSupport;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class ComboBoxAction
        extends com.intellij.openapi.actionSystem.ex.ComboBoxAction
        implements BackgroundUpdateAware, NlsSupport {

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        JPanel panel=new JPanel(new GridBagLayout());
        ComboBoxButton button = new ComboBoxButton(presentation);
        GridBagConstraints constraints = new GridBagConstraints(
                0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, JBUI.insets(3), 0, 0);
        panel.add(button, constraints);
        panel.setFocusable(false);
        return panel;
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}
