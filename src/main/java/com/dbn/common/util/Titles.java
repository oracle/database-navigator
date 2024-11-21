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

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.connection.session.DatabaseSession;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class Titles {

    public static final String PRODUCT_NAME = "DB Navigator";
    public static final String TITLE_PREFIX = PRODUCT_NAME + " - ";

    public static String signed(String title) {
        return TITLE_PREFIX + title;
    }

    public static String suffixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = databaseContext.getConnection();
        if (connection == null) return title;

        title = title + " - " + connection.getName();

        DatabaseSession session = databaseContext.getSession();
        if (session == null) return title;

        return title + " (" + session + ")";
    }

    public static String prefixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = databaseContext.getConnection();
        if (connection == null) return title;

        return connection.getName()  + " - " + title;
    }
}
