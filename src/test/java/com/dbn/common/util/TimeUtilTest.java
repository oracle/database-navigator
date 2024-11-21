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

package com.dbn.common.util;

import com.dbn.common.util.TimeUtil;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.common.util.TimeUtil.Millis.*;

public class TimeUtilTest {

    @Test
    public void presentableDuration() {
        Assert.assertEquals("1h", logged(TimeUtil.presentableDuration(ONE_HOUR, true)));
        Assert.assertEquals("1h 1m", logged(TimeUtil.presentableDuration(ONE_HOUR + ONE_MINUTE, true)));
        Assert.assertEquals("1h 35m", logged(TimeUtil.presentableDuration(ONE_HOUR + ONE_MINUTE * 35, true)));
        Assert.assertEquals("2h 35m", logged(TimeUtil.presentableDuration(ONE_HOUR * 2 + ONE_MINUTE * 35, true)));
        Assert.assertEquals("1m", logged(TimeUtil.presentableDuration(ONE_MINUTE, true)));
        Assert.assertEquals("1m 45s", logged(TimeUtil.presentableDuration(ONE_MINUTE + ONE_SECOND * 45, true)));
        Assert.assertEquals("3m 45s", logged(TimeUtil.presentableDuration(ONE_MINUTE * 3 + ONE_SECOND * 45, true)));
        Assert.assertEquals("1000 ms", logged(TimeUtil.presentableDuration(ONE_SECOND, true)));
        Assert.assertEquals("1456 ms", logged(TimeUtil.presentableDuration(ONE_SECOND + 456, true)));
        Assert.assertEquals("3456 ms", logged(TimeUtil.presentableDuration(ONE_SECOND * 3 + 456, true)));
        Assert.assertEquals("7s 456ms", logged(TimeUtil.presentableDuration(ONE_SECOND * 7 + 456, true)));
        Assert.assertEquals("one hour", logged(TimeUtil.presentableDuration(ONE_HOUR, false)));
        Assert.assertEquals("one hour and one minute", logged(TimeUtil.presentableDuration(ONE_HOUR + ONE_MINUTE, false)));
        Assert.assertEquals("one hour and 35 minutes", logged(TimeUtil.presentableDuration(ONE_HOUR + ONE_MINUTE * 35, false)));
        Assert.assertEquals("2 hours and 35 minutes", logged(TimeUtil.presentableDuration(ONE_HOUR * 2 + ONE_MINUTE * 35, false)));
        Assert.assertEquals("one minute", logged(TimeUtil.presentableDuration(ONE_MINUTE, false)));
        Assert.assertEquals("one minute and 45 seconds", logged(TimeUtil.presentableDuration(ONE_MINUTE + ONE_SECOND * 45, false)));
        Assert.assertEquals("3 minutes and 45 seconds", logged(TimeUtil.presentableDuration(ONE_MINUTE * 3 + ONE_SECOND * 45, false)));
        Assert.assertEquals("1000 ms", logged(TimeUtil.presentableDuration(ONE_SECOND, false)));
        Assert.assertEquals("1456 ms", logged(TimeUtil.presentableDuration(ONE_SECOND + 456, false)));
        Assert.assertEquals("3456 ms", logged(TimeUtil.presentableDuration(ONE_SECOND * 3 + 456, false)));
        Assert.assertEquals("7 seconds and 456 ms", logged(TimeUtil.presentableDuration(ONE_SECOND * 7 + 456, false)));
            }
    
    private static String logged(String val) {
        System.out.println(val);
        return val;
    }
}