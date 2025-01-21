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

import com.dbn.common.util.Unsafe;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NonNls;

/**
 * Use this "constant" if the possible values are variable (i.e. cannot be implemented with enum).
 */
@EqualsAndHashCode
public abstract class PseudoConstant<T extends PseudoConstant<T>> implements Constant<T> {

    private final String id;
    private final transient int ordinal;

    protected PseudoConstant(String id) {
        if (id == null) {
            // initialization phase (trigger class load - static definitions)
            this.id = null;
            this.ordinal = 0;
        } else {
            this.id = id.intern();
            this.ordinal = PseudoConstantRegistry.register(Unsafe.cast(this));
        }
    }

    @Override
    public final String id() {
        return id;
    }

	@Override
	public int ordinal() {
		return ordinal;
	}

	@Override
	public final String toString() {
		return id();
	}

    public static <T extends PseudoConstant<T>> T get(Class<T> clazz, @NonNls String id) {
        return PseudoConstantRegistry.get(clazz, id);
    }

    public static <T extends PseudoConstant<T>> T[] values(Class<T> clazz) {
        return PseudoConstantRegistry.values(clazz);
    }

    public static <T extends PseudoConstant<T>> T[] list(Class<T> clazz, @NonNls String ids) {
        return PseudoConstantRegistry.list(clazz, ids);
    }

}
