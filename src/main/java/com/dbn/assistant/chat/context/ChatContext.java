/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.assistant.chat.context;

import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.common.state.PersistentStateElement;

public interface ChatContext extends PersistentStateElement {

    AIProvider getProvider();

    AIModel getModel();

    Object getAction();


    String getProviderId();

    String getProfileName();

    String getModelId();

    String getActionId();



    void setProviderId(String providerId);

    void setProfileName(String profileName);

    void setModelId(String modelId);

    void setActionId(String actionId);

    void setInteractive(boolean interactive);



    boolean isInteractive();

    boolean isModelSwitch(ChatContext that);

    boolean isProviderSwitch(ChatContext that);

    boolean isProfileSwitch(ChatContext that);

    boolean isActionSwitch(ChatContext that);
}
