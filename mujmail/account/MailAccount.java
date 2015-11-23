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
import mujmail.protocols.InProtocol;

/**
 * Represents POP3 or IMAP4 account.
 * <p>Hold information needed for creating connection to server and other mailbox specific informations.
 * <p>Mail account is like structure with getter and setter methods.
 *
 */
public interface MailAccount {
    /** Marks that account connect to POP3 server when connection - POP3 account. */
    public static final byte POP3 = 0;
    /** Marks that account connect to IMAP server when connection - IMAP account. */
    public static final byte IMAP = 1;

    /** Imap server used by Google mail */
    static final String CONST_GMAIL_IMAP_SERVER = "imap.gmail.com";
    /** Special mail folder where gmail stores sent mails */
    static final String CONST_GMAIL_SENTBOX = "[Gmail]/Sent Mail";
    /** Special mail folder where gmail stores deleted mails */
    static final String CONST_GMAIL_TRASHBOX = "[Gmail]/Trash";
    /** Special mail folder where ordinary imap servers store sent mails */
    static final String CONST_IMAP_SERVER_SENTBOX = "sent";
    /** Special mail folder where ordinary imap servers store deleted mails */
    static final String CONST_IMAP_SERVER_TRASHBOX = "trash";

    /* There are 2 type of accounts mail account defined a visible to user (primary, MailAccountClass)
     * and invisible account for internal purposes of user boxes  (derived, MailAccountDerive) wich is slightly modified reference to existing account)
     */
    /** Mail class type. Primary mail account - information holder. See {@link #getAccountClassType()} */
    public static final byte ACCOUNT_CLASS_TYPE_PRIMARY = 1; 
    //#ifdef MUJMAIL_USR_FOLDERS
    /** Mail class type. Derived mail account. See {@link #getAccountClassType()} */
    public static final byte ACCOUNT_CLASS_TYPE_DERIVED = 2;
    //#endif
    
    //#ifdef MUJMAIL_SYNC
    /** Entry name that holds AccountClassType value in string representation (synchronizing) */
    public static final String CLASS_TYPE_ID_STRING = "AccountClassType: ";
    //#endif
    
    /**
     * Retruns true if this account is imap account.
     * @return true if IMAP account
     */
    public boolean isIMAP();
    
    /**
     * Retruns true if this account is POP3 account.
     * @return true id POP3 account
     */
    public boolean isPOP3();
    
    //#ifdef MUJMAIL_SYNC
    /**
     * This method is used when "synchronize with server" is invoked.
     * 
     * @return string representation of account for synchronization
     */
    public String serialize();
    //#endif
        

    /**
     * Prepares the account for pushing.
     * @param box Box where store new imail.
     */
    public void prepareForPushing(InBox box);
    
    /**
     * Create "push" connection and wait (check) for new mails.
     */
    public void startPushing();

    public String getEmail();
    void setEmail(String email);

    public String getServer();
    void setServer(String server);

    public short getPort();
    public void setPort(short port);

    public boolean isSSL();
    void setSSL(boolean SSL);

    public byte getSSLType();
    //#ifdef MUJMAIL_SSL
    void setSSLType(byte SSLType);
    //#endif

    public String getUserName();
    void setUserName(String userName);

    public String getPassword();
    void setPassword(String password);

    public String getIMAPPprimaryBox();
    void setIMAPPrimaryBox(String IMAPPrimaryBox);

    public byte getType();
    void setType(byte type);

    /**
     * Gets protocol instance that can be used for communication 
     *  with mail server described in account
     * @return Protocol object
     */
    public InProtocol getProtocol();
    void setProtocol(InProtocol protocol);

    public int getRecordID();
    public void setRecordID(int recordID);

    /** 
     * Active accounts are retrieved into standard inbox folder
     * @return if true mail account should be retrieved (into standard inbox folder).
     */
    public boolean isActive();
    public void setActive(boolean active);

    public boolean isCopyToSrvTrash();

    public void setCopyToSrvTrash(boolean copyToSrvTrash);
    
    public boolean isCopyToSrvSent();
    public void setCopyToSrvSent(boolean copyToSrvSent);

    public String getCopyToSrvSentFolderName();
    public void setCopyToSrvSentFolderName(String copyToSrvSentFolderName);
    
    public String getCopyToSrvTrashFolderName();
    public void setCopyToSrvTrashFolderName(String copyToSrvTrashFolderName);
    
    /**
     * Instance type identifier.
     * @return type of account class. Is used to distinguis beetween successors.
     *
     * <p>see{@link MailAccountPrimary#ACCOUNT_CLASS_TYPE_PRIMARY definition}
     */
    public byte getAccountClassType();
    
}
