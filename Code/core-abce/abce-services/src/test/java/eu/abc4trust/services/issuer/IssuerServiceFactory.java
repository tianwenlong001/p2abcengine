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

package eu.abc4trust.services.issuer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import com.google.inject.Injector;
import com.sun.jersey.api.client.WebResource.Builder;

import edu.rice.cs.plt.tuple.Pair;
import eu.abc4trust.abce.external.user.UserAbcEngine;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManagerException;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.cryptoEngine.idemix.user.IdemixCryptoEngineUserImpl;
import eu.abc4trust.exceptions.CannotSatisfyPolicyException;
import eu.abc4trust.exceptions.IdentitySelectionException;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.returnTypes.IssuMsgOrCredDesc;
import eu.abc4trust.ri.servicehelper.FileSystem;
import eu.abc4trust.util.CryptoUriUtil;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.CredentialTemplate;
import eu.abc4trust.xml.IssuanceLogEntry;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.IssuerParametersInput;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.util.XmlUtils;

public class IssuerServiceFactory extends AbstractTestFactory {

    static ObjectFactory of = new ObjectFactory();

    final String baseUrl = "http://localhost:9500/abce-services/issuer";

    public SystemParameters getSystemParameters(int securityLevel,
            URI cryptoMechanism) {
        String requestString = "/setupSystemParameters/?securityLevel="
                + securityLevel + "&cryptoMechanism=" + cryptoMechanism;
        Builder resource = this.getHttpBuilder(requestString, this.baseUrl);

        SystemParameters systemParameters = resource
                .get(SystemParameters.class);
        return systemParameters;
    }

    public IssuerParameters getIssuerParameters(String issuerParametersUid)
            throws IOException, JAXBException, UnsupportedEncodingException,
            SAXException {
        InputStream is = FileSystem
                .getInputStream("/credentialSpecificationSimpleIdentitycard.xml");
        CredentialSpecification credSpec = (CredentialSpecification) XmlUtils
                .getObjectFromXML(is, false);

        return this.getIssuerParameters(issuerParametersUid,
                credSpec);
    }

    public IssuerParameters getIssuerParameters(String issuerParametersUid,
            CredentialSpecification credSpec)
                    throws IOException, JAXBException, UnsupportedEncodingException,
                    SAXException {
        URI hash = CryptoUriUtil.getHashSha256();
        String requestString = "/setupIssuerParameters/";

        Builder resource = this.getHttpBuilder(requestString, this.baseUrl);

        IssuerParametersInput ipt = of.createIssuerParametersInput();
        ipt.setAlgorithmID(URI.create("urn:abc4trust:1.0:algorithm:idemix"));
        // ipt.setAlgorithmID(URI.create("urn:abc4trust:1.0:algorithm:uprove"));
        ipt.setCredentialSpecUID(credSpec.getSpecificationUID());
        ipt.setHashAlgorithm(hash);
        ipt.setParametersUID(URI.create(issuerParametersUid));
        ipt.setRevocationParametersUID(null);
        ipt.setVersion("1.0");

        IssuerParameters issuerParameters = resource
                .post(
                        IssuerParameters.class, of.createIssuerParametersInput(ipt));
        return issuerParameters;
    }

    public IssuanceMessageAndBoolean initIssuanceProtocol(
            String issuerParametersUid, String issuancePolicyAndAttributesFile)
                    throws IOException, JAXBException, UnsupportedEncodingException,
                    SAXException {
        String requestString = "/initIssuanceProtocol/";

        Builder resource = this.getHttpBuilder(requestString, this.baseUrl);

        IssuancePolicyAndAttributes issuancePolicyAndAttributes = super
                .loadIssuancePolicyAndAttributes(issuancePolicyAndAttributesFile);

        CredentialTemplate credentialTemplate = issuancePolicyAndAttributes
                .getIssuancePolicy().getCredentialTemplate();
        credentialTemplate.setIssuerParametersUID(URI
                .create(issuerParametersUid));

        IssuanceMessageAndBoolean issuanceMessageAndBoolean = resource
                .post(IssuanceMessageAndBoolean.class,
                        of.createIssuancePolicyAndAttributes(issuancePolicyAndAttributes));

        return issuanceMessageAndBoolean;
    }

    public IssuanceMessageAndBoolean issuanceProtocolStep(
            IssuanceMessage issuanceMessage)
                    throws IOException, JAXBException, UnsupportedEncodingException,
                    SAXException {
        String requestString = "/issuanceProtocolStep/";

        Builder resource = this.getHttpBuilder(requestString, this.baseUrl);

        IssuanceMessageAndBoolean issuanceMessageAndBoolean = resource
                .post(IssuanceMessageAndBoolean.class,
                        of.createIssuanceMessage(issuanceMessage));

        return issuanceMessageAndBoolean;
    }

    @SuppressWarnings("deprecation")
    public Pair<IssuMsgOrCredDesc, URI> issueCredential(
            CredentialSpecification credentialSpecification,
            IssuerServiceFactory issuerServiceFactory,
            SystemParameters systemParameters, String issuerParametersUid,
            IssuerParameters issuerParameters, Injector userInjector)
                    throws KeyManagerException, CryptoEngineException, IOException,
                    JAXBException, UnsupportedEncodingException, SAXException,
                    CannotSatisfyPolicyException, IdentitySelectionException,
                    CredentialManagerException {
        KeyManager userKeyManager = userInjector.getInstance(KeyManager.class);

        userKeyManager.storeSystemParameters(systemParameters);

        userKeyManager.storeIssuerParameters(URI.create(issuerParametersUid),
                issuerParameters);

        userKeyManager.storeCredentialSpecification(
                credentialSpecification.getSpecificationUID(),
                credentialSpecification);

        UserAbcEngine userEngine = userInjector
                .getInstance(UserAbcEngine.class);

        IdemixCryptoEngineUserImpl.loadIdemixSystemParameters(systemParameters);

        URI issuanceLogEntryUid = null;

        // Init issuance protocol.
        IssuanceMessageAndBoolean issuerIssuanceMessage = issuerServiceFactory
                .initIssuanceProtocol(issuerParametersUid,
                        "/issuancePolicyAndAttributes.xml");
        assertNotNull(issuerIssuanceMessage);

        issuanceLogEntryUid = issuerIssuanceMessage.getIssuanceLogEntryURI();

        // Reply from user.
        IssuMsgOrCredDesc userIm = userEngine
                .issuanceProtocolStep(issuerIssuanceMessage
                        .getIssuanceMessage());

        // int round = 1;
        while (!issuerIssuanceMessage.isLastMessage()) {
            // System.out.println("Issuance round: " + round);

            assertNotNull(userIm.im);

            // Issuer issuance protocol step.
            issuerIssuanceMessage = issuerServiceFactory
                    .issuanceProtocolStep(userIm.im);
            assertNotNull(issuerIssuanceMessage);

            issuanceLogEntryUid = issuerIssuanceMessage
                    .getIssuanceLogEntryURI();

            assertNotNull(issuerIssuanceMessage.getIssuanceMessage());
            userIm = userEngine.issuanceProtocolStep(issuerIssuanceMessage
                    .getIssuanceMessage());

            boolean userLastMessage = (userIm.cd != null);
            assertTrue(issuerIssuanceMessage.isLastMessage() == userLastMessage);
        }
        return new Pair<IssuMsgOrCredDesc, URI>(userIm, issuanceLogEntryUid);
    }

    public IssuanceLogEntry getIssuanceLogEntry(URI issuanceEntryUid) {
        String requestString = "/getIssuanceLogEntry/?issuanceEntryUid="
                + issuanceEntryUid.toString();

        Builder resource = this.getHttpBuilder(requestString, this.baseUrl);

        IssuanceLogEntry issuanceLogEntry = resource
                .get(IssuanceLogEntry.class);

        return issuanceLogEntry;
    }
}

