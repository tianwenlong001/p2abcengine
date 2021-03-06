//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//* Alexandra Instituttet A/S                                         *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2014. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package eu.abc4trust.revocationProxy;

import eu.abc4trust.xml.CryptoParams;
import eu.abc4trust.xml.Reference;
import eu.abc4trust.xml.RevocationMessage;

public interface RevocationProxyCommunicationStrategy {

    CryptoParams requestRevocationHandle(RevocationMessage m,
            Reference nonRevocationEvidenceReference)
                    throws RevocationProxyException, Exception;

    CryptoParams requestRevocationInformation(RevocationMessage m,
            Reference revocationInfoReference) throws RevocationProxyException, Exception;

    CryptoParams revocationEvidenceUpdate(RevocationMessage m,
            Reference nonRevocationEvidenceUpdateReference)
                    throws RevocationProxyException, Exception;

    CryptoParams getCurrentRevocationInformation(RevocationMessage m,
      Reference revocationInfoReference) throws RevocationProxyException, Exception;
}
