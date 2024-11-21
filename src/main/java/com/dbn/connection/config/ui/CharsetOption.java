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
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CharsetOption implements Presentable {
    public static List<CharsetOption> ALL = new ArrayList<>();
    static {
        for (Charset charset : Charset.availableCharsets().values()){
            ALL.add(new CharsetOption(charset));
        }
    }

    private final Charset charset;
    public CharsetOption(Charset charset) {
        this.charset = charset;
    }

    @NotNull
    @Override
    public String getName() {
        return charset.name();
    }


    public static CharsetOption get(Charset charset) {
        for (CharsetOption charsetOption : ALL) {
            if (charsetOption.charset.equals(charset)) {
                return charsetOption;
            }
        }
        return null;
    }

    public Charset getCharset() {
        return charset;
    }
}
