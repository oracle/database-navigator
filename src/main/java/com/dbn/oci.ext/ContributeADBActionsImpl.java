package com.dbn.oci.ext;

import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportService;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.wizard.WizardDialog;
import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.UIModelContext;
import com.oracle.oci.intellij.api.oci.OCIDatabase;
import com.oracle.oci.intellij.api.oci.OCIModelObject;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class ContributeADBActionsImpl implements ContributeADBActions {



  private @NotNull PluginDescriptor pluginDescriptor;

  @Override
  public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  public List<ExtensionContextAction> getModelContextActions(final UIModelContext context) {
    List<ExtensionContextAction>  actions = new ArrayList<>();
    OCIModelObject contextObject = context.getContextObject();
    addActions((OCIDatabase) contextObject,actions);
    return actions;
  }

  private void addActions(OCIDatabase obj, List<ExtensionContextAction> actions) {
    actions.add(new ExtensionContextAction(obj.getDisplayName()) {

      @Override
      public void actionPerformed(ActionEvent e) {
        DatabaseType databaseType = DatabaseType.ORACLE;
        ConnectionConfigType configType = ConnectionConfigType.CUSTOM;
        @NotNull
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        final Project[] selected = new Project[1];
        selected[0] = openProjects[0];
        WizardModel model = new WizardModel("Launch Express Settings Wizard");
        WizardStep wizardStep = new ExpressConnectionStartStep(obj.getDisplayName(), selected, openProjects);
        model.add(wizardStep);
        WizardDialog dialog =
                new WizardDialog(true, model);
        boolean wasOk = dialog.showAndGet();


        if (wasOk) {
          // verify if user wants to use wallet or tokenAuth
          ExpressConnectionStartStep wizardStep1 = (ExpressConnectionStartStep) wizardStep;
          String walletLocation = wizardStep1.getWalletLocation();

          if (wizardStep1.useWallet()){
            obj.generateWallet("ALL","NxS3uO7#",walletLocation);
          }
          ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(selected[0]);
          TnsImportService importService = TnsImportService.getInstance();
          File tnsNamesFile = new File(walletLocation+"/tnsnames.ora");
          TnsNames tnsNames = null;
          try {
             tnsNames = TnsNamesParser.get(tnsNamesFile);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
          tnsNames.getProfiles().get(0).setSelected(true);
          TnsImportData tnsImportData = new TnsImportData();
          tnsImportData.setImportType(TnsImportType.PROFILE);
          tnsImportData.setTnsNames(tnsNames);
          tnsImportData.setSelectedOnly(true);
          settingsManager.createConnections(tnsImportData);

//          importService.importTnsNamesFromoci(selected[0],d -> settingsManager.createConnections(d),walletLocation+"/tnsnames.ora");
//          settingsManager.createConnection(databaseType, configType);
        }
      }
    });
  }
  public static abstract class ObjectHolder<T> {
    T obj;

    public ObjectHolder(T obj) {
      this.obj = obj;
    }

    @Override
    public abstract String toString();
  }

  private static final class ExpressConnectionStartStep extends WizardStep<WizardModel> {
    private final Project[] selected;
    private final @NotNull Project[] openProjects;
    Checkbox useWalletCheckBox = new Checkbox();
    JPanel walletLocationPanel = new JPanel(new BorderLayout());
    JLabel walletLocationLabel = new JLabel("Select Wallet Download  Location");
    TextFieldWithBrowseButton walletLocationTextField = new TextFieldWithBrowseButton();
    Checkbox useTokenAuthenticationCheckBox = new Checkbox();
    private ExpressConnectionStartStep( String title, Project[] selected,
                                       @NotNull Project[] openProjects) {
      super(title);
      this.selected = selected;
      this.openProjects = openProjects;
      useWalletCheckBox.addItemListener(e -> {
        walletLocationPanel.setVisible(useWalletCheckBox.getState());
      });
      walletLocationPanel.setVisible(false);
      FileChooserDescriptor walletFolderChooserDesc = new FileChooserDescriptor(false, true, false, false, false, false);

      walletLocationTextField.addBrowseFolderListener(
              txt("cfg.connection.title.SelectWalletDirectory"),
              txt("cfg.connection.text.ValidTnsNamesFolder"),
              null,walletFolderChooserDesc
      );
      walletLocationPanel.add(walletLocationLabel, BorderLayout.WEST);
      walletLocationPanel.add(walletLocationTextField, BorderLayout.CENTER);
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
      JPanel rootComponent = new JPanel();
      rootComponent.setLayout(new GridLayout(3, 2));

      rootComponent.add(new JLabel("Project:"));
      JComboBox<ObjectHolder<Project>> projectCombo = new JComboBox<>();

      Arrays.asList(openProjects)
              .stream().forEach(val ->
                      projectCombo.addItem(new ObjectHolder<Project>(val) {
                        @Override
                        public String toString() {
                          return this.obj.getName();
                        }
                      }));
      projectCombo.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selected[0] = (Project) projectCombo.getSelectedItem();
        }
      });

      rootComponent.add(projectCombo);

      rootComponent.add(new JLabel("Download new wallet"));
      rootComponent.add(useWalletCheckBox);
      rootComponent.add(walletLocationPanel);
      rootComponent.add(new JLabel("Use OCI token authentication"));
      rootComponent.add(useTokenAuthenticationCheckBox);
      return rootComponent;
    }

    public boolean useWallet() {
      return useWalletCheckBox.getState();
    }

    public boolean useTokenAuthentication() {
      return useTokenAuthenticationCheckBox.getState();
    }
    public String getWalletLocation() {
      return walletLocationTextField.getText();
    }
  }
}
