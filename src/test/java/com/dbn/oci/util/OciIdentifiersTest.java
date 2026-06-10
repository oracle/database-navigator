/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.oci.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OciIdentifiersTest {
    @Test
    public void testUserOcidValidationAcceptsDifferentRealms() {
        assertTrue(OciIdentifiers.isUserOcid("ocid1.user.oc1..aaaa"));
        assertTrue(OciIdentifiers.isUserOcid("ocid1.user.oc2..aaaa"));
        assertTrue(OciIdentifiers.isUserOcid(" ocid1.user.oc3..aaaa "));

        assertFalse(OciIdentifiers.isUserOcid("ocid1.tenancy.oc1..aaaa"));
        assertFalse(OciIdentifiers.isUserOcid("ocid1.user.oc1."));
        assertFalse(OciIdentifiers.isUserOcid(null));
    }

    @Test
    public void testTenancyOcidValidationAcceptsDifferentRealms() {
        assertTrue(OciIdentifiers.isTenancyOcid("ocid1.tenancy.oc1..aaaa"));
        assertTrue(OciIdentifiers.isTenancyOcid("ocid1.tenancy.oc2..aaaa"));

        assertFalse(OciIdentifiers.isTenancyOcid("ocid1.compartment.oc1..aaaa"));
        assertFalse(OciIdentifiers.isTenancyOcid("ocid1.tenancy..aaaa"));
    }

    @Test
    public void testCompartmentScopeAcceptsCompartmentOrTenancy() {
        assertTrue(OciIdentifiers.isCompartmentScopeOcid("ocid1.compartment.oc1..aaaa"));
        assertTrue(OciIdentifiers.isCompartmentScopeOcid("ocid1.compartment.oc2..aaaa"));
        assertTrue(OciIdentifiers.isCompartmentScopeOcid("ocid1.tenancy.oc1..aaaa"));

        assertFalse(OciIdentifiers.isCompartmentScopeOcid("ocid1.user.oc1..aaaa"));
        assertFalse(OciIdentifiers.isCompartmentScopeOcid("compartment"));
    }
}
