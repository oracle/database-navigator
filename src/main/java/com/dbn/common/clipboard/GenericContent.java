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
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Generic {@link Transferable} implementation,
 * holding a type-unspecified {@link Serializable} against a custom {@link DataFlavor}
 *
 * @author Dan Cioca (Oracle)
 */
@Slf4j
public final class GenericContent implements Transferable {
    private final DataFlavor[] dataFlavors;
    private final Serializable content;

    public GenericContent(Serializable content, DataFlavor dataFlavor) {
        this.content = content;
        this.dataFlavors = new DataFlavor[]{dataFlavor};
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return Objects.equals(dataFlavor, dataFlavors[0]);
    }

    @NotNull
    @Override
    public Serializable getTransferData(DataFlavor flavor){
        return content;
    }
}
