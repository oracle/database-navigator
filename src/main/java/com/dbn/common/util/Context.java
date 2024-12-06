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

package com.dbn.common.util;

import com.dbn.common.ui.form.DBNForm;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;

import static com.dbn.common.util.Classes.className;
import static com.dbn.common.util.Unsafe.cast;

@UtilityClass
public class Context {

    public static DataContext getDataContext(Object source) {
        if (source instanceof DataContext) return (DataContext) source;
        if (source instanceof DBNForm) return getDataContext((DBNForm) source);
        if (source instanceof Editor) return getDataContext((Editor) source);
        if (source instanceof FileEditor) return getDataContext((FileEditor) source);
        if (source instanceof Component) return getDataContext((Component) source);

        throw new UnsupportedOperationException("Unsupported source for data context lookup: " + className(source));
    }

    public static DataContext getDataContext(DBNForm form) {
        return getDataContext(form.getComponent());
    }

    public static DataContext getDataContext(Editor editor) {
        return getDataContext(editor.getComponent());
    }

    public static DataContext getDataContext(FileEditor editor) {
        return getDataContext(editor.getComponent());
    }

    public static DataContext getDataContext(Component component) {
        return DataManager.getInstance().getDataContext(component);
    }

    @Nullable
    public static <T> T getData(Component component, String dataId) {
        DataContext dataContext = getDataContext(component);
        return cast(dataContext.getData(dataId));
    }

    @Nullable
    public static <T> T getData(FileEditor component, String dataId) {
        DataContext dataContext = getDataContext(component);
        return cast(dataContext.getData(dataId));
    }

}
