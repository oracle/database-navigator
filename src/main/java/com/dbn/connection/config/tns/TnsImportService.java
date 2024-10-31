package com.dbn.connection.config.tns;

import com.dbn.connection.config.tns.ui.TnsNamesImportDialog;
import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.connection.config.tns.TnsImportService.COMPONENT_NAME;

@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class TnsImportService extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.TnsImportService";
    private TnsImportType importType = TnsImportType.FIELDS;

    private TnsImportService() {
        super(COMPONENT_NAME);
    }

    public static TnsImportService getInstance() {
        return applicationService(TnsImportService.class);
    }


    public void importTnsNames(Project project, Consumer<TnsImportData> consumer) {
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(TnsNamesParser.FILE_CHOOSER_DESCRIPTOR, project, null);
        if (virtualFiles.length != 1) return;

        File file = new File(virtualFiles[0].getPath());
        Dialogs.show(() -> new TnsNamesImportDialog(project, file), (dialog, exitCode) -> {
            if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
            consumer.accept(dialog.getImportData());
        });
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element optionsElement = newElement(element, "tns-import-options");

        setEnum(optionsElement, "import-type", importType);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element optionsElement = element.getChild("tns-import-options");
        if (optionsElement == null) return;

        importType = getEnum(optionsElement, "import-type", importType);
    }

    public void importTnsNamesFromoci(Project project, Consumer<TnsImportData> consumer,String path) {
        File file = new File(path);
        Dialogs.show(() -> new TnsNamesImportDialog(project, file), (dialog, exitCode) -> {
            if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
            consumer.accept(dialog.getImportData());
        });
    }
}
