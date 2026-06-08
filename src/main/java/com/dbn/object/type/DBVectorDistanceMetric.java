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

package com.dbn.object.type;

import com.dbn.common.constant.Constant;
import com.dbn.common.constant.Constants;
import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import static com.dbn.nls.NlsResources.txt;

@NonNls
@Getter
public enum DBVectorDistanceMetric implements Constant<DBVectorDistanceMetric>, Presentable {
    COSINE("COSINE", txt("app.objects.hint.DBVectorDistanceMetric_COSINE")),
    DOT ("DOT", txt("app.objects.hint.DBVectorDistanceMetric_DOT")),
    EUCLIDEAN ("EUCLIDEAN", txt("app.objects.hint.DBVectorDistanceMetric_EUCLIDEAN")),
    EUCLIDEAN_SQUARED ("EUCLIDEAN_SQUARED", txt("app.objects.hint.DBVectorDistanceMetric_EUCLIDEAN_SQUARED")),
    HAMMING ("HAMMING", txt("app.objects.hint.DBVectorDistanceMetric_HAMMING")),
    MANHATTAN ("MANHATTAN", txt("app.objects.hint.DBVectorDistanceMetric_MANHATTAN")),
    JACCARD ("JACCARD", txt("app.objects.hint.DBVectorDistanceMetric_JACCARD"))

    ;

    private final String name;
    private final String description;

    DBVectorDistanceMetric(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static DBVectorDistanceMetric get(String id) {
        return Constants.get(values(), id, COSINE);
    }
}
