package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.object.common.DBObject;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class WrapperCustomNamingProviderForm extends DBNFormBase {
    private WrapperModel model;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel objectsPanel;

    private JTextField sqlWrapperNameTextField;
    private JTextField javaWrapperNameTextField;
    private final List<JTextField> sqlTypeFields = new ArrayList<>();
    private final List<JTextField> packageMethodFields = new ArrayList<>();
    private final Map<String, String> sqlTypeNames = new HashMap<>();
    private final Map<String, String> packageMethodNames = new HashMap<>();

    public WrapperCustomNamingProviderForm(WrapperCustomNamingProviderDialog dialog, WrapperModel model, boolean classLevel) {
        super(dialog);
        this.model = model;
        initHeaderPanel(model);
        initHintPanel();
        initObjectList(model, classLevel);
    }

    private void initHeaderPanel(WrapperModel model) {
        DBObject sourceObject = model.getSourceObject();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, sourceObject);
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain("The following execution wrapper object types have length more than allowed limit from the database.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(WrapperModel model, boolean classLevel) {
        verticalBoxLayout(objectsPanel);
        objectsPanel.setBorder(new RoundedLineBorder(Colors.getOutlineColor(), 2));

        JPanel contentPanel = new JPanel();
        verticalBoxLayout(contentPanel);

        Set<String> sqlTypeNames = model.getSqlTypeNames();
        for (String sqlType : sqlTypeNames) {
                JTextField textField = new JTextField(sqlType, 50);
                // Hide valid names, to create map later
                textField.setVisible(sqlType.length() > 30);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.add(new JLabel(Icons.DBO_TYPE));
                row.add(new JLabel("SQL Type"));
                row.add(textField);
                contentPanel.add(row);

                sqlTypeFields.add(textField);
        }

        if (model.getSqlWrapperName().length() > 30) {
            sqlWrapperNameTextField = new JTextField(model.getSqlWrapperName(), 50);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.add(new JLabel(model.getSqlWrapperIcon()));
            row.add(new JLabel("SQL Wrapper"));
            row.add(sqlWrapperNameTextField);
            contentPanel.add(row);
        }

        if (model.getJavaWrapperName().length() > 30) {
            javaWrapperNameTextField = new JTextField(model.getJavaWrapperName(), 50);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.add(new JLabel(Icons.DBO_JAVA_CLASS));
            row.add(new JLabel("Java Wrapper"));
            row.add(javaWrapperNameTextField);
            contentPanel.add(row);
        }

        if(classLevel) {
            for (MethodWrapper methodWrapper : model.getMethods()) {
                JTextField textField = new JTextField(methodWrapper.getSqlMethodName(), 50);
                textField.setEnabled(methodWrapper.getSqlMethodName().length() > 30);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.add(new JLabel(methodWrapper.getIcon()));
                row.add(new JLabel("SQL package method"));
                row.add(textField);
                contentPanel.add(row);
                packageMethodFields.add(textField);
            }
        }

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);

        objectsPanel.add(scrollPane);
    }

    @Override
    protected void initValidation() {
        if (javaWrapperNameTextField != null)
            addTextValidation(javaWrapperNameTextField, p -> p.length() <= 30, "Length should be less than 31 characters");

        if (sqlWrapperNameTextField != null)
            addTextValidation(sqlWrapperNameTextField, p -> p.length() <= 30, "Length should be less than 31 characters");

        for (JTextField textField : packageMethodFields) {
            addTextValidation(textField, p -> p.length() <= 30, "Length should be less than 31 characters");
        }

        for(JTextField sqlTypeField : sqlTypeFields) {
            addTextValidation(sqlTypeField, p -> p.length() <= 30, "Length should be less than 31 characters");
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public String getJavaWrapperName() {
        return javaWrapperNameTextField == null ? model.getJavaWrapperName() : javaWrapperNameTextField.getText();
    }

    public String getSqlWrapperName() {
        return sqlWrapperNameTextField == null ? model.getSqlWrapperName() : sqlWrapperNameTextField.getText();
    }

    public Map<String, String> getSqlTypeNames() {
        List<String> longSqlTypeNamesList = new ArrayList<>(model.getSqlTypeNames());
        for (int i = 0; i < sqlTypeFields.size(); i++) {
            sqlTypeNames.put(longSqlTypeNamesList.get(i), sqlTypeFields.get(i).getText());
        }
        return sqlTypeNames;
    }

    public Map<String, String> getPackageMethodNames() {
        for (int i = 0; i < packageMethodFields.size(); i++) {
            packageMethodNames.put(model.getMethods().get(i).getSqlMethodName(), packageMethodFields.get(i).getText());
        }
        return packageMethodNames;
    }
}
