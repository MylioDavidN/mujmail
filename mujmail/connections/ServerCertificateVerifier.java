//#condition MUJMAIL_SSL
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 Nodir Yuldashev
 
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package mujmail.connections;


import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.crypto.tls.CertificateVerifyer;

/**
 * Verify cerfificate 
 * Each certificate asume as valid. No checking is done 
 */
public class ServerCertificateVerifier implements CertificateVerifyer {

    private static final boolean DEBUG = false;
    
    /**
     * Checks if certificate is trusty.
     * 
     * @param certs Certificate to check.
     * @return Always true.
     */
    public boolean isValid(X509CertificateStructure[] certs) {

        if (DEBUG) {
            for (int i = 0; i < certs.length; i++) {
                System.out.println("Certificate issued by " + certs[i].getIssuer().toString());
                System.out.println(" to " + certs[i].getSubject().toString());
                System.out.println();
            }
        }
        return true;
    }

}
