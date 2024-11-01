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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class ChecksumTest {
    @Test
    public void softTest() {
        URL resource = getClass().getResource("checksum");
        File file = new File(resource.getPath());
        String checksum = Checksum.soft(file);

        Assert.assertEquals("7f196cd5d143cadab0e61d98017e088f4088bf91e2da98c2b06d1fe3e9144106", checksum);
    }

    @Test
    public void strongTest() {
        URL resource = getClass().getResource("checksum");
        File file = new File(resource.getPath());
        String checksum = Checksum.strong(file);

        Assert.assertEquals("d3d6a0e6bc321f42fca0ab97fd1c1ddde74c91026610653f4e6d2518ced18355", checksum);
    }

}