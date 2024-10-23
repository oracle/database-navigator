/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.ui.form.field;

import com.dbn.common.filter.Filter;
import com.dbn.common.lookup.Condition;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.ClientProperty;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.dbn.common.ui.util.ClientProperty.ACCESSIBILITY_CONDITION;
import static com.dbn.common.ui.util.ClientProperty.VISIBILITY_CONDITION;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.common.util.Unsafe.warned;

/**
 * Cached implementation of {@link DBNFormFieldAdapter}
 * It uses reflection to identify editable {@link JComponent} fields.
 *
 * @author Dan Cioca (Oracle)
 */
class DBNFormFieldAdapterImpl implements DBNFormFieldAdapter {
    private final Set<JComponent> fields = ContainerUtil.createWeakSet();

    DBNFormFieldAdapterImpl(DBNForm form) {
        Class formClass = form.getClass();
        Field[] fields = formClass.getDeclaredFields();
        for (Field field : fields) {
            JComponent component = unwrapComponent(field, form);
            if (component == null) continue;

            this.fields.add(component);
        }
    }

    @Override
    public void classifyFields(JComponentCategory category, Filter<JComponent> filter) {
        fields(filter).forEach(c -> category.classify(c));
    }

    @Override
    public void captureFieldValues(Filter<JComponent> filter) {
        fields(filter).forEach(c -> captureFieldValue(c));
    }

    @Override
    public void resetFieldValues(Filter<JComponent> filter) {
        fields(filter).forEach(c -> setFieldValue(c, null));
    }

    @Override
    public void restoreFieldValues(Filter<JComponent> filter) {
        fields(filter).forEach(c -> restoreFieldValue(c));
    }

    @Override
    public void updateFields(Filter<JComponent> filter, Consumer<JComponent> consumer) {
        fields(filter).forEach(consumer);
    }

    @Override
    public void initFieldsVisibility(Condition condition, Filter<JComponent> filter) {
        fields(filter).forEach(c -> VISIBILITY_CONDITION.set(c, condition));
    }

    @Override
    public void initFieldsAccessibility(Condition condition, Filter<JComponent> filter) {
        fields(filter).forEach(c -> ACCESSIBILITY_CONDITION.set(c, condition));
    }

    @Override
    public void updateFieldsVisibility() {
        for (JComponent component : fields) {
            Condition condition = VISIBILITY_CONDITION.get(component);
            if (condition == null) continue;

            boolean visible = condition.check();
            component.setVisible(visible);
        }
    }

    @Override
    public void updateFieldsAccessibility() {
        for (JComponent component : fields) {
            Condition condition = ACCESSIBILITY_CONDITION.get(component);
            if (condition == null) continue;

            boolean accessible = condition.check();
            component.setEnabled(accessible);
        }
    }

    private @NotNull Stream<JComponent> fields(Filter<JComponent> filter) {
        return this.fields.stream().filter(c -> filter.accepts(c));
    }


    @Nullable
    private JComponent unwrapComponent(Field field, DBNForm form) {
        Class<?> fieldType = field.getType();
        if (!JComponent.class.isAssignableFrom(fieldType)) return null;

        field.setAccessible(true);
        JComponent component = warned(null, () -> cast(field.get(form)));

        if (component instanceof JTextComponent) return component;
        if (component instanceof JComboBox) return component;
        if (component instanceof JCheckBox) return component;
        if (component instanceof JLabel) return component;
        if (component instanceof TextFieldWithBrowseButton) return component;
        // TODO....

        return null;
    }

    private static void captureFieldValue(JComponent component) {
        Object value = getFieldValue(component);
        if (value == null) return;
        if (value.toString().isEmpty()) return;
        if (value.toString().isBlank()) return;

        ClientProperty.CACHED_VALUE.set(component, value);
    }

    private static void restoreFieldValue(JComponent component) {
        Object value = ClientProperty.CACHED_VALUE.get(component);
        setFieldValue(component, value);
    }

    private static @Nullable Object getFieldValue(JComponent component) {
        if (component instanceof TextFieldWithBrowseButton) {
            TextFieldWithBrowseButton textComponent = (TextFieldWithBrowseButton) component;
            return textComponent.getText();

        } else if (component instanceof JTextComponent) {
            JTextComponent textComponent = (JTextComponent) component;
            return textComponent.getText();

        } else if (component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            return comboBox.getSelectedItem();

        } else  if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            return checkBox.isSelected();

        }
        // TODO...
        return null;
    }

    private static void setFieldValue(JComponent component, Object value) {
        if (component instanceof TextFieldWithBrowseButton) {
            TextFieldWithBrowseButton textComponent = (TextFieldWithBrowseButton) component;
            String stringValue = value == null ? "" : value.toString();
            textComponent.setText(stringValue);

        } else if (component instanceof JTextComponent) {
            JTextComponent textComponent = (JTextComponent) component;
            String stringValue = value == null ? "" : value.toString();
            textComponent.setText(stringValue);

        } else if (component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            comboBox.setSelectedItem(value);

        } else if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            boolean booleanValue = value  != null && (Boolean) value;
            checkBox.setSelected(booleanValue);
        }

        // TODO...
    }

}
