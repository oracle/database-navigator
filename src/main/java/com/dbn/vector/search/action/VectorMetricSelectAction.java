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

package com.dbn.vector.search.action;

import com.dbn.common.action.SelectDropdownAction;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.List;

public class VectorMetricSelectAction extends SelectDropdownAction<DBVectorDistanceMetric> implements VectorActionSupport{
    @Override
    protected List<DBVectorDistanceMetric> getObjects(DataContext dataContext) {
        return List.of(
                DBVectorDistanceMetric.COSINE,
                DBVectorDistanceMetric.DOT,
                DBVectorDistanceMetric.EUCLIDEAN,
                DBVectorDistanceMetric.MANHATTAN,
                DBVectorDistanceMetric.HAMMING);
    }

    @Override
    protected String getDescription(AnActionEvent e) {
        return "Vector distance metric";
    }

    @Override
    protected DBVectorDistanceMetric getSelectedObject(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return DBVectorDistanceMetric.COSINE;

        return console.getSelectedMetric();
    }

    @Override
    protected void setSelectedObject(AnActionEvent e, DBVectorDistanceMetric metric) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return;

        console.setSelectedMetric(metric);
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return false;

        if (console.isSearching()) return false;

        return true;
    }
}
