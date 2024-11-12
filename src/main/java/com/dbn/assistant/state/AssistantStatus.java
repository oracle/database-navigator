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

package com.dbn.assistant.state;

import com.dbn.common.property.Property;

/**
 * Transient status of the Chat Box
 *
 * @author Dan Cioca (Oracle)
 */
public enum AssistantStatus implements Property.IntBase {
    INITIALIZING, // the profiles and models are being loaded
    UNAVAILABLE,  // the chat-box is unavailable due to connectivity, privilege issues or alike
    QUERYING,     // the chat-box is waiting for response from backend
    ;

    public static final AssistantStatus[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);

    @Override
    public IntMasks masks() {
        return masks;
    }

}
