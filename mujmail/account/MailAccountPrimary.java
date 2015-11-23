package mujmail.account;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>
 
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

import mujmail.InBox;
import mujmail.protocols.IMAP4;
import mujmail.protocols.InProtocol;

/**
 * Represents POP3 or IMAP4 account.
 * It contains the InProtocol object.
 */
public class MailAccountPrimary implements MailAccount{
    /** The name of this source file */
    private static final String SOURCE_FILE = "MailAccountPrimary";

    /** Position in persistens database */
    private int recordID;
    private byte type;
    private String email;
    /** Active accounts are retrieved into standard inbox folder */
    private boolean active;
    private String server;
    /** Protocol for communication with server specified in this connection */
    private InProtocol protocol;
    private short port;
    private String userName;
    private String password = "";
    private boolean SSL;
    private byte SSLType = 0;
    private String IMAPPrimaryBox = "INBOX";
    private IMAP4 imapPush;

    private boolean copyToSrvSent = true; //copy to Srv sent when sent new mail
    private boolean copyToSrvTrash = true; //copy to Srv trash when delMailFromServer
    private String copyToSrvSentFolderName = CONST_IMAP_SERVER_SENTBOX;   // Server folder name where to store sent mails
    private String copyToSrvTrashFolderName = CONST_IMAP_SERVER_TRASHBOX; // Server folder name where to store deleted mails
    
    /** String reprezentation of account class type. Used for synchronization purposes */
    public static final String CLASS_TYPE_STRING = "Primary";

    /** 
     * Creates uninitialized instance of accout. 
     * <p>Use setters for initialize account.
     */
    public MailAccountPrimary() {
    }
    
    /**
     * Creates new initialized instance of account.
     */
    public MailAccountPrimary(byte type,
            String email,
            boolean active,
            String server,
            short port,
            String userName,
            String password,
            boolean SSL,
            byte mujMailSSL,
            boolean copyToSrvSent,
            boolean copyToSrvTrash,
            String copyToSrvSentFolderName,
            String copyToSrvTrashFolderName) {
        this.type = type;
        this.email = email;
        this.active = active;
        this.server = server;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.SSL = SSL;
        //#ifdef MUJMAIL_SSL
        this.SSLType = mujMailSSL;
        //#endif
        this.copyToSrvSent = copyToSrvSent;
        this.copyToSrvTrash = copyToSrvTrash;
        this.copyToSrvSentFolderName = copyToSrvSentFolderName;
        this.copyToSrvTrashFolderName = copyToSrvTrashFolderName;
    }

    //#ifdef MUJMAIL_SYNC
    public String serialize() {
    	StringBuffer buff = new StringBuffer();

        buff.append( MailAccount.CLASS_TYPE_ID_STRING ).append( CLASS_TYPE_STRING ).append('\n');
    	
        buff.append("Active: ").append( this.isActive() ? 1 : 0).append('\n')
        	.append("Email: ").append( this.email ).append('\n')
        	.append("Type: ").append( this.type ).append('\n')
        	.append("SSL: ").append( this.SSL ? 1 : 0 ).append('\n')
        	.append("SSLType: ").append( this.SSLType ).append('\n')
        	.append("Server: ").append( this.server ).append('\n')
        	.append("Port: ").append( this.port ).append('\n')
        	.append("User: ").append( this.userName ).append('\n')
        	.append("IMAPPrimaryBox: ").append( this.IMAPPrimaryBox ).append('\n')
        	.append("CopyToSrvSent: ").append( this.copyToSrvSent ? 0 : 1 ).append('\n')
        	.append("CopyToSrvSentFolderName: ").append( this.copyToSrvSentFolderName ).append('\n')
        	.append("CopyToSrvTrash: ").append( this.copyToSrvTrash? 0 : 1 ).append('\n')
        	.append("CopyToSrvTrashFolderName: ").append( this.copyToSrvTrashFolderName ).append('\n')
        	;
    	return buff.toString();
    }
    //#endif
    
    //#ifdef MUJMAIL_DEVELOPMENT
//#     /**
//#      * This function converts MailAccount object to string in format:
//#      *  EntryName: entry value\n ....
//#      */
//#     public String toString() {
//#     	String result = new String();
//# 
//#         result += MailAccount.CLASS_TYPE_ID_STRING + CLASS_TYPE_STRING + "\n";
//#     	
//#     	if (this.isActive())
//#     		result += "Active: 1\n";
//#     	else
//#     		result += "Active: 0\n";
//#     	result += "Email: "+this.email + "\n";
//#     	result += "Type: "+String.valueOf(this.type) + "\n";
//#     	if (this.SSL)
//#     		result += "SSL: 1\n";
//#     	else
//#     		result += "SSL: 0\n";
        //#ifdef MUJMAIL_SSL
//#     	result += "SSLType: " + String.valueOf(this.SSLType) + "\n";
        //#endif
//#         result += "Server: " + this.server + "\n";
//#     	result += "Port: " + String.valueOf(this.port) + "\n";
//#     	result += "User: " + this.userName + "\n";
//#     	result += "IMAPPrimaryBox: " + this.IMAPPrimaryBox + "\n";
//#         result += "CopyToSrvSent: " + (this.copyToSrvSent?"0":"1") + "\n";
//#     	result += "CopyToSrvSentFolderName: " + this.copyToSrvSentFolderName + "\n";
//#     	result += "CopyToSrvTrash: " + (this.copyToSrvTrash?"0":"1") + "\n";
//#     	result += "CopyToSrvTrashFolderName: " + this.copyToSrvTrashFolderName + "\n\n";
//#         
//#     	return result;
//#     }
    //#endif

    //#ifdef MUJMAIL_SYNC
    /**
     * Get value of next entry in string.
     * 
     * @param str String where entry value to found.
     * @return string that begin with next entry value
     */
    public static String getNextValue(String str) {
   	
    	//There has to be space in the string, so we shouldn't get any errors
    	str = str.substring(str.indexOf(' ') + 1);
    	
    	return str;
    }
    //#endif
    
    //#ifdef MUJMAIL_SYNC
    /**
     * This function parses account data string which is received from mujMail server.
     * <p>Format of string: line separated entries in format EntryName: EntryValue
     * <p>
     * <p>Note: Depends on order of entries
     * @param acctStr	Account data string to be parsed
     * @return			Account data object created from given string
     */
    public static MailAccount parseAccountString(String acctStr)
    {
        acctStr = getNextValue(acctStr);
    	if (!CLASS_TYPE_STRING.equalsIgnoreCase( acctStr.substring(0, acctStr.indexOf('\n')))) {
            System.out.println(SOURCE_FILE+":parseAccountString - invalid handling class");
            return null;
        }
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);

        MailAccountPrimary result = new MailAccountPrimary();


        acctStr = getNextValue(acctStr);
    	result.setActive("1".equals(acctStr.substring(0, acctStr.indexOf('\n'))) ? true : false);
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	
    	acctStr = getNextValue(acctStr);
    	result.email = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	
    	acctStr = getNextValue(acctStr);
    	result.type = Byte.parseByte(acctStr.substring(0, acctStr.indexOf('\n')));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	
    	acctStr = getNextValue(acctStr);
    	result.SSL = "1".equals(acctStr.substring(0, acctStr.indexOf('\n'))) ? true : false;
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
		
    	//#ifdef MUJMAIL_SSL
        acctStr = getNextValue(acctStr);
    	result.SSLType = Byte.parseByte(acctStr.substring(0, acctStr.indexOf('\n')));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	//#endif

        acctStr = getNextValue(acctStr);
    	result.server = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	
    	acctStr = getNextValue(acctStr);
    	result.port = Short.parseShort(acctStr.substring(0, acctStr.indexOf('\n')));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	
    	acctStr = getNextValue(acctStr);
    	result.userName = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
    	    	
    	acctStr = getNextValue(acctStr);
    	result.IMAPPrimaryBox = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);

        acctStr = getNextValue(acctStr);
    	result.setCopyToSrvSent("1".equals(acctStr.substring(0, acctStr.indexOf('\n'))) ? true : false);
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);

        acctStr = getNextValue(acctStr);
    	result.copyToSrvSentFolderName = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);

        acctStr = getNextValue(acctStr);
    	result.setCopyToSrvTrash("1".equals(acctStr.substring(0, acctStr.indexOf('\n'))) ? true : false);
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);

    	acctStr = getNextValue(acctStr);
    	result.copyToSrvTrashFolderName = acctStr.substring(0, acctStr.indexOf('\n'));
    	acctStr = acctStr.substring(acctStr.indexOf('\n') + 2);
        
        return result;
    }
    //#endif

    public boolean isIMAP() {
        return (type == IMAP);
    }
    
    public boolean isPOP3() {
        return (type == POP3);
    }

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
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getServer() {
        return server;
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public boolean isSSL() {
        return SSL;
    }

    public byte getSSLType() {
        return SSLType;
    }
    
    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getIMAPPprimaryBox() {
        return IMAPPrimaryBox;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public void setSSL(boolean SSL) {
        this.SSL = SSL;
    }

    //#ifdef MUJMAIL_SSL
    public void setSSLType(byte SSLType) {
        this.SSLType = SSLType;
    }
    //#endif

    public void setServer(String server) {
        this.server = server;
    }

    public void setProtocol(InProtocol protocol) {
        this.protocol = protocol;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the copyToSrvTrash
     */
    public boolean isCopyToSrvTrash() {
        return copyToSrvTrash && isIMAP();
    }

    /**
     * @param copyToSrvTrash the copyToSrvTrash to set
     */
    public void setCopyToSrvTrash(boolean copyToSrvTrash) {
        this.copyToSrvTrash = copyToSrvTrash;
    }
    
    public boolean isCopyToSrvSent() {
        return copyToSrvSent && isIMAP();
    }

    public void setCopyToSrvSent(boolean copyToSrvSent) {
        this.copyToSrvSent = copyToSrvSent;
    }

    public String getCopyToSrvSentFolderName() {
        return copyToSrvSentFolderName;
    }
    
    public void setCopyToSrvSentFolderName(String copyToSrvSentFolderName) {
        this.copyToSrvSentFolderName = copyToSrvSentFolderName;
    }
    
    public String getCopyToSrvTrashFolderName() {
        return copyToSrvTrashFolderName;
    }

    public void setCopyToSrvTrashFolderName(String copyToSrvTrashFolderName) {
        this.copyToSrvTrashFolderName = copyToSrvTrashFolderName;
    }
    
    public byte getAccountClassType() {
        return MailAccount.ACCOUNT_CLASS_TYPE_PRIMARY;
    }
    
}
