package com.dbn.execution.java.ui;

import com.dbn.common.data.Data;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
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
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@UtilityClass
public class JavaExecutionInputUtil {

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

        List<?> values = Data.csvToList(inputField.getText(), argumentClass);
        valueHolder.updateUserValue(values, false);
        inputField.setUserValueHolder(valueHolder);

        inputField.createArrayEditorPopup(false);

        onTextChange(inputTextField, e -> {
            List newVal = Data.csvToList(inputField.getText(), argumentClass);
            valueHolder.setUserValue(newVal);
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
