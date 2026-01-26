package com.dbn.execution.java.ui;

import com.dbn.common.data.Data;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.execution.java.wrapper.support.WrapperSupportData;
import com.dbn.execution.java.wrapper.support.WrapperSupportInfo;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.UiSuitability.UI_NOT_PREFERRED;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.UiSuitability.UI_NOT_SUPPORTED;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.UiSuitability.UI_PREFERRED;
import static com.dbn.execution.java.wrapper.support.WrapperSupportEvaluator.evaluateArgumentSupport;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@UtilityClass
public class JavaExecutionInputUtil {

    public enum UiSuitability {
        UI_PREFERRED,
        UI_NOT_PREFERRED,
        UI_NOT_SUPPORTED
    }
    private final static int PREFERRED_MAXIMUM_DEPTH = 6;

    public static UiSuitability classifyForUi(DBJavaParameter parameter, WrapperSupportData supportData) {
        WrapperSupportInfo supportInfo = evaluateArgumentSupport(parameter, supportData);
        if (!supportInfo.isSupported()) return UI_NOT_SUPPORTED;

        int nestingLevel = supportInfo.getNestingLevel();
        if (nestingLevel == -1) return UI_NOT_SUPPORTED;
        if (nestingLevel > PREFERRED_MAXIMUM_DEPTH) return UI_NOT_PREFERRED;

        return UI_PREFERRED;
    }

    public static void setupSingleDimArrayEditor(TextFieldWithPopup<?> inputField, DBJavaField javaField) {
        setupSingleDimArrayEditor(
                javaField,
                javaField.getJavaClassRef(),
                inputField);
    }

    public static void setupSingleDimArrayEditor(TextFieldWithPopup<?> inputField, DBJavaParameter javaParameter) {
        setupSingleDimArrayEditor(
                javaParameter,
                javaParameter.getJavaClassRef(),
                inputField);
    }

    @SneakyThrows
    private static void setupSingleDimArrayEditor(
            DBObject argument,
            DBObjectRef<DBJavaClass> argumentJavaClass,
            TextFieldWithPopup<?> inputField) {

        DBObjectType argumentType = argument.getObjectType();
        Class<?> argumentClass = resolveArgumentType(argumentJavaClass);

        JTextField inputTextField = inputField.getTextField();
        inputTextField.setEnabled(false);

        Project project = argument.getProject();
        String argumentName = argument.getName();
        UserValueHolderImpl<List<?>> valueHolder = new UserValueHolderImpl<>(argumentName, argumentType, null, project);
        valueHolder.setDataClass(argumentClass);

        List<?> values = Data.arrayStringToList(inputField.getText(), argumentClass);
        valueHolder.updateUserValue(values, false);
        inputField.setUserValueHolder(valueHolder);

        inputField.createArrayEditorPopup(false);

        onTextChange(inputTextField, e -> {
            List valueList = Data.arrayStringToList(inputField.getText(), argumentClass);
            valueHolder.setUserValue(valueList);
        });
    }

    private static @NotNull Class<?> resolveArgumentType(DBObjectRef<DBJavaClass> argumentJavaClass) throws ClassNotFoundException {
        // assumed to be a java primitive or a scalar type (Number, String, Boolean... )
        String argumentTypeName = getCanonicalName(argumentJavaClass);
        Class<?> argumentType = Data.asPrimitiveClass(argumentTypeName);
        if (argumentType == null) {
            argumentType = Class.forName(argumentTypeName);
        }
        return argumentType;
    }
}
