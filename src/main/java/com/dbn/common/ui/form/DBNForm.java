/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.ui.form;

import com.dbn.common.action.DataProviderDelegate;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

public interface DBNForm extends DBNComponent, DataProviderDelegate, NlsSupport {

    @Nullable
    default JComponent getPreferredFocusedComponent() {
        return null;
    }

    /**
     * Form validator invoked by the dialog validation logic for preventing invalid inputs from being submitted
     * {@link DBNForm} implementations which require input validation must overwrite this with the appropriate validation logic
     *
     * @param components the components to be validated (if left empty, all components will be validated)
     * @return a list of {@link ValidationInfo} objects representing the validation errors
     */
    List<ValidationInfo> validate(JComponent... components);

    /**
     * Retrieves the parent dialog of the current form, if present.
     *
     * @param <D> the type of the parent dialog, extending from {@link DBNDialog}
     * @return the parent dialog instance, or {@code null} if no parent dialog exists
     */
    @Nullable
    <D extends DBNDialog> D getParentDialog();
}
