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

import com.dbn.common.filter.CompositeFilter;
import com.dbn.common.filter.Filter;
import com.dbn.common.util.Commons;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Utility class exposing various {@link Filter<JComponent>} factory methods
 *
 * @author Dan Cioca (Oracle)
 */

@UtilityClass
public class JComponentFilter {

    /**
     * Returns a filter that accepts only accessible {@link JComponent} objects
     * ("accessible" is understood as visible and enabled)
     * @return a {@link Filter} object
     */
    public static Filter<JComponent> accessible() {
        return c -> c.isVisible() && c.isEnabled();
    }

    /**
     * Returns a filter that accepts only inaccessible {@link JComponent} objects
     * ("inaccessible" is understood as hidden or disabled)
     * @return a {@link Filter} object
     */
    public static Filter<JComponent> inaccessible() {
        return c -> !c.isVisible() || !c.isEnabled();
    }

    /**
     * Returns a filter that accepts only accessible {@link JComponent} objects classified as the given category
     * @param category the {@link JComponentCategory} to filter by
     * @return a {@link Filter} object
     */
    public static Filter<JComponent> accessibleClassifiedAs(JComponentCategory category) {
        return CompositeFilter.from(accessible(), classifiedAs(category));
    }

    /**
     * Returns a filter that accepts {@link JComponent} objects classified as the given category
     * @param category the {@link JComponentCategory} to filter by
     * @return a {@link Filter} object
     */
    public static Filter<JComponent> classifiedAs(JComponentCategory category) {
        return c -> category.classifies(c);
    }

    /**
     * Returns a filter that accepts only the components specified in the given varargs parameter
     * @param components the array of components ot be accepted by the filter
     * @return a {@link Filter} object
     */
    public static Filter<JComponent> array(@Nullable JComponent... components) {
        return c -> components == null || components.length == 0 || Commons.isOneOf(c, components);
    }

}
