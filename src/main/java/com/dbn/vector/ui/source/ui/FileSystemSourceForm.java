package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileSystemSourceForm extends DBNFormBase {
  private JPanel mainPanel;
  private TextFieldWithBrowseButton filesField;
  private JList<String> selectedFilesList  ;
  private DBNScrollPane DBNScrollPane1;
  private DefaultListModel<String> filesListModel =  new DefaultListModel<>();
  public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.multipleFiles().
          withTitle("Select Text Files to Embed").
          withDescription("Select valid text files to embed");
//          withFileFilter(virtualFile -> Objects.equals(virtualFile.getExtension(), List.of("pdf","txt","md","json","xml","tex","log") ));

  public FileSystemSourceForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    System.out.println("ffesm");
    selectedFilesList.setModel(filesListModel);
    filesField.addBrowseFolderListener(
            getProject(),
            FILE_CHOOSER_DESCRIPTOR
    );

    filesField.addActionListener(e -> {
      FileChooser.chooseFiles(FILE_CHOOSER_DESCRIPTOR, getProject(), /* parent= */ null,
              (List<VirtualFile> selected) -> {
                // clear old entries
                filesListModel.clear();
                // set the text field to a “;”-delimited list of paths
                String paths = selected.stream()
                        .map(VirtualFile::getPath)
                        .collect(Collectors.joining(";"));
                filesField.setText(paths);

                // populate JList
                for (VirtualFile vf : selected) {
                  filesListModel.addElement(vf.getCanonicalFile().getName());
                }
              });
    });

  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
