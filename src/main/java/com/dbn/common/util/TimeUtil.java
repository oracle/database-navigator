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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nls;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.util.TimeUtil.Millis.ONE_HOUR;
import static com.dbn.common.util.TimeUtil.Millis.ONE_MINUTE;
import static com.dbn.nls.NlsResources.txt;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@UtilityClass
public class TimeUtil {
    public interface Millis{
        long ONE_SECOND = 1000;
        long TWO_SECONDS = 2 * ONE_SECOND;
        long FIVE_SECONDS = 5 * ONE_SECOND;
        long TEN_SECONDS = 10 * ONE_SECOND;
        long THIRTY_SECONDS = 30 * ONE_SECOND;
        long ONE_MINUTE = 60 * ONE_SECOND;
        long ONE_HOUR = 60 * ONE_MINUTE;
        long THREE_MINUTES = 3 * ONE_MINUTE;
        long FIVE_MINUTES = 5 * ONE_MINUTE;
        long TEN_MINUTES = 10 * ONE_MINUTE;
    }

    public static int getMinutes(int seconds) {
        return seconds / 60;
    }

    public static int getSeconds(int minutes) {
        return minutes * 60;
    }

    public static boolean isOlderThan(long timestamp, long millis) {
        return currentTimeMillis() - millis > timestamp;
    }

    public static boolean isOlderThan(long timestamp, Duration duration) {
        return currentTimeMillis() - timestamp > duration.toMillis();
    }

    public static boolean isOlderThan(long timestamp, long duration, TimeUnit timeUnit) {
        return currentTimeMillis() - timeUnit.toMillis(duration) > timestamp;
    }

    public static long millisSince(long start) {
        return currentTimeMillis() - start;
    }

    public static long secondsSince(long start) {
        return MILLISECONDS.toSeconds(millisSince(start));
    }

    public static @Nls String presentableDuration(Duration duration, boolean compact) {
        return presentableDuration(duration.toMillis(), compact);
    }

    public static @Nls String presentableDuration(long millis, boolean compact) {
        long hours = MILLISECONDS.toHours(millis);
        if (hours > 0) {
            long minutes = MILLISECONDS.toMinutes(millis - (hours * ONE_HOUR));
            String hoursDuration = presentableDuration(hours, DurationUnit.HOUR, compact);
            return minutes > 0 ?
                    composeDuration(hoursDuration, presentableDuration(minutes, DurationUnit.MINUTE, compact), compact) :
                    hoursDuration;
        }

        long minutes = MILLISECONDS.toMinutes(millis);
        if (minutes > 0) {
            long seconds = MILLISECONDS.toSeconds(millis - (minutes * ONE_MINUTE));
            String minutesDuration = presentableDuration(minutes, DurationUnit.MINUTE, compact);
            return seconds > 0 ?
                    composeDuration(minutesDuration, presentableDuration(seconds, DurationUnit.SECOND, compact), compact) :
                    minutesDuration;
        }

        long seconds = MILLISECONDS.toSeconds(millis);
        if (seconds > 0) return presentableDuration(seconds, DurationUnit.SECOND, compact);

        return txt("app.shared.unit.Duration_MILLISECOND", millis);
    }

    private static @Nls String composeDuration(@Nls String firstDuration, @Nls String secondDuration, boolean compact) {
        return compact ?
                txt("app.shared.text.CompactDurationComposition", firstDuration, secondDuration) :
                txt("app.shared.text.DurationComposition", firstDuration, secondDuration);
    }

    private static @Nls String presentableDuration(long value, DurationUnit unit, boolean compact) {
        return switch (unit) {
            case HOUR -> compact ?
                    txt("app.shared.unit.CompactDuration_HOUR", value) :
                    txt("app.shared.unit.Duration_HOUR", value);
            case MINUTE -> compact ?
                    txt("app.shared.unit.CompactDuration_MINUTE", value) :
                    txt("app.shared.unit.Duration_MINUTE", value);
            case SECOND -> compact ?
                    txt("app.shared.unit.CompactDuration_SECOND", value) :
                    txt("app.shared.unit.Duration_SECOND", value);
            case MILLISECOND -> compact ?
                    txt("app.shared.unit.CompactDuration_MILLISECOND", value) :
                    txt("app.shared.unit.Duration_MILLISECOND", value);
        };
    }

    private enum DurationUnit {
        HOUR,
        MINUTE,
        SECOND,
        MILLISECOND
    }
}
