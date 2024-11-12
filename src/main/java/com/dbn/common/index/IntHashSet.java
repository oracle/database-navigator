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

package com.dbn.common.index;

import java.util.Arrays;

/**
 * copy of java.util.regex.IntHashSet
 */
public class IntHashSet {
    private int[] entries = new int[32];
    private int[] hashes = new int[9];
    private int pos = 0;

    public IntHashSet() {
        Arrays.fill(this.entries, -1);
        Arrays.fill(this.hashes, -1);
    }

    public boolean contains(int i) {
        for(int h = this.hashes[i % this.hashes.length]; h != -1; h = this.entries[h + 1]) {
            if (this.entries[h] == i) {
                return true;
            }
        }

        return false;
    }

    public void add(int i) {
        int h0 = i % this.hashes.length;
        int next = this.hashes[h0];

        for(int next0 = next; next0 != -1; next0 = this.entries[next0 + 1]) {
            if (this.entries[next0] == i) {
                return;
            }
        }

        this.hashes[h0] = this.pos;
        this.entries[this.pos++] = i;
        this.entries[this.pos++] = next;
        if (this.pos == this.entries.length) {
            this.expand();
        }

    }

    private void expand() {
        int[] old = this.entries;
        int[] es = new int[old.length << 1];
        int hlen = old.length / 2 | 1;
        int[] hs = new int[hlen];
        Arrays.fill(es, -1);
        Arrays.fill(hs, -1);

        int next;
        for(int n = 0; n < this.pos; es[n++] = next) {
            int i = old[n];
            int hsh = i % hlen;
            next = hs[hsh];
            hs[hsh] = n;
            es[n++] = i;
        }

        this.entries = es;
        this.hashes = hs;
    }
}
