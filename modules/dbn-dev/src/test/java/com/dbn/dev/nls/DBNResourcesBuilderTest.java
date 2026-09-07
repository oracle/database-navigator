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

package com.dbn.dev.nls;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DBNResourcesBuilderTest {
    @Test
    public void rebuildsUsingTemplateCategoryOrder() {
        String source = "" +
                "# source header\n" +
                "app.alpha.Z=alpha z\n" +
                "app.beta.Z=beta z\n" +
                "app.beta.A=beta a\\nnext\n" +
                "cfg.connection.Z=connection z\n";

        String template = "" +
                "# template header\n" +
                "[APP_KEYS]\n" +
                "\n" +
                "# another template header\n" +
                "[CFG_KEYS]\n";

        String expected = "" +
                "# template header\n" +
                "app.beta.A=beta a\\nnext\n" +
                "app.beta.Z=beta z\n" +
                "app.alpha.Z=alpha z\n" +
                "\n" +
                "# another template header\n" +
                "cfg.connection.Z=connection z\n";

        assertEquals(expected, DBNResourcesBuilder.rebuild(source, template));
    }
}
