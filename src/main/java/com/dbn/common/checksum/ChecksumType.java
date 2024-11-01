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

package com.dbn.common.checksum;

import com.intellij.util.io.DigestUtil;

import java.security.MessageDigest;
import java.util.function.Supplier;

public enum ChecksumType {
    MD_5(() -> DigestUtil.md5()),
    SHA_1(() -> DigestUtil.sha1()),
    SHA_256(() -> DigestUtil.sha256()),
    //...
    ;

    private final Supplier<MessageDigest> messageDigest;

    ChecksumType(Supplier<MessageDigest> messageDigest) {
        this.messageDigest = messageDigest;
    }

    public MessageDigest getMessageDigest() {
        return messageDigest.get();
    }
}
