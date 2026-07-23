package com.dbn.common.ui.form;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.Disposable;
import lombok.Getter;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import static com.dbn.common.ui.table.Tables.onModelChange;
import static com.dbn.common.ui.util.CheckBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.Lists.onModelChange;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.UserInterface.visitRecursively;

/** Monitors user interaction with form fields and tracks whether a form changed. */
@Getter
public class DBNFormMonitor {
    private boolean changed;
    private final WeakRef<DBNForm> form;

    public DBNFormMonitor(DBNForm form) {
        this.form = WeakRef.of(form);
    }

    public DBNForm getForm() {
        return form.ensure();
    }

    public void init() {
        visitRecursively(getForm().getComponent(), c -> monitorComponent(c));
    }

    private void monitorComponent(JComponent component) {
        if (component instanceof JTextComponent textComponent) {
            onTextChange(textComponent, e -> markChanged());

        } else if (component instanceof JComboBox<?> comboBox) {
            onSelectionChange(comboBox, value -> markChanged());

        } else if (component instanceof JList<?> list) {
            onModelChange(list, e -> markChanged());

        } else if (component instanceof JTable table) {
            onModelChange(table, e -> markChanged());

        } else if (component instanceof JToggleButton) {
            onSelectionChange((AbstractButton) component, e -> markChanged());
        }
    }

    public void reset() {
        changed = false;
    }

    public void markChanged() {
        markChanged(true);
    }

    private void markChanged(boolean propagate) {
        changed = true;
        if (propagate) {
            Disposable parent = getForm().getParentComponent();
            while (parent instanceof DBNFormBase parentForm) {
                parentForm.getMonitor().markChanged(false);
                parent = parentForm.getParentComponent();
            }
        }
        DBNDialog dialog = getForm().getParentDialog();
        if (dialog != null) dialog.updateDialogButtons();
    }
}
