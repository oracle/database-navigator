package com.dbn.vector.model.sourceconfig;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.vector.model.common.CreateTableConfig;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static java.util.Collections.emptyList;

@Setter
@Getter
public class FileSystemSourceConfig implements PersistentStateElement {
    private List<String> filePaths = new ArrayList<>();

    private boolean store;
    // if it's to be stored
    private CreateTableConfig tableConfig;

    public List<VirtualFile> getFiles() {
        if (filePaths == null) return emptyList();
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        return filePaths
                .stream()
                .map(p -> fileManager.findFileByNioPath(Path.of(p)))
                .filter(f -> f != null)
                .toList();
    }

    @Override
    public void readState(Element element) {
        Element filesElement = element.getChild("files");
        List<Element> fileElements = childrenOf(filesElement, "file");
        for (Element fileElement : fileElements) {
            String path = stringAttribute(fileElement, "path");
            filePaths.add(path);
        }

        store = booleanAttribute(element, "store", store);
    }

    @Override
    public void writeState(Element element) {
        Element filesElement = newElement(element, "files");
        for (String filePath : filePaths) {
            Element fileElement = newElement(filesElement, "file");
            setStringAttribute(fileElement, "path", filePath);
        }
        setBooleanAttribute(element, "store", store);
    }

    public int getFileCount() {
        return filePaths.size();
    }
}
