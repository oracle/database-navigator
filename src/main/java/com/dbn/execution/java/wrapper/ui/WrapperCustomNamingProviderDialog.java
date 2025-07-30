package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.HashMap;
import java.util.Map;

@Getter
public class WrapperCustomNamingProviderDialog extends DBNDialog<WrapperCustomNamingProviderForm> {
    private final WrapperModel model;

    private String javaWrapperName;
    private String sqlWrapperName;
    private Map<String, String> sqlTypeNames;
    private Map<String, String> packageMethodNames;

    private boolean classLevel;
    public WrapperCustomNamingProviderDialog(Project project, WrapperModel model, boolean classLevel) {
        super(project, "Wrapper Custom Name", false);
        this.setModal(true);
        this.setAutoSize(true);
        this.model = model;
        this.classLevel = classLevel;
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected @NotNull WrapperCustomNamingProviderForm createForm() {
        return new WrapperCustomNamingProviderForm(this, model, classLevel);
    }

    @Override
    protected void doOKAction() {
        javaWrapperName = getForm().getJavaWrapperName();
        sqlWrapperName = getForm().getSqlWrapperName();
        sqlTypeNames = new HashMap<>(getForm().getSqlTypeNames());
        packageMethodNames = new HashMap<>(getForm().getPackageMethodNames());
        super.doOKAction();
    }
}
