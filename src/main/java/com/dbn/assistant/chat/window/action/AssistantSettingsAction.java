package com.dbn.assistant.chat.window.action;

import com.dbn.options.ConfigId;
import com.dbn.options.action.ProjectSettingsOpenAction;

public class AssistantSettingsAction extends ProjectSettingsOpenAction {
    public AssistantSettingsAction() {
        super(ConfigId.ASSISTANT, false);
    }
}
