//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.abce.internal.user.credentialManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.util.StorageUtil;

public class PersistentSecretStorage implements SecretStorage {

    private final File secretFile;

    @Inject
    public PersistentSecretStorage(
            @Named("SecretStorageFile") File secretFile) {
        super();
        this.secretFile = secretFile;

    }

    @Override
    public byte[] getSecret(URI creduid) throws Exception {
        return StorageUtil.getData(this.secretFile, creduid);
    }


    @Override
    public List<URI> listSecrets() throws Exception {
        return StorageUtil.getAllUris(this.secretFile);
    }

    @Override
    public void addSecret(URI secuid, byte[] secBytes) throws IOException {
        StorageUtil.appendData(this.secretFile, secuid, secBytes);
    }

    @Override
    public void deleteSecret(URI secuid) throws Exception {
        StorageUtil.deleteData(this.secretFile, secuid);
    }

}
