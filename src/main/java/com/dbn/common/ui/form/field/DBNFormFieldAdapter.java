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
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Capability extension adapter for {@link DBNForm} components, providing various bulk activities on the {@link JComponent} elements declared in the form.
 * Useful to control field dynamics like toggling visibility and accessibility of fields within a given form
 * <p/>
 * <li>classify fields by {@link JComponentCategory}</li>
 * <li>capture and restore field values</li>
 * <li>change field visibility and availability</li>
 * <li>generically amend field attributes</li>
 *
 * @author Dan Cioca (Oracle)
 */
public interface DBNFormFieldAdapter {

    /**
     * Classifies {@link JComponent} fields by a given {@link JComponentCategory}
     * @param category the {@link JComponentCategory} to be applied to the fields
     * @param filter the {@link Filter} to be used when considering fields to be classified
     */
    void classifyFields(JComponentCategory category, Filter<JComponent> filter);

    /**
     * Captures the current field values in a hidden cache
     * @param filter the {@link Filter} to be used when considering fields to be cached
     */
    void captureFieldValues(Filter<JComponent> filter);

    /**
     * Resets the values {@link JComponent} fields for which the given filter is applicable
     * @param filter the {@link Filter} to be used when considering fields to be reset
     */
    void resetFieldValues(Filter<JComponent> filter);

    /**
     * Restores field values from the hidden cache
     * @param filter the {@link Filter} to be used when considering fields to be restored
     */
    void restoreFieldValues(Filter<JComponent> filter);

    /**
     * Updates fields returned by the given filter by applying the consumer function on them
     * @param filter the {@link Filter} to be used when considering fields to be updated
     * @param consumer the component update logic
     */
    void updateFields(Filter<JComponent> filter, Consumer<JComponent> consumer);

    /**
     * Initializes the visibility condition for the fields represented by the given filter
     * Upon invocation of {@link #updateFieldsVisibility()}, the field visibility will be adjusted based on the given condition
     * @param condition the condition returning the visibility flag
     * @param filter the {@link Filter} to be used when considering fields to adjusted
     */
    void initFieldsVisibility(Condition condition, Filter<JComponent> filter);

    /**
     * Initializes the accessibility condition for the fields represented by the given filter
     * Upon invocation of {@link #updateFieldsAccessibility()}, the field accessibility will be adjusted based on the given condition
     * @param condition the condition returning the accessibility flag
     * @param filter the {@link Filter} to be used when considering fields to adjusted
     */
    void initFieldsAccessibility(Condition condition, Filter<JComponent> filter);

    /**
     * Updates field visibility based on the conditions initialised
     * with {@link #initFieldsVisibility(Condition, Filter)}
     */
    void updateFieldsVisibility();

    /**
     * Updates field accessibility based on the conditions initialised
     * with {@link #initFieldsAccessibility(Condition, Filter)}
     */
    void updateFieldsAccessibility();

    /**
     * Default factory method for {@link DBNFormFieldAdapter}
     * @param form the {@link DBNForm} to be extended
     * @return a new {@link DBNFormFieldAdapter} implementation for the given form
     */
    static DBNFormFieldAdapter create(@NotNull DBNForm form) {
        return new DBNFormFieldAdapterImpl(form);
    }

}
