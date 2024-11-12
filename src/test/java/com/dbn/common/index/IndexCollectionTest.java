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

import org.junit.Assert;
import org.junit.Test;

public class IndexCollectionTest {

    @Test
    public void indexOf() {
        IndexCollection indexCollection = new IndexCollection(2, 5, 8, 12, 16, 23, 38, 56, 72, 91);
        Assert.assertEquals(5, indexCollection.indexOf(23));
        Assert.assertEquals(-1, indexCollection.indexOf(71));
    }

    @Test
    public void add() {
        IndexCollection indexCollection = new IndexCollection();

        indexCollection.add(23);
        Assert.assertEquals(0, indexCollection.indexOf(23));

        indexCollection.add(2);
        Assert.assertEquals(0, indexCollection.indexOf(2));

        indexCollection.add(25);
        Assert.assertEquals(2, indexCollection.indexOf(25));

        indexCollection.add(25);
        Assert.assertEquals(2, indexCollection.indexOf(25));

        indexCollection.add(2);
        Assert.assertEquals(0, indexCollection.indexOf(2));

    }

}