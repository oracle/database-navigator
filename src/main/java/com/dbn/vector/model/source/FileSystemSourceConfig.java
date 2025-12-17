package com.dbn.vector.model.source;

import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static java.util.Collections.emptyList;

@Setter
@Getter
public class FileSystemSourceConfig implements PersistentStateElement {
    private List<String> filePaths = new ArrayList<>();

    public List<VirtualFile> getFiles() {
        if (filePaths == null) return emptyList();
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        return filePaths
                .stream()
                .map(p -> fileManager.findFileByNioPath(Path.of(p)))
                .filter(f -> f != null)
                .collect(Collectors.toList());
    }

    @Override
    public void readState(Element element) {
        Element filesElement = element.getChild("files");
        List<Element> fileElements = childrenOf(filesElement, "file");
        for (Element fileElement : fileElements) {
            String path = stringAttribute(fileElement, "path");
            filePaths.add(path);
        }
    }

    @Override
    public void writeState(Element element) {
        Element filesElement = newElement(element, "files");
        for (String filePath : filePaths) {
            Element fileElement = newElement(filesElement, "file");
            setStringAttribute(fileElement, "path", filePath);
        }
    }

    public int getFileCount() {
        return filePaths.size();
    }
}
