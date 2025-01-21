/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.language.common.navigation;

import com.dbn.common.icon.Icons;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class NavigateToSpecificationAction extends NavigationAction{
    public NavigateToSpecificationAction(DBObject parentObject, @NotNull BasePsiElement navigationElement, @NotNull DBObjectType objectType) {
        super(txt("app.codeEditor.action.GoToSpecification",  objectType.getCapitalizedName()), Icons.NAVIGATION_GO_TO_SPEC, parentObject, navigationElement);
    }
}
