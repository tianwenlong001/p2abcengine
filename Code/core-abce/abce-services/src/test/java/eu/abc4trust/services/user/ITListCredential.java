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

package eu.abc4trust.services.user;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

import eu.abc4trust.services.ITAbstract;
import eu.abc4trust.services.TestScenarioFactory;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.URISet;

public class ITListCredential extends ITAbstract {

    static ObjectFactory of = new ObjectFactory();

    final String baseUrl = "http://localhost:9500/abce-services/issuer";

    @Test
    public void issuanceProtocolIdemix() throws Exception {
        String engineSuffix = "idemix";

        this.deleteStorageDirectory("user_storage");

        UserServiceFactory userServiceFactory = new UserServiceFactory();

        URISet credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().isEmpty());

        TestScenarioFactory testScenarioFactory = new TestScenarioFactory();
        URI credentialUid = testScenarioFactory.issuanceProtocol(engineSuffix);

        credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().size() == 1);
        assertTrue(credentials.getURI().get(0).compareTo(credentialUid) == 0);
    }

    @Test
    public void issuanceProtocolUProve() throws Exception {

        String engineSuffix = "uprove";

        this.deleteStorageDirectory("user_storage");

        UserServiceFactory userServiceFactory = new UserServiceFactory();

        URISet credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().isEmpty());

        TestScenarioFactory testScenarioFactory = new TestScenarioFactory();
        URI credentialUid = testScenarioFactory.issuanceProtocol(engineSuffix);

        credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().size() == 1);
        assertTrue(credentials.getURI().get(0).compareTo(credentialUid) == 0);
    }

}