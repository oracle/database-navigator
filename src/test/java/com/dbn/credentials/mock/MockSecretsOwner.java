package com.dbn.credentials.mock;

import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MockSecretsOwner implements SecretsOwner {
    private Object secretOwnerId;
    private String secretOwnerName;
    private Secret[] secrets;

    @Override
    public void initSecrets() {}
}

