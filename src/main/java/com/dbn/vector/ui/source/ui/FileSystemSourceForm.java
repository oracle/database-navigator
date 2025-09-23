package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.file.VirtualFileListForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.common.CreateTableConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class FileSystemSourceForm extends DBNFormBase {
  private JPanel mainPanel;
  private VirtualFileListForm fileListForm;
  private JPanel fileListPanel;
  private JCheckBox storetableCheckbox;
//  private FileSystemSourceConfig fileSystemSourceConfig;
  public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.multipleFiles().
          withTitle("Select Text Files to Embed").
          withDescription("Select valid text files to embed");

  public FileSystemSourceForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    fileListForm= new VirtualFileListForm(this, "Source files");
    fileListPanel.add(fileListForm.getComponent());
  }

  public FileSystemSourceConfig getFileSystemSourceConfig() {
    FileSystemSourceConfig fileSystemSourceConfig = new FileSystemSourceConfig();
    fileSystemSourceConfig.setVirtualFiles(fileListForm.getFileList().getModel().getFiles());
    fileSystemSourceConfig.setToStore(storetableCheckbox.isSelected());
    if (storetableCheckbox.isSelected()){
      CreateTableConfig createTableConfig = new CreateTableConfig();
      fileSystemSourceConfig.setTableConfig(createTableConfig);
    }
    return fileSystemSourceConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
