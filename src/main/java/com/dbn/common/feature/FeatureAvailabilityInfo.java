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

package com.dbn.common.feature;

import lombok.Getter;

/**
 * Common purpose Availability information holder
 * Holds a {@link FeatureAvailability} attribute as well as a String message to further disambiguate or explain the availability
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class FeatureAvailabilityInfo {
    private final FeatureAvailability availability;
    private final String message;

    public FeatureAvailabilityInfo(FeatureAvailability availability) {
        this(availability, null);
    }
    public FeatureAvailabilityInfo(FeatureAvailability availability, String message) {
        this.availability = availability;
        this.message = message;
    }
}
