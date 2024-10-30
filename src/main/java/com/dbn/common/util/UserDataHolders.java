/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

/**
 * Utility class for {@link UserDataHolder} routines
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class UserDataHolders {

    /**
     * Ensures the given {@link UserDataHolder} has the data represented by the given {@link Key}
     * @param holder the data holder to source data from
     * @param key the key to source data for
     * @param supplier the data producer, used in case the data is not yet present in the holder
     * @return the data behind the given key
     * @param <T> the type of the data in question
     */
    public static <T> T ensure(UserDataHolder holder, Key<T> key, Supplier<T> supplier) {
        T userData = holder.getUserData(key);
        if (userData == null) {
            synchronized (holder) {
                userData = holder.getUserData(key);
                if (userData == null) {
                    userData = supplier.get();
                    holder.putUserData(key, userData);
                }
            }
        }
        return userData;
    }
}
