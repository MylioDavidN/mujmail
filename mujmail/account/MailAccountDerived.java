//#condition MUJMAIL_USR_FOLDERS
/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

package mujmail.account;

import mujmail.InBox;
import mujmail.protocols.IMAP4;
import mujmail.protocols.InProtocol;
import mujmail.protocols.POP3;


/**
 * Account that represents special mail account. This account is derived from a primary account. 
 * <p>These accounts are used from user folder for retrieving mails.
 * <p>This account can have differen imap folders to retrieve. Othere entrines are shared with primary account.
 */
public class MailAccountDerived implements MailAccount {

    /** String representation of account class type. Used for synchronization purposes */
    public static final String CLASS_TYPE_STRING = "Derived";
    
    /** Primary account where we refer for undefined informations*/
    private MailAccount sourceAccount;
    /** Holds imap folder derived account retieves */
    private String IMAPPrimaryBox = "INBOX";
    /** Imap pusher for acccount */
    private IMAP4 imapPush;
    /** Position where this account is be stored database*/
    private int recordID;
    /** protocol that can used for retrieving email by current account */
    private InProtocol protocol;

    /**
     * 
     * @param ma
     * @param IMAPPrimaryBox
     */
    public MailAccountDerived( MailAccount ma, String IMAPPrimaryBox) {
        this.sourceAccount = ma;
        this.IMAPPrimaryBox = IMAPPrimaryBox;
        if (ma.isIMAP()) {
            this.protocol = new IMAP4(this);
        } else {
            this.protocol = new POP3(this);
        }
    }
    
    /**
     * Mail account that is refecenced by this proxy class.
     *
     * @return Mail account that holds informacions.
     */
    public MailAccount getSourceAccount() {
        return sourceAccount;
    }

    public boolean isIMAP() {
        return sourceAccount.isIMAP();
    }
    
    public boolean isPOP3() {
        return sourceAccount.isPOP3();
    }
    
    //#ifdef MUJMAIL_SYNC
    /**
     * This function converts MailAccount object to string format
     * Used when synchronization with server is invoked.
     * @return String representation of this object
     */
    public String serialize() {
        // Never used, Derived account not stored in account database
        // To shrink code, commented
        return null;
        /*
        String result = new String();
        result += MailAccount.CLASS_TYPE_ID_STRING + CLASS_TYPE_STRING + "\n";
        result += "SourceAccount: " + sourceAccount.getEmail() + "\n";
        result += "IMAPPrimaryBox: " + this.IMAPPrimaryBox + "\n\n";
    	return result;
        /* */
    }
    //#endif
    
    //#ifdef MUJMAIL_SYNC    
    /* *
     * This function parses account data string which is received from mujMail server.
     * 
     * @param acctStr	Account data string to be parsed
     * @return			Account data object created from given string
     */
     // Never used, Derived account not stored in account database
     // To shrink code, commented
    /*
     public static MailAccount parseAccountString(String acctStr) {
        acctStr = MailAccountPrimary.getNextValue(acctStr);
        String accountClassType = acctStr.substring(0, acctStr.indexOf('\n'));
        if (!accountClassType.equalsIgnoreCase(CLASS_TYPE_STRING)) {
            System.out.println(SOURCE_FILE+":parseAccountString - invalid handling class" + accountClassType);
            return null;
        }

        acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
        acctStr = MailAccountPrimary.getNextValue(acctStr);
        String sourceAccount   = acctStr.substring(0, acctStr.indexOf('\n'));

        acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
        acctStr = MailAccountPrimary.getNextValue(acctStr);        
        String IMAPPrimaryBox   = acctStr.substring(0, acctStr.indexOf('\n'));
        acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
        
    	MailAccount sa = (MailAccount)MujMail.mujmail.getMailAccounts().get(sourceAccount);
        if (sa==null) {
            System.out.println(SOURCE_FILE+":parseAccountString - not existing source mail account" + sourceAccount);
            return null;
        }
    	return new MailAccountDerived(sa, IMAPPrimaryBox);
    }
   /* End of commented parseAccountString function */
   //#endif
    
    /**
     * Prepares the account for pushing.
     */
    public void prepareForPushing(InBox box) {
        IMAP4 imapIDLE = new IMAP4(this);
        imapIDLE.targetBox = box;
        imapPush = imapIDLE;
    }
    
    public void startPushing() {
        imapPush.push();
    }

    public String getEmail() {
        return sourceAccount.getEmail();
    }

    public void setEmail(String email) {
        sourceAccount.setEmail(email);
    }

    public String getServer() {
        return sourceAccount.getServer();
    }

    public short getPort() {
        return sourceAccount.getPort();
    }

    public void setPort(short port) {
        sourceAccount.setPort(port);
    }

    public boolean isSSL() {
        return sourceAccount.isSSL();
    }

    public byte getSSLType() {
         return sourceAccount.getSSLType();
     }

    public String getUserName() {
        return sourceAccount.getUserName();
    }

    public String getPassword() {
        return sourceAccount.getPassword();
    }

    public String getIMAPPprimaryBox() {
        return IMAPPrimaryBox;
    }

    public byte getType() {
        return sourceAccount.getType();
    }

    public void setType(byte type) {
        sourceAccount.setType(type);
    }

    public void setSSL(boolean SSL) {
        sourceAccount.setSSL(SSL);
    }

    //#ifdef MUJMAIL_SSL
    public void setSSLType(byte SSLType) {
        sourceAccount.setSSLType(SSLType);
    }
    //#endif

    public void setServer(String server) {
        sourceAccount.setServer(server);
    }

    public void setProtocol(InProtocol protocol) {
        this.protocol = protocol;
    }

    public void setUserName(String userName) {
        sourceAccount.setUserName(userName);
    }

    public void setPassword(String password) {
        sourceAccount.setPassword(password);
    }

    public void setIMAPPrimaryBox(String IMAPPrimaryBox) {
        this.IMAPPrimaryBox = IMAPPrimaryBox;
    }

    /**
     * Gets protocol instance that can be used for communication 
     *  with mail server described in account
     * @return Protocol object
     */
    public InProtocol getProtocol() {
        return protocol;
    }

    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int recordID) {
        this.recordID = recordID;
    }

    public boolean isActive() {
        return sourceAccount.isActive();
    }

    public void setActive(boolean active) {
        sourceAccount.setActive(active);
    }

    public boolean isCopyToSrvTrash() {
        return sourceAccount.isCopyToSrvTrash();
    }

    public void setCopyToSrvTrash(boolean copyToSrvTrash) {
        sourceAccount.setCopyToSrvTrash(copyToSrvTrash);
    }
    
    public boolean isCopyToSrvSent() {
        return sourceAccount.isCopyToSrvSent();
    }

    public void setCopyToSrvSent(boolean copyToSrvSent) {
        sourceAccount.setCopyToSrvSent(copyToSrvSent);
    }

    public String getCopyToSrvSentFolderName() {
        return sourceAccount.getCopyToSrvSentFolderName();
    }
    
    public void setCopyToSrvSentFolderName(String copyToSrvSentFolderName) {
        sourceAccount.setCopyToSrvSentFolderName(copyToSrvSentFolderName);
    }
    
    public String getCopyToSrvTrashFolderName() {
        return sourceAccount.getCopyToSrvTrashFolderName();
    }

    public void setCopyToSrvTrashFolderName(String copyToSrvTrashFolderName) {
        sourceAccount.setCopyToSrvTrashFolderName(copyToSrvTrashFolderName);
    }
    
    public byte getAccountClassType() {
        return MailAccount.ACCOUNT_CLASS_TYPE_DERIVED;
    }
}

