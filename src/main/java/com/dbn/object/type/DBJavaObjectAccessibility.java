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

package com.dbn.object.type;

import com.dbn.common.constant.Constant;
import com.dbn.common.constant.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public enum DBJavaObjectAccessibility implements Constant<DBJavaObjectAccessibility> {
    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"),
    ;

    private final String name;

    @Nullable
    public static DBJavaObjectAccessibility get(String name) {
        // safe lookup - default to null if not known
        return Constants.get(values(), name, null);
    }

}
