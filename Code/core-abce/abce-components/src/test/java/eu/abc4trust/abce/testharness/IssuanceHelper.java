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

package eu.abc4trust.abce.testharness;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import edu.rice.cs.plt.tuple.Pair;
import eu.abc4trust.abce.external.issuer.IssuerAbcEngine;
import eu.abc4trust.abce.external.revocation.RevocationAbcEngine;
import eu.abc4trust.abce.external.user.UserAbcEngine;
import eu.abc4trust.abce.external.verifier.VerifierAbcEngine;
import eu.abc4trust.abce.integrationtests.ReloadTokensInMemoryCommunicationStrategy;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.cryptoEngine.uprove.user.ReloadTokensCommunicationStrategy;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.returnTypes.IssuMsgOrCredDesc;
import eu.abc4trust.ui.idSelection.IdentitySelection;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.CredentialDescription;
import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.RevocationInformation;
import eu.abc4trust.xml.util.XmlUtils;

public class IssuanceHelper {
    private final Random random = new Random(43);

    public IssuanceHelper() {
        super();
    }

    public List<Attribute> populateIssuerAttributes(
            Map<String, Object> issuerAttsMap,
            String credentialSpecificationFilename) throws Exception {
        List<Attribute> issuerAtts = new LinkedList<Attribute>();
        ObjectFactory of = new ObjectFactory();

        CredentialSpecification credentialSpecification = this
                .loadCredentialSpecification(credentialSpecificationFilename);

        for (AttributeDescription attdesc : credentialSpecification
                .getAttributeDescriptions().getAttributeDescription()) {
            Attribute att = of.createAttribute();
            att.setAttributeUID(URI.create("" + this.random.nextLong()));
            URI type = attdesc.getType();
            AttributeDescription attd = of.createAttributeDescription();
            attd.setDataType(attdesc.getDataType());
            attd.setEncoding(attdesc.getEncoding());
            attd.setType(type);
            att.setAttributeDescription(attd);
            Object value = issuerAttsMap.get(type.toString());
            if (value != null) {
                issuerAtts.add(att);
                att.setAttributeValue(value);
            }
        }
        return issuerAtts;
    }

    public CredentialDescription issueCredential(Injector issuerInjector,
            Injector userInjector,
            String credentialSpecificationFilename,
            String issuancePolicyFilename, Map<String, Object> issuerAttsMap)
                    throws Exception {

        List<Attribute> issuerAtts = this.populateIssuerAttributes(
                issuerAttsMap, credentialSpecificationFilename);

        // Step 1. Load the credential specification into the keymanager.
        CredentialSpecification credentialSpecification = this.loadCredentialSpecification(credentialSpecificationFilename);

        KeyManager issuerKeyManager = issuerInjector
                .getInstance(KeyManager.class);
        issuerKeyManager.storeCredentialSpecification(
                credentialSpecification.getSpecificationUID(),
                credentialSpecification);

        KeyManager userKeyManager = userInjector.getInstance(KeyManager.class);
        userKeyManager.storeCredentialSpecification(
                credentialSpecification.getSpecificationUID(),
                credentialSpecification);

        // Step 2. Load the issuance policy and attributes.
        IssuancePolicy ip = (IssuancePolicy) XmlUtils.getObjectFromXML(this
                .getClass().getResourceAsStream(issuancePolicyFilename), true);

        // Step 3. Issue credential.
        IssuerAbcEngine issuerEngine = issuerInjector
                .getInstance(IssuerAbcEngine.class);
        UserAbcEngine userEngine = userInjector
                .getInstance(UserAbcEngine.class);

        //For reload of tokens set these on the UProve reloader
        Integer port = userInjector.getInstance(Key.get(Integer.class, Names.named("UProvePortNumber")));
        if (port!=null) {
            //uprove is set up
            ReloadTokensInMemoryCommunicationStrategy reloadTokens = (ReloadTokensInMemoryCommunicationStrategy) userInjector.getInstance(ReloadTokensCommunicationStrategy.class);
            reloadTokens.setIssuerAbcEngine(issuerEngine);
            //We need the re-issuance policy instead, but not all tests have those, so ignore if not present
            IssuancePolicy ip_reIssuance;
            try{
                ip_reIssuance = (IssuancePolicy) XmlUtils.getObjectFromXML(this
                        .getClass().getResourceAsStream(issuancePolicyFilename.subSequence(0, issuancePolicyFilename.length()-4)+"_reIssuance.xml"), true);
            }catch(Exception e){
                ip_reIssuance = (IssuancePolicy) XmlUtils.getObjectFromXML(this
                        .getClass().getResourceAsStream(issuancePolicyFilename), true);
            }
            reloadTokens.setIssuancePolicy(ip_reIssuance);
        }
        IssuanceHelper testHelper = new IssuanceHelper();
        CredentialDescription cd = testHelper.runIssuanceProtocol(issuerEngine,
                userEngine, ip, issuerAtts);
        assertNotNull(cd);

        // ObjectFactory of = new ObjectFactory();
        // JAXBElement<CredentialDescription> xcd = of
        // .createCredentialDescription(cd);
        // System.out.println(XmlUtils.toNormalizedXML(xcd));
        return cd;
    }

    public CredentialSpecification loadCredentialSpecification(
            String credentialSpecificationFilename) throws JAXBException,
            UnsupportedEncodingException, SAXException {
        CredentialSpecification credentialSpecification = (CredentialSpecification) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                credentialSpecificationFilename), true);
        return credentialSpecification;
    }

    public CredentialDescription runIssuanceProtocol(
            IssuerAbcEngine issuerEngine, UserAbcEngine userEngine,
            IssuancePolicy ip, List<Attribute> issuerAtts)
                    throws Exception {

        // Issuer starts the issuance.
        IssuanceMessageAndBoolean issuerIm = issuerEngine.initIssuanceProtocol(
                ip, issuerAtts);
        assertFalse(issuerIm.isLastMessage());

        ObjectFactory of = new ObjectFactory();
        System.out.println("Trying to print first issuance message");
        JAXBElement<IssuanceMessage> actual = of.createIssuanceMessage(issuerIm
                .getIssuanceMessage());
        System.out.println(XmlUtils.toNormalizedXML(actual));

        // Reply from user.
        IssuMsgOrCredDesc userIm = userEngine.issuanceProtocolStep(issuerIm
                .getIssuanceMessage());
        actual = of.createIssuanceMessage(userIm.im);
        System.out.println(XmlUtils.toNormalizedXML(actual));
        int round = 1;
        while (!issuerIm.isLastMessage()) {
            System.out.println("Issuance round: " + round);
            assertNotNull(userIm.im);
            issuerIm = issuerEngine.issuanceProtocolStep(userIm.im);

            // for (Object o : issuerIm.im.getAny()) {
            // System.out.println(o.getClass());
            // CredentialDescription cd = (CredentialDescription) XmlUtils
            // .unwrap(o, CredentialDescription.class);
            // for (Attribute a : cd.getAttribute()) {
            // System.out.println(a.getAttributeDescription().getType()
            // + ", " + a.getAttributeValue());
            // }
            // }

            // System.out.println("Issuer message:");
            // actual = of.createIssuanceMessage(issuerIm.im);
            // System.out.println(XmlUtils.toNormalizedXML(actual));

            assertNotNull(issuerIm.getIssuanceMessage());
            userIm = userEngine.issuanceProtocolStep(issuerIm
                    .getIssuanceMessage());

            // if (userIm.im != null) {
            // System.out.println("User message:");
            // actual = of.createIssuanceMessage(userIm.im);
            // System.out.println(XmlUtils.toNormalizedXML(actual));
            // }
            boolean userLastMessage = (userIm.cd != null);
            assertTrue(issuerIm.isLastMessage() == userLastMessage);
        }
        assertNull(userIm.im);
        assertNotNull(userIm.cd);
        return userIm.cd;
    }

    public Pair<PresentationToken, PresentationPolicyAlternatives> createPresentationToken(
            Injector verfierInjector, Injector userInjector,
            String presentationPolicyFilename,
            IdentitySelection idSelectionCallback) throws Exception {
        return this.createPresentationToken(verfierInjector, userInjector,
                presentationPolicyFilename, null, idSelectionCallback);
    }

    public Pair<PresentationToken, PresentationPolicyAlternatives> createPresentationToken(
            Injector verfierInjector, Injector userInjector,
            String presentationPolicyFilename,
            RevocationInformation revocationInformation,
            IdentitySelection idSelectionCallback) throws Exception {
        assertNotNull(presentationPolicyFilename);
        PresentationPolicyAlternatives presentationPolicyAlternatives = this.loadPresentationPolicy(presentationPolicyFilename);

        if (revocationInformation != null) {
            for (PresentationPolicy pp : presentationPolicyAlternatives
                    .getPresentationPolicy()) {
                for (CredentialInPolicy cred : pp.getCredential()) {
                    IssuerAlternatives ia = cred.getIssuerAlternatives();
                    for (IssuerParametersUID ipUid : ia
                            .getIssuerParametersUID()) {

                        URI revInfoUid = revocationInformation.getInformationUID();
                        ipUid.setRevocationInformationUID(revInfoUid);
                    }
                }
            }
        }

        assertNotNull(presentationPolicyAlternatives);

        UserAbcEngine userEngine = userInjector
                .getInstance(UserAbcEngine.class);

        PresentationToken presentationToken = userEngine
                .createPresentationToken(presentationPolicyAlternatives,
                        idSelectionCallback);
        if(presentationToken == null) {
            throw new RuntimeException("Cannot generate presentationToken");
        }
        assertNotNull(presentationToken);

        return new Pair<PresentationToken, PresentationPolicyAlternatives>(
                presentationToken, presentationPolicyAlternatives);
    }

    public Pair<PresentationToken, PresentationPolicyAlternatives> createPresentationToken_NotSatisfied(
            Injector verfierInjector, Injector userInjector,
            String presentationPolicyFilename,
            IdentitySelection idSelectionCallback) throws Exception {
        assertNotNull(presentationPolicyFilename);
        PresentationPolicyAlternatives presentationPolicyAlternatives = this.loadPresentationPolicy(presentationPolicyFilename);
        assertNotNull(presentationPolicyAlternatives);

        UserAbcEngine userEngine = userInjector
                .getInstance(UserAbcEngine.class);

        PresentationToken presentationToken = userEngine
                .createPresentationToken(presentationPolicyAlternatives,
                        idSelectionCallback);

        assertNull(presentationToken);

        return null;
    }

    public Pair<PresentationToken, PresentationPolicyAlternatives> createPresentationToken_NotSatisfied(
            Injector verfierInjector, Injector userInjector, RevocationInformation revocationInformation,
            String presentationPolicyFilename,
            IdentitySelection idSelectionCallback) throws Exception {
        assertNotNull(presentationPolicyFilename);
        PresentationPolicyAlternatives presentationPolicyAlternatives = this.loadPresentationPolicy(presentationPolicyFilename);
        assertNotNull(presentationPolicyAlternatives);

        if (revocationInformation != null) {
            for (PresentationPolicy pp : presentationPolicyAlternatives
                    .getPresentationPolicy()) {
                for (CredentialInPolicy cred : pp.getCredential()) {
                    IssuerAlternatives ia = cred.getIssuerAlternatives();
                    for (IssuerParametersUID ipUid : ia
                            .getIssuerParametersUID()) {

                        URI revInfoUid = revocationInformation.getInformationUID();
                        ipUid.setRevocationInformationUID(revInfoUid);
                    }
                }
            }
        }

        UserAbcEngine userEngine = userInjector
                .getInstance(UserAbcEngine.class);

        PresentationToken presentationToken = userEngine
                .createPresentationToken(presentationPolicyAlternatives,
                        idSelectionCallback);

        assertNull(presentationToken);

        return null;
    }

    private PresentationPolicyAlternatives loadPresentationPolicy(
            String presentationPolicyFilename) throws JAXBException,
            UnsupportedEncodingException, SAXException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(
                presentationPolicyFilename);
        assertNotNull(resourceAsStream);
        try {
            PresentationPolicyAlternatives presentationPolicyAlternatives = (PresentationPolicyAlternatives) XmlUtils
                    .getObjectFromXML(
                            resourceAsStream, true);
            return presentationPolicyAlternatives;
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    public PresentationToken verify(Injector verfierInjector,
            PresentationPolicyAlternatives presentationPolicyAlternatives,
            PresentationToken presentationToken) throws Exception {
        VerifierAbcEngine verifierEngine = verfierInjector
                .getInstance(VerifierAbcEngine.class);

        assertNotNull(presentationPolicyAlternatives);
        assertNotNull(presentationToken);

        PresentationTokenDescription pte = verifierEngine
                .verifyTokenAgainstPolicy(presentationPolicyAlternatives,
                        presentationToken, true);
        assertNotNull(pte);

        return presentationToken;
    }

    public Credential loadCredential(String credentialFilename)
            throws Exception {
        Credential credential = (Credential) XmlUtils.getObjectFromXML(this
                .getClass().getResourceAsStream(credentialFilename), true);
        return credential;
    }

    public void revokeCredential(Injector revocationInjector, URI revParamsUid,
            Attribute revocationHandleAttribute) throws CryptoEngineException {
        RevocationAbcEngine revocationEngine = revocationInjector
                .getInstance(RevocationAbcEngine.class);
        List<Attribute> attributes = new LinkedList<Attribute>();
        attributes.add(revocationHandleAttribute);
        revocationEngine.revoke(revParamsUid, attributes);
    }
}
