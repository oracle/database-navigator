package com.dbn.credentials;

import com.dbn.credentials.mock.MockSecretsOwner;
import com.dbn.credentials.mock.TestableDatabaseCredentialManager;
import com.dbn.test.util.RegressionTest;
import com.dbn.test.util.RegressionTest.BugSystem;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.idea.TestFor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@TestFor(classes = DatabaseCredentialManager.class)
@RunWith(Parameterized.class)
public class DatabaseCredentialManagerTests {

    private static MockSecretsOwner secretsOwner;
    private final SecretType secretType;
    private final Object ownerId;
    private final String userName;
    private final String expectedServiceName;

    @BeforeClass
    public static void beforeClass() {
        secretsOwner = new MockSecretsOwner("FooBar", "MyConnectionName", new Secret[0]);
        SecretsOwnerRegistry.register(secretsOwner);
    }

    public DatabaseCredentialManagerTests(String expectedServiceName, SecretType secretType, Object ownerId, String userName) {
        this.expectedServiceName = expectedServiceName;
        this.secretType = secretType;
        this.ownerId = ownerId;
        this.userName = userName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // the first two expected service names should be the same whether userName is null or ""
                // this is the crux of the JDBC-4636 error cause
                {"DB Navigator - Azure token client secret: default@MyConnectionName", SecretType.CONNECTION_AZURE_TOKEN_CLIENT_SECRET, "FooBar", ""},
                {"DB Navigator - Azure token client secret: default@MyConnectionName",SecretType.CONNECTION_AZURE_TOKEN_CLIENT_SECRET, "FooBar", null},
                // make sure that user/password creds, which always use non-empty, non-null user names still works
                // as expected
                {"DB Navigator - Connection password: ADMIN@MyConnectionName", SecretType.CONNECTION_PASSWORD, "FooBar", "ADMIN"},
        });
    }

    @Test
    @RegressionTest(source = BugSystem.JIRA, number = 4636, component = "JDBC")
    public void testCredentialAttributeKey() {
        CredentialAttributes target=
            TestableDatabaseCredentialManager.
                    createAttributes(this.secretType, this.ownerId, this.userName);
        Assert.assertEquals(this.expectedServiceName, target.getServiceName());
        Assert.assertEquals(this.userName, target.getUserName());
        Assert.assertFalse(target.isPasswordMemoryOnly());
        Assert.assertTrue(target.getCacheDeniedItems());
    }
}
