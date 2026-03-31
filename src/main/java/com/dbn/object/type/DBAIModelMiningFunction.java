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

import com.dbn.common.constant.PseudoConstant;
import org.jetbrains.annotations.NonNls;

@SuppressWarnings("unused")
public class DBAIModelMiningFunction extends PseudoConstant<DBAIModelMiningFunction> {
    public static final DBAIModelMiningFunction CLASSIFICATION = get("CLASSIFICATION");
    public static final DBAIModelMiningFunction REGRESSION = get("REGRESSION");
    public static final DBAIModelMiningFunction CLUSTERING = get("CLUSTERING");
    public static final DBAIModelMiningFunction FEATURE_EXTRACTION = get("FEATURE_EXTRACTION");
    public static final DBAIModelMiningFunction ASSOCIATION_RULES = get("ASSOCIATION_RULES");
    public static final DBAIModelMiningFunction ATTRIBUTE_IMPORTANCE = get("ATTRIBUTE_IMPORTANCE");
    ;

    protected DBAIModelMiningFunction(@NonNls String id) {
        super(id);
    }

    public static DBAIModelMiningFunction get(@NonNls String id) {
        return PseudoConstant.get(DBAIModelMiningFunction.class, id);
    }
}
