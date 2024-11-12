package com.dbn.ddl;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.file.FileTypeService;
import com.dbn.common.thread.Background;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.ddl.options.DDLFileExtensionSettings;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.notification.NotificationGroup.DDL;
import static com.dbn.common.util.Commons.nvl;
import static com.intellij.lang.Language.findLanguageByID;

@State(
    name = DDLFileManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DDLFileManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DDLFileManager";

    private DDLFileManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, FileTypeManager.TOPIC, fileTypeListener);
    }

    public void registerExtensions(DDLFileExtensionSettings settings) {
        FileTypeService fileTypeService = FileTypeService.getInstance();
        List<DDLFileType> fileTypes = settings.getFileTypes();
        fileTypes.forEach(ft -> ft.getExtensions().forEach(e -> fileTypeService.associateExtension(ft.getLanguageFileType(), e)));;
    }

    public static DDLFileManager getInstance(@NotNull Project project) {
        return projectService(project, DDLFileManager.class);
    }

    private DDLFileExtensionSettings getExtensionSettings() {
        return DDLFileSettings.getInstance(getProject()).getExtensionSettings();
    }

    public DDLFileType getDDLFileType(DDLFileTypeId ddlFileTypeId) {
        return getExtensionSettings().getFileType(ddlFileTypeId);
    }

    DDLFileType getDDLFileTypeForExtension(String extension) {
        return getExtensionSettings().getFileTypeForExtension(extension);
    }

    String createDDLStatement(DBSourceCodeVirtualFile sourceCodeFile, DBContentType contentType) {
        DBSchemaObject object = sourceCodeFile.getObject();
        String content = sourceCodeFile.getOriginalContent().toString().trim();
        if (content.isEmpty()) return "";

        ConnectionHandler connection = object.getConnection();
        String alternativeStatementDelimiter = connection.getSettings().getDetailSettings().getAlternativeStatementDelimiter();
        DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
        return dataDefinition.createDDLStatement(getProject(),
                object.getObjectType().getTypeId(),
                connection.getUserName(),
                object.getSchema().getName(),
                object.getName(),
                contentType,
                content,
                alternativeStatementDelimiter);
    }


    @Nullable
    public DDLFileType getDDLFileType(DBObjectType objectType, DBContentType contentType) {
        DDLFileTypeId ddlFileTypeId = objectType.getDdlFileTypeId(contentType);
        return ddlFileTypeId == null ? null : getDDLFileType(ddlFileTypeId);
    }

    @NotNull
    List<DDLFileType> getDDLFileTypes(DBObjectType objectType) {
        Collection<DDLFileTypeId> typeIds = objectType.getDdlFileTypeIds();
        if (typeIds == null) return Collections.emptyList();

        return typeIds.stream().map(id -> getDDLFileType(id)).collect(Collectors.toList());
    }


    /***************************************
     *            FileTypeListener         *
     ***************************************/

    private final FileTypeListener fileTypeListener = new FileTypeListener() {
        @Override
        public void fileTypesChanged(@NotNull FileTypeEvent event) {
            FileTypeService fileTypeService = FileTypeService.getInstance();
            if (fileTypeService.isSilentFileTypeChange()) return;

            List<DDLFileType> ddlFileTypeList = getExtensionSettings().getFileTypes();
            for (DDLFileType ddlFileType : ddlFileTypeList) {
                DBLanguageFileType fileType = ddlFileType.getLanguageFileType();
                List<FileNameMatcher> associations = fileTypeService.getAssociations(fileType);
                List<String> registeredExtension = new ArrayList<>();
                for (FileNameMatcher association : associations) {
                    if (association instanceof ExtensionFileNameMatcher) {
                        ExtensionFileNameMatcher extensionMatcher = (ExtensionFileNameMatcher) association;
                        registeredExtension.add(extensionMatcher.getExtension());
                    }
                }

                StringBuilder restoredAssociations = new StringBuilder();
                for (String extension : ddlFileType.getExtensions()) {
                    if (!registeredExtension.contains(extension)) {
                        fileTypeService.associateExtension(fileType, extension);
                        if (restoredAssociations.length() > 0) {
                            restoredAssociations.append(", ");
                        }
                        restoredAssociations.append(extension);

                    }
                }

                if (restoredAssociations.length() > 0) {
                    sendInfoNotification(DDL, txt("ntf.ddlFiles.info.FileAssociationsRestored",restoredAssociations, getProject().getName()));
                }
            }

        }
    };

    @Nullable
    public LanguageFileType resolveFileType(DBObjectType objectType, DBContentType contentType) {
        if (objectType == DBObjectType.JAVA_OBJECT) {
            // java module may not be present in the IDE (if not IntelliJ)
            // (fallback to plain text)
            Language language = findLanguageByID("JAVA");
            return nvl(language, PlainTextLanguage.INSTANCE).getAssociatedFileType();
        } else {
            DDLFileType ddlFileType = getDDLFileType(objectType, contentType);
            return ddlFileType == null ? null : ddlFileType.getLanguageFileType();
        }
    }


    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }

    @Override
    public void initializeComponent() {
        Background.run(getProject(), () -> registerExtensions(getExtensionSettings()));
    }
}
