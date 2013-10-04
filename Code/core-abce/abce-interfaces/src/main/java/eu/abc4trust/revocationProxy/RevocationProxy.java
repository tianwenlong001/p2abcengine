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

package eu.abc4trust.revocationProxy;

import eu.abc4trust.xml.RevocationAuthorityParameters;
import eu.abc4trust.xml.RevocationMessage;

public interface RevocationProxy {
    /**
     * This method carries out one step in a possibly interactive protocol with a Revocation
     * Authority. Depending on the revocation mechanism, interaction with the Revocation Authority may
     * be needed during credential issuance, during creation or verification of a presentation token,
     * or during the separately triggered creation or update of non-revocation evidence. This method
     * will be called by the CryptoEngine of the User, Verifier, or Issuer whenever it needs to
     * interact with the Revocation Authority. The method acts as a proxy for the communication with
     * the Revocation Authority: in the parameters revpars it looks up the appropriate endpoint to
     * contact the Revocation Authority, sends the outgoing revocation message m, and returns the
     * response received from the Revocation Authority to the local CryptoEngine. The returned message
     * will have the same Context attribute as the outgoing message m, so that the different messages
     * in a protocol execution can be linked.
     * 
     * @param m
     * @param revpars
     * @return
     */
    public RevocationMessage processRevocationMessage(RevocationMessage m,
            RevocationAuthorityParameters revpars) throws Exception;
}
