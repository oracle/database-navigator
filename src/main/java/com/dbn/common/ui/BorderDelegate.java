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

package com.dbn.common.ui;

import com.dbn.common.ui.util.Borders;
import lombok.experimental.Delegate;

import javax.swing.border.Border;
import java.util.function.Supplier;


/**
 * A delegate implementation of the {@link Border} interface that allows delegating the actual
 * border behavior to a dynamically provided {@link Border} instance.
 *
 * This class is particularly useful when the border instance can change dynamically or needs
 * to be lazily resolved at runtime.
 *
 * The delegate is supplied through a functional {@link Supplier}, and if the supplier provides
 * a null border, a default empty border is used instead.
 *
 * @author Dan Cioca (Oracle)
 */
public class BorderDelegate implements Border {
    private final Supplier<Border> delegate;

    public BorderDelegate(Supplier<Border> delegate) {
        this.delegate = delegate;
    }

    public static Border delegate(Supplier<Border> delegate) {
        return new BorderDelegate(delegate);
    }

    @Delegate
    public Border getDelegate() {
        Border border = delegate.get();
        return border == null ? Borders.EMPTY_BORDER : border;
    }
}
