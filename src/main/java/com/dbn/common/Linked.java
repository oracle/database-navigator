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

package com.dbn.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Linked<T extends Linked<T>> {
    private T previous;
    private T next;
    private int index = -1;

    public Linked(T previous) {
        this.previous = previous;
        if (previous != null) {
            previous.setNext((T) this);
        }
    }

    public int getIndex() {
        if (index == -1) {
            index = previous == null ? 0 : previous.getIndex() + 1;
        }
        return index;
    }

    public boolean isLast() {
        return next == null;
    }

    public boolean isFirst() {
        return previous == null;
    }
}
