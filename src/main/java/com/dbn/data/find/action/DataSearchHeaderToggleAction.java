package com.dbn.data.find.action;

import com.dbn.common.action.ToggleAction;
import com.dbn.data.find.DataSearchComponent;
import com.intellij.find.FindModel;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import lombok.Getter;

import javax.swing.*;

@Getter
public abstract class DataSearchHeaderToggleAction extends ToggleAction implements DumbAware {
    private final DataSearchComponent searchComponent;

    protected DataSearchHeaderToggleAction(DataSearchComponent searchComponent, String text, Icon icon, Icon hoveredIcon, Icon selectedIcon) {
        super(text);
        this.searchComponent = searchComponent;
        Presentation templatePresentation = getTemplatePresentation();
        templatePresentation.setIcon(icon);
        templatePresentation.setHoveredIcon(hoveredIcon);
        templatePresentation.setSelectedIcon(selectedIcon);
    }

    protected FindModel getFindModel() {
        return getSearchComponent().getFindModel();
    }
}
