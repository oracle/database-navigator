package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.file.VirtualFileListForm;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class FileSystemSourceForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JPanel fileListPanel;
  private JCheckBox storetableCheckbox;
  private VirtualFileListForm fileListForm;

//  private FileSystemSourceConfig fileSystemSourceConfig;
  public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.multipleFiles().
          withTitle("Select Text Files to Embed").
          withDescription("Select valid text files to embed");

  public FileSystemSourceForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    fileListForm = new VirtualFileListForm(this, "Source files");
    fileListPanel.add(fileListForm.getComponent());
  }

  @Override
  protected void initValidation() {
    addValidation(fileListForm.getFileList(), l -> l.getModel().getSize() > 0, "Please select at least one file");
  }

  @Override
  public void resetFormChanges() {
    FileSystemSourceConfig config = getConfig();
    fileListForm.setFiles(config.getFiles());
    storetableCheckbox.setSelected(config.isStore());
  }

  @Override
  public void applyFormChanges() {
    FileSystemSourceConfig config = getConfig();
    config.setFilePaths(fileListForm.getFilePaths());
    config.setStore(storetableCheckbox.isSelected());
  }

  private FileSystemSourceConfig getConfig() {
    return getEmbeddingRequest().getSourceConfig().getFileSourceConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
