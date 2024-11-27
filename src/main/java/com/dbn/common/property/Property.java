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

package com.dbn.common.property;

public interface Property {
    @Deprecated
    default PropertyGroup group() {
        return null;
    }

    default boolean implicit() {
        return false;
    }

    interface LongBase extends Property{
        int ordinal();

        LongMasks masks();

        default long maskOn() {
            return masks().on;
        }

        default long maskOff() {
            return masks().off;
        }

        class LongMasks {
            private final long on;
            private final long off;

            public LongMasks(LongBase property) {
                int shift = property.ordinal();
                assert shift < 63;
                this.on = 1L << shift;
                this.off = ~this.on;
            }
        }
    }

    interface IntBase extends Property{
        int ordinal();

        IntMasks masks();

        default int maskOn() {
            return masks().on;
        }

        default int maskOff() {
            return masks().off;
        }

        class IntMasks {
            private final int on;
            private final int off;

            public IntMasks(IntBase property) {
                int shift = property.ordinal();
                assert shift < 32;
                this.on = 1 << shift;
                this.off = ~this.on;
           }
        }
    }
}
