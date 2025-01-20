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

package com.dbn.common.constant;

import org.jetbrains.annotations.NonNls;

public class TestCrossRefConstant extends PseudoConstant<TestCrossRefConstant> {
    // various static initialization ways
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_0 = get("CROSS_REF_CONSTANT_0");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_1 = get("CROSS_REF_CONSTANT_1");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_2 = get("CROSS_REF_CONSTANT_2");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_4 = new TestCrossRefConstant("CROSS_REF_CONSTANT_4");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_5 = new TestCrossRefConstant("CROSS_REF_CONSTANT_5");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_6 = get("CROSS_REF_CONSTANT_6");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_7 = new TestCrossRefConstant("CROSS_REF_CONSTANT_7");
    public static final TestCrossRefConstant CROSS_REF_CONSTANT_8 = get("CROSS_REF_CONSTANT_8");

    private TestCrossRefConstant(@NonNls String id) {
        super(id);
    }

    public static TestCrossRefConstant get(@NonNls String id) {
        return PseudoConstant.get(TestCrossRefConstant.class, id);
    }
}
