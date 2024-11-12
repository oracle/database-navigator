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
 * Small memory footprint collection of primitive int values.
 * The values are held in a sorted int array which gets reconstructed with every insertion
 * The elements lookup and insertion index resolution is done with binary search
 * (this is to replace the outdated THashSet from trove4j)
 *
 * TODO for larger collections it's probably better to switch to IntHashSet ("add-only" copy of java.util.regex.IntHashSet)
 * (lookup inside a 100 records collections translates to up to 7 basic operations)
 *
 * @author Dan Cioca (Oracle)
 */
public class IndexCollection{
    public static final int[] EMPTY_ARRAY = new int[0];
    private int[] values;

    public IndexCollection() {
        values = EMPTY_ARRAY;
    }

    public IndexCollection(int ... values) {
        this.values = Arrays.copyOf(values, values.length);
    }

    int[] values() {
        return values;
    }

    public boolean isEmpty() {
        return values.length == 0;
    }

    public synchronized void add(int value) {
        int index = insertionIndex(value);
        if (index == -1) return; // no change

        int[] copy = new int[values.length + 1];

        copy[index] = value;
        System.arraycopy(values, 0, copy, 0, index);
        System.arraycopy(values, index, copy, index + 1, values.length - index);

        this.values = copy;
    }

    private int insertionIndex(int value) {
        if (isEmpty()) return 0;
        int left = 0;
        int right = values.length - 1;

        if (value < values[left]) return 0;
        if (value > values[right]) return values.length;


        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (values[mid] == value) return -1; // already present
            if (values[mid] < value) left = mid + 1; else right = mid - 1;
        }

        return left;
    }

    public boolean contains(int value) {
        //return indexOf(value) > -1;

        if (values.length == 0) return false;

        int left = 0;
        int right = values.length - 1;
        if (value < values[left]) return false;
        if (value > values[right]) return false;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (values[mid] == value) return true; // found
            if (values[mid] > value) right = mid - 1; else left = mid + 1;
        }

        return false;
    }

    public int indexOf(int value) {
        if (values.length == 0) return -1;

        int left = 0;
        int right = values.length - 1;
        if (value < values[left]) return -1;
        if (value > values[right]) return -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (values[mid] == value) return mid; // found
            if (values[mid] > value) right = mid - 1; else left = mid + 1;
        }

        return -1;
    }


    public int size() {
        return values.length;
    }
}
