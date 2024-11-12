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

package com.dbn.assistant.profile.wizard;

import com.dbn.common.action.BasicAction;
import com.dbn.common.util.Naming;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

import static com.intellij.icons.AllIcons.Diff.GutterCheckBox;
import static com.intellij.icons.AllIcons.Diff.GutterCheckBoxSelected;

public class DatasetTypeToggleAction extends BasicAction {
    private final DBObjectType datasetType;
    private final Supplier<Set<DBObjectType>> selectedDatasetTypes;
    private final Runnable toggleCallback;

    private DatasetTypeToggleAction(DBObjectType datasetType, Supplier<Set<DBObjectType>> selectedDatasetTypes, Runnable toggleCallback) {
        super();
        this.datasetType = datasetType;
        this.selectedDatasetTypes = selectedDatasetTypes;
        this.toggleCallback = toggleCallback;
    }

    public static DatasetTypeToggleAction create(DBObjectType datasetType, Supplier<Set<DBObjectType>> selectedDatasetTypes, Runnable toggleCallback) {
        return new DatasetTypeToggleAction(datasetType, selectedDatasetTypes, toggleCallback);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        String text =
            datasetType == DBObjectType.TABLE ? "Tables" :
            datasetType == DBObjectType.VIEW ? "Views" :
            datasetType == DBObjectType.MATERIALIZED_VIEW ? "Materialized Views" : "";

        String description = "Show " + Naming.capitalizeWords(datasetType.getListName());

        Presentation presentation = e.getPresentation();
        presentation.setText(text);
        presentation.setDescription(description);
        presentation.setIcon(isSelected() ? GutterCheckBoxSelected: GutterCheckBox);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Set<DBObjectType> selections = getSelections();
        if (isSelected())
            selections.remove(datasetType); else
            selections.add(datasetType);

        toggleCallback.run();
    }

    private boolean isSelected() {
        return getSelections().contains(datasetType);
    }

    private Set<DBObjectType> getSelections() {
        return selectedDatasetTypes.get();
    }


    @Override
    public boolean displayTextInToolbar() {
        return true;
    }
}

