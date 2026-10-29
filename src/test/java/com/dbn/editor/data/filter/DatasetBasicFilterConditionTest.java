/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.editor.data.filter;

import com.dbn.common.locale.DBDateFormat;
import com.dbn.common.locale.DBNumberFormat;
import com.dbn.common.locale.Formatter;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatasetBasicFilterConditionTest {

    private static final Formatter FORMATTER = new Formatter(
            0, Locale.US, DBDateFormat.MEDIUM, DBNumberFormat.UNGROUPED);

    @Test
    public void rendersNumericLiteral() {
        assertEquals("42.5", DatasetBasicFilterCondition.renderNumericValue("42.5", FORMATTER));
    }

    @Test
    public void rejectsSqlFromNumericFilterValue() {
        assertNull(DatasetBasicFilterCondition.renderNumericValue("0 or 1=1", FORMATTER));
        assertNull(DatasetBasicFilterCondition.renderNumericValue("1) union select", FORMATTER));
    }
}
