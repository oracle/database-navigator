package com.dbn.execution.java.ui;

import com.dbn.common.data.Data;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import java.util.List;
import javax.swing.JTextField;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@UtilityClass
public class JavaExecutionInputUtil {

    public static void setupSingleDimArrayEditor(DBJavaField parameter,
                                          String               rawValue,
                                          Project project,
                                          TextFieldWithPopup<?> inputField,
                                          JTextField inputTextField) {
        setupSingleDimArrayEditor(parameter.getName(), getCanonicalName(parameter.getJavaClassRef()),
                parameter.getObjectType(), rawValue, project, inputField, inputTextField);
    }

    public static void setupSingleDimArrayEditor(DBJavaParameter parameter,
                                           String               rawValue,
                                           Project              project,
                                           TextFieldWithPopup<?> inputField,
                                           JTextField inputTextField){
        setupSingleDimArrayEditor(parameter.getName(), getCanonicalName(parameter.getJavaClassRef()),
                parameter.getObjectType(), rawValue, project, inputField, inputTextField);
    }

    @SneakyThrows
    private static void setupSingleDimArrayEditor(String      parameterName,
                                           String      parameterJavaType,
                                           DBObjectType objectType,
                                           String               rawValue,
                                           Project              project,
                                           TextFieldWithPopup<?> inputField,
                                           JTextField inputTextField) {

        Class<?> parameterJavaClass = Data.asPrimitiveClass(parameterJavaType);
        if(parameterJavaClass == null){
            parameterJavaClass = Class.forName(parameterJavaType);
        }
        Class<?> finalParameterJavaClass = parameterJavaClass;

        inputTextField.setEnabled(false);

        UserValueHolderImpl<List<?>> arrayUserValueHolder = new UserValueHolderImpl<>(
                parameterName, objectType, null, project);
        arrayUserValueHolder.setDataClass(finalParameterJavaClass);

        List<?> values = Data.csvToList(inputField.getText(), finalParameterJavaClass);
        arrayUserValueHolder.updateUserValue(values, false);
        inputField.setUserValueHolder(arrayUserValueHolder);

        inputField.createArrayEditorPopup(false);

        onTextChange(inputTextField, e -> {
            List newVal = Data.csvToList(inputField.getText(), finalParameterJavaClass);
            arrayUserValueHolder.setUserValue(newVal);
        });
    }
}
