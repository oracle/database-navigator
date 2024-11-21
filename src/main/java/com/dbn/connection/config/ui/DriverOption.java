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

package com.dbn.connection.config.ui;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.Driver;
import java.util.List;
import java.util.Objects;

@Getter
public class DriverOption implements Presentable {
    private final Class<Driver> driver;

    public DriverOption(Class<Driver> driver) {
        this.driver = driver;
    }

    @NotNull
    @Override
    public String getName() {
        return driver.getName();
    }

    public static DriverOption get(List<DriverOption> driverOptions, String name) {
        for (DriverOption driverOption : driverOptions) {
            if (Objects.equals(driverOption.getName(), name)) {
                return driverOption;
            }
        }
        return null;
    }
}
