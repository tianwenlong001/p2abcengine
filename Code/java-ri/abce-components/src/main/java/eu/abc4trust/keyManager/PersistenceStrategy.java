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

package eu.abc4trust.keyManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import com.google.inject.Inject;


public class PersistenceStrategy {

    private final KeyStorage storage;

    @Inject
    public PersistenceStrategy(KeyStorage storage) {
        this.storage = storage;
    }

    public Object loadObject(URI uid) throws PersistenceException {
        ObjectInputStream objectInput = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            byte[] tokenBytes = this.storage.getValue(uid);
            if (tokenBytes == null) {
                return null;
            }
            byteArrayInputStream = new ByteArrayInputStream(tokenBytes);
            objectInput = new ObjectInputStream(byteArrayInputStream);
            return objectInput.readObject();
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        } finally {
            // Close the streams.
            this.closeIgnoringException(objectInput);
            this.closeIgnoringException(byteArrayInputStream);
        }
    }

    public boolean writeObject(URI uid, Object o) throws PersistenceException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutput = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutput = new ObjectOutputStream(byteArrayOutputStream);
            objectOutput.writeObject(o);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            this.storage.addValue(uid, bytes);
            return true;
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        } finally {
            // Close the streams.
            this.closeIgnoringException(objectOutput);
            this.closeIgnoringException(byteArrayOutputStream);
        }
    }

    private void closeIgnoringException(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ex) {
                // Ignore, there is nothing we can do if close fails.
            }
        }
    }

}
