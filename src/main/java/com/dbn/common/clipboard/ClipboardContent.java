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

package com.dbn.common.clipboard;

import lombok.extern.slf4j.Slf4j;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import static com.dbn.common.util.Classes.simpleClassName;

@Slf4j
public abstract class ClipboardContent implements Transferable {
    private final DataFlavor[] dataFlavors;
    private final String content;

    public ClipboardContent(String content) {
        this.content = content;
        this.dataFlavors = createDataFlavorsGuarded();
    }

    protected DataFlavor[] createDataFlavorsGuarded() {
        try {
            return createDataFlavors();
        } catch (Throwable e) {
            log.warn("Failed to initialise data flavors for {}. Returning string flavor", simpleClassName(this), e);
            DataFlavor[] dataFlavors = {DataFlavor.stringFlavor};
            return dataFlavors;
        }
    }

    protected abstract DataFlavor[] createDataFlavors() throws Exception;

    @Override
    public final DataFlavor[] getTransferDataFlavors() {
        return dataFlavors;
    }

    @Override
    public final Object getTransferData(DataFlavor flavor){
        return content;
    }
}
