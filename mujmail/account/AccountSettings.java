/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2006 Martin Stefan <martin.stefan@centrum.cz>
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import mujmail.Lang;
import mujmail.MujMail;
import mujmail.MyAlert;
import mujmail.Properties;
import mujmail.Settings;
import mujmail.protocols.IMAP4;
import mujmail.protocols.POP3;
import mujmail.protocols.SMTP;
//#ifdef MUJMAIL_SSL
import mujmail.ui.SSLTypeChooser;
//#endif
/**
 * Cares about administration of the account.
 * Offers the form for creating new account, 
 * the settings reads and stores into
 * the database.
 */
public class AccountSettings extends Form implements Runnable, ItemStateListener {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false; /// Debugging output for this file
    
    /** Standard TCP port for POP communication */
    public static final int CONST_PORT_POP = 110;
    /** Standard TCP port for POP communication secured by SSL */
    public static final int CONST_PORT_POPS = 995;
    /** Standard TCP port for IMAP communication */
    public static final int CONST_PORT_IMAP = 143;
    /** Standard TCP port for IMAP communication secured by SSL */
    public static final int CONST_PORT_IMAPS = 993;

    private ChoiceGroup active;
    private TextField email;
    private ChoiceGroup protocolType;
    private TextField inboxServer;
    private TextField inboxPort;
    private TextField inboxAuthName;
    private TextField inboxAuthPass;
    private TextField IMAP_boxes;
    private ChoiceGroup SSL;
    //#ifdef MUJMAIL_SSL
    private SSLTypeChooser sslTypeChooser;
    //#endif
    private ChoiceGroup copyToServer;
    private TextField copyToSrvSentFolderName;
    private TextField copyToSrvTrashFolderName;
    private boolean  copyToSrvSentFolderNameVisible = false;
    private boolean  copyToSrvTrashFolderNameVisible = false;

    public Command back, ok;
    private MujMail mujMail;
    private boolean editting = false;

    /** Marks if loading databases */
    private boolean busy;
    /** Marks if loading of accounts had been finnished */
    private boolean accountsLoaded = false;
    /** Object on which wait fo beeing notify */
    private Object notifier = new Object(); 

    /**
     * Constructor of the class
     * @param mujMail - main application
     */
    public AccountSettings(MujMail mujMail) {
        super("Account form");
        this.mujMail = mujMail;

        active = new ChoiceGroup(Lang.get(Lang.AS_ACTIVATION), Choice.MULTIPLE);
	    active.append( Lang.get(Lang.ACTIVE), null); 
        if ( Properties.textFieldMailIncorrect ) {
            email = new TextField(Lang.get(Lang.AS_EMAIL), "", 50, TextField.ANY);
        } else {
            email = new TextField(Lang.get(Lang.AS_EMAIL), "", 50, TextField.EMAILADDR);
        }
        protocolType = new ChoiceGroup(Lang.get(Lang.AS_PROTOCOL), Choice.EXCLUSIVE);
        protocolType.append("POP3", null);
        protocolType.append("IMAP4", null);
        protocolType.setSelectedIndex(0, true);
        inboxServer = new TextField(Lang.get(Lang.AS_SERVER), "", 50, TextField.ANY);
        inboxPort = new TextField(Lang.get(Lang.AS_PORT), "", 50, TextField.NUMERIC);
        inboxAuthName = new TextField(Lang.get(Lang.AS_USR_NAME), "", 50, TextField.ANY);
        inboxAuthPass = new TextField(Lang.get(Lang.AS_PASS), "", 50, TextField.PASSWORD);
        IMAP_boxes = new TextField(Lang.get(Lang.AS_IMAP_MAILBOXES), "", 50, TextField.ANY);
        SSL = new ChoiceGroup("SSL", Choice.MULTIPLE);
        SSL.append(Lang.get(Lang.AS_SSL), null);

        //#ifdef MUJMAIL_SSL
        sslTypeChooser = new SSLTypeChooser(this, 4);
        //#endif

        copyToServer = new ChoiceGroup(Lang.get(Lang.AS_COPY_TO_SERVER), Choice.MULTIPLE);
        copyToServer.append( Lang.get(Lang.AS_COPY_TO_SRV_SENT), null);
        copyToServer.append( Lang.get(Lang.AS_COPY_TO_SRV_TRASH), null);
        copyToServer.setLabel(Lang.get(Lang.AS_COPY_TO_SERVER));
        
        copyToSrvSentFolderName  = new TextField(Lang.get(Lang.AS_COPY_TO_SRV_SENT_MAILBOX), "" ,1000, TextField.ANY);
        copyToSrvSentFolderName.setLabel(Lang.get(Lang.AS_COPY_TO_SRV_SENT_MAILBOX));
        copyToSrvTrashFolderName = new TextField(Lang.get(Lang.AS_COPY_TO_SRV_TRASH_MAILBOX), "", 1000, TextField.ANY);
        copyToSrvTrashFolderName.setLabel(Lang.get(Lang.AS_COPY_TO_SRV_TRASH_MAILBOX));
        
        back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        ok = new Command(Lang.get(Lang.BTN_OK), Command.OK, 1);


        addCommand(ok);
        addCommand(back);
        setItemStateListener(this);
        setCommandListener(mujMail);
    }

    
    /**
     * Updates current view according settings changes made by user.
     * <p>Actualize protocol ports, SSL settings, IMAP specific setting (folders, ...)
     *
     * @param item Item that was changed
     */
    public void itemStateChanged(Item item) {
        int portOld = 0;
        try {
           portOld = Integer.parseInt(inboxPort.getString());
        } catch (Exception e) {
            System.out.println( "WARN  AccountSettings.itemStateChanged(Item) - unable to parse integer from string '" + inboxPort.getString() + "'" );
            e.printStackTrace();
        }

        if (item == protocolType) {
            if (protocolType.getSelectedIndex() == 0) { //pop3 selected
                // Protocol ports customization
                if (SSL.isSelected(0) == false) { // imap->pop3
                    if (portOld == CONST_PORT_IMAP) inboxPort.setString( Integer.toString(CONST_PORT_POP) );
                } else { // imaps->pop3s
                    if (portOld == CONST_PORT_IMAPS) inboxPort.setString( Integer.toString(CONST_PORT_POPS) );
                }

                int index = -1;
                for ( int i = 0; i < size(); i++) 
                    if (this.get(i).getLabel().equals(Lang.get(Lang.AS_IMAP_MAILBOXES))) index = i;
                if (index != -1) delete(index); //delete IMAP_boxes

                // MailBoxes setting
                int ctsIndex = -1; // CopyToServer ChoiceGroup index
                for ( int i = 0; i < size(); i++) 
                    if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SERVER))) ctsIndex = i;
                if (ctsIndex != -1) 
                    delete( ctsIndex);
                // remove all visible, then adds  visible one ... its necessary not to change order of 
                if (copyToSrvSentFolderNameVisible == true) {
                    int index1 = -1; // CopyToServer ChoiceGroup index
                    for ( int i = 0; i < size(); i++) 
                        if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SRV_SENT_MAILBOX))) index1 = i;
                    delete(index1);
                    copyToSrvSentFolderNameVisible = false;
                }
                if (copyToSrvTrashFolderNameVisible == true) {
                    int index1 = -1; // CopyToServer ChoiceGroup index
                    for ( int i = 0; i < size(); i++) 
                        if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SRV_TRASH_MAILBOX))) index1 = i;
                    delete(index1);
                    copyToSrvTrashFolderNameVisible = false;
                }
            } else { // imap selected
                // Protocol ports customization
                if (SSL.isSelected(0) == false) { // pop3->imap
                    if (portOld == CONST_PORT_POP) inboxPort.setString( Integer.toString(CONST_PORT_IMAP) );
                } else { // pop3s->imaps
                    if (portOld == CONST_PORT_POPS) inboxPort.setString( Integer.toString(CONST_PORT_IMAPS) );
                }

                int index = -1;
                for ( int i = 0; i < size(); i++) {
                    if ( this.get(i).getLabel().equals(Lang.get(Lang.AS_PASS)) ) {
                        index = i;
                    }
                }
                index +=1;
                insert(index, IMAP_boxes);
                index +=1;
                insert(index, copyToServer);
                if (copyToServer.isSelected(0)) {
                    index +=1;
                    insert(index, copyToSrvSentFolderName);
                    copyToSrvSentFolderNameVisible = true;
                }
                if (copyToServer.isSelected(1)) {
                    index +=1;
                    insert(index, copyToSrvTrashFolderName);
                    copyToSrvTrashFolderNameVisible = true;
                }
            }
        }

        if (item == SSL) {
            if (SSL.isSelected(0)) {
                //#ifdef MUJMAIL_SSL
                sslTypeChooser.insertToForm();
                //#endif
                // Protocol ports customization
                if (protocolType.isSelected(0)) { // pop3->pop3s
                    if (portOld == CONST_PORT_POP) inboxPort.setString( Integer.toString(CONST_PORT_POPS) );
                } else { // imap->imaps
                    if (portOld == CONST_PORT_IMAP) inboxPort.setString( Integer.toString(CONST_PORT_IMAPS) );
                }
            } else {
                //#ifdef MUJMAIL_SSL
                delete(4);
                //#endif
                // Protocol ports customization
                if (protocolType.isSelected(0)) { // pop3s->pop3
                    if (portOld == CONST_PORT_POPS) inboxPort.setString( Integer.toString(CONST_PORT_POP) );
                } else { // imaps->imap
                    if (portOld == CONST_PORT_IMAPS) inboxPort.setString( Integer.toString(CONST_PORT_IMAP) );
                }
            }
        }

        // Showing and removing server mailbox names as needed
        if ( item == copyToServer) {

            int ctsIndex = -1; // CopyToServer ChoiceGroup index
            for ( int i = 0; i < size(); i++) 
                if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SERVER))) ctsIndex = i;

            // remove all visible, then adds  visible one ... its necessary not to change order of 
            if (copyToSrvSentFolderNameVisible == true) {
                int index = -1; // CopyToServer ChoiceGroup index
                for ( int i = 0; i < size(); i++) 
                    if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SRV_SENT_MAILBOX))) index = i;
                delete(index);
                copyToSrvSentFolderNameVisible = false;
            }
            if (copyToSrvTrashFolderNameVisible == true) {
                int index = -1; // CopyToServer ChoiceGroup index
                for ( int i = 0; i < size(); i++) 
                    if (this.get(i).getLabel().equals(Lang.get(Lang.AS_COPY_TO_SRV_TRASH_MAILBOX))) index = i;
                delete(index);
                copyToSrvTrashFolderNameVisible = false;
            }

            if (copyToServer.isSelected(0)) {
                ctsIndex +=1;
                insert(ctsIndex,copyToSrvSentFolderName);
                copyToSrvSentFolderNameVisible = true;
            }
            if (copyToServer.isSelected(1)) {
                ctsIndex +=1;
                insert(ctsIndex,copyToSrvTrashFolderName);
                copyToSrvTrashFolderNameVisible = true;
            }

        }
        if (item == inboxServer) {
            if (inboxServer.getString().equalsIgnoreCase(MailAccount.CONST_GMAIL_IMAP_SERVER)) {
                if (copyToSrvSentFolderName.getString().equalsIgnoreCase(MailAccount.CONST_IMAP_SERVER_SENTBOX))  copyToSrvSentFolderName.setString( MailAccount.CONST_GMAIL_SENTBOX);
                if (copyToSrvTrashFolderName.getString().equalsIgnoreCase(MailAccount.CONST_IMAP_SERVER_TRASHBOX)) copyToSrvTrashFolderName.setString( MailAccount.CONST_GMAIL_TRASHBOX);
            } else {
                if (copyToSrvSentFolderName.getString().equalsIgnoreCase(MailAccount.CONST_GMAIL_SENTBOX))    copyToSrvSentFolderName.setString(MailAccount.CONST_IMAP_SERVER_SENTBOX);
                if (copyToSrvTrashFolderName.getString().equalsIgnoreCase(MailAccount.CONST_GMAIL_TRASHBOX )) copyToSrvTrashFolderName.setString(MailAccount.CONST_IMAP_SERVER_TRASHBOX );
            }
        }
    }

    /** 
     * @return true while loading accounts from database.
     */
    public boolean isBusy() {
        return busy;
    }

    /**
     * A method that shows a form for editing an account
     * @param accountID Identification of the account
     */
    public void showAccount(String accountID) {
        MailAccount account;
        if (accountID != null && !accountID.equals("DEFAULT_INIT")) {
            editting = true;
            account = (MailAccount) mujMail.getMailAccounts().get(accountID);
        } else {
            editting = false;
            account = new MailAccountPrimary(
                    MailAccount.IMAP, "your_email@gmail.com", true, "imap.gmail.com",
                    (short) 993, "your_email@gmail.com", "", true, (byte) 1, true, false,
                    MailAccount.CONST_IMAP_SERVER_SENTBOX,
                    MailAccount.CONST_IMAP_SERVER_TRASHBOX);
        }
        // clear form
        deleteAll();
        copyToSrvSentFolderNameVisible = false;
        copyToSrvTrashFolderNameVisible = false;
   
        active.setSelectedIndex(0, account.isActive());
        email.setString(account.getEmail());
        protocolType.setSelectedIndex(account.getType(), true);
        SSL.setSelectedIndex(0,account.isSSL());
        //#ifdef MUJMAIL_SSL
        sslTypeChooser.setSelectedType(account.getSSLType());
        //#endif
        inboxServer.setString(account.getServer());
        inboxPort.setString(String.valueOf(account.getPort()));
        inboxAuthName.setString(account.getUserName());
        inboxAuthPass.setString(account.getPassword());
        IMAP_boxes.setString(account.getIMAPPprimaryBox());
        copyToServer.setSelectedIndex(0, account.isCopyToSrvSent());
        copyToServer.setSelectedIndex(1,account.isCopyToSrvTrash());
        copyToSrvSentFolderName.setString(account.getCopyToSrvSentFolderName());
        copyToSrvTrashFolderName.setString(account.getCopyToSrvTrashFolderName());

        // append all needed
        append(active);
        append(email);
        append(protocolType);
        append(SSL);
        append(inboxServer);
        append(inboxPort);
        append(inboxAuthName);
        append(inboxAuthPass);
        if (account.getType() == MailAccount.IMAP ) append(IMAP_boxes);

        //#ifdef MUJMAIL_SSL
        if (account.isSSL()) {
            sslTypeChooser.insertToForm();
        }
        //#endif
        if (protocolType.getSelectedIndex() == 1) {
            append(copyToServer);
            if (copyToServer.isSelected(0)) {
                append(copyToSrvSentFolderName);
                copyToSrvSentFolderNameVisible = true;
            }
            if (copyToServer.isSelected(1)) {
                append(copyToSrvTrashFolderName);
                copyToSrvTrashFolderNameVisible = true;
            }
        }

        // init with default values
        mujMail.getDisplay().setCurrent(this);
        if (accountID != null && accountID.equals("DEFAULT_INIT")) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AS_SET_DEFAULT_ACCOUNT), MyAlert.DEFAULT, AlertType.INFO);
        }
    }

    /**
     * Connects to "ACCOUNTS" <code>RecordStore</code> and returns a count of records in it.
     * <p>It is used before <code>loadAccounts()</code> method is called.
     * 
     * @return Account count stored in persistent database
     */
    public int getNumAccounts() {
        RecordStore rs;
        int numAccounts = 0;
        try {
            rs = RecordStore.openRecordStore("ACCOUNTS", true);
            numAccounts = rs.getNumRecords();
            rs.closeRecordStore();
        } catch (RecordStoreException ex) {
            ex.printStackTrace();
        }

        return numAccounts;
    }

    /**
     * This procedure serves to save new or edited Account from the form to the
     * <code> RecordStore </code> and refreshes the <code> Account </code> array
     * as well.
     * @param accountID name of account
     * @param account Account to save into database. If null content of current form is used.
     */
    public void saveAccount(String accountID, MailAccount account) {

    	//the following check is done only in case a new account is
    	//created using the account form; should not be performed
    	//for settings synchronization (account == null) condition
    	//achieves this
        if (!editting && account == null && mujMail.getMailAccounts().containsKey(email.getString())) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AS_ALREADY_EXITS) + email.getString(), MyAlert.DEFAULT, AlertType.INFO);
            return;
        }

        /**
         * Initialize a new account in case Ok is pressed 
         * in AccountSettings form (in this case account is NULL)
         */        
        if (account == null) {
	        account = new MailAccountPrimary();
	        account.setActive( active.isSelected(0) );
	        account.setEmail( email.getString() );
	        account.setSSL(SSL.isSelected(0));
                //#ifdef MUJMAIL_SSL
                account.setSSLType((byte) sslTypeChooser.getSSLTypeNumberChosen());
                //#endif
	        account.setType((byte) protocolType.getSelectedIndex());
	        account.setServer(inboxServer.getString());
	        try {
	            account.setPort(Short.parseShort(inboxPort.getString()));
	        } catch (Exception ex) {
	            inboxPort.setString("110");
	            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_SAVING) + account.getEmail() + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
	            return;
	        }
	        account.setUserName(inboxAuthName.getString());
	        account.setPassword(inboxAuthPass.getString());
	        if (account.getType() == MailAccount.POP3) {
	            account.setProtocol(new POP3(account));
	        } else {
	            account.setIMAPPrimaryBox( IMAP_boxes.getString() );
	            account.setProtocol(new IMAP4(account));
	        }
	        account.setCopyToSrvSent(copyToServer.isSelected(0));
	        account.setCopyToSrvTrash(copyToServer.isSelected(1));
	        account.setCopyToSrvSentFolderName(copyToSrvSentFolderName.getString());
	        account.setCopyToSrvTrashFolderName(copyToSrvTrashFolderName.getString());
        }

        // save to RecordStore
        try {
            RecordStore rs = RecordStore.openRecordStore("ACCOUNTS", true);
            
            //this variable is used when the account is restored
            //through Restore settings command
            boolean accountExists = mujMail.getMailAccounts().containsKey(account.getEmail());
            
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(buffer);
                if (editting) {
                    account.setRecordID( ((MailAccount) mujMail.getMailAccounts().get(accountID)).getRecordID() );
                }
                else if (accountExists) {
                	account.setRecordID( ((MailAccount) mujMail.getMailAccounts().get(account.getEmail())).getRecordID() );
                }
                stream.writeByte(account.getAccountClassType());
                stream.writeBoolean(account.isActive());
                stream.writeUTF(account.getEmail());
                stream.writeByte(account.getType());
                stream.writeBoolean(account.isSSL());
                //#ifdef MUJMAIL_SSL
                stream.writeByte(account.getSSLType());
                //#endif
                stream.writeUTF(account.getServer());
                stream.writeShort(account.getPort());
                stream.writeUTF(account.getUserName());
                stream.writeUTF(account.getPassword());

                if (account.getType() == MailAccount.IMAP) {
                    stream.writeUTF(account.getIMAPPprimaryBox());
                }
                stream.writeBoolean(account.isCopyToSrvSent());
                stream.writeBoolean(account.isCopyToSrvTrash());
                stream.writeUTF(account.getCopyToSrvSentFolderName());
                stream.writeUTF(account.getCopyToSrvTrashFolderName());

                stream.flush();
                /*for now we dont use setRecord as it seems buggy
                if (editting) {
                rs.setRecord(account.recordID, buffer.toByteArray(), 0, buffer.size());
                //even the accountID (email) was changed, remove the old one to update the new AccoundID
                mujMail.mailAccounts.remove(accountID);
                }
                else
                account.recordID = rs.addRecord(buffer.toByteArray(), 0, buffer.size());
                 */
                int oldIndex = account.getRecordID();
                account.setRecordID( rs.addRecord(buffer.toByteArray(), 0, buffer.size()) );
                
                //Delete the existing record if the saving is 
                //done from account form or from Sync class
                if (editting || accountExists) {
                    rs.deleteRecord(oldIndex);
                    //even the accountID (email) was changed, remove the old one to update the new AccoundID
                    mujMail.getMailAccounts().remove(accountID);
                }

                mujMail.getMailAccounts().put(account.getEmail(), account);
                //change settings primary email account if necessary
                if (account.isActive() && !mujMail.getMailAccounts().containsKey(Settings.primaryEmail)) {
                    Settings.primaryEmail = account.getEmail();
                    mujMail.getSettings().saveSettings(true);
                } else if (!account.isActive() && Settings.primaryEmail.equals(account.getEmail())) {
                    Settings.primaryEmail = Settings.notSetPE;
                    mujMail.getSettings().saveSettings(true);
                }
                
                // Active account list may be changed. Update Inbox retrieve account list
                mujMail.getInBox().actualizeActiveAccountList();
                
                stream.close();
                buffer.close();
            } catch (Exception ex) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_SAVING) + account.getEmail() + Lang.get(Lang.FAILED) + " " + ex, MyAlert.DEFAULT, AlertType.ERROR);
            }
            rs.closeRecordStore();
            mujMail.mainMenu();
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_SAVING) + account.getEmail() + Lang.get(Lang.FAILED) + " " + ex, MyAlert.DEFAULT, AlertType.ERROR);
        }
    }

    /**
     * Removes specified mail account from persistent <code>RecordStore</code> accounts database.
     *
     * @param accountID Account to remove
     * @param sure if false shows alert
     */
    public void deleteAccount(String accountID, boolean sure) {
        if (!sure) {
            mujMail.alert.setAlert(this,mujMail.getMenu(), Lang.get(Lang.ALRT_SYS_DEL_CONFIRM) + accountID + "?", MyAlert.DEL_CONFIRM, AlertType.CONFIRMATION);
            return;
        }
        try {
            RecordStore rs = RecordStore.openRecordStore("ACCOUNTS", true);
            try {
                rs.deleteRecord(((MailAccount) mujMail.getMailAccounts().get(accountID)).getRecordID());
                mujMail.getMailAccounts().remove(accountID);
                //if this account was primary account, reset primary account to default
                if (Settings.primaryEmail.equals(accountID)) {
                    Settings.primaryEmail = Settings.notSetPE;
                    mujMail.getSettings().saveSettings(true);
                }

            } catch (Exception ex) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_DELETING) + accountID + Lang.get(Lang.FAILED) + " " + ex, MyAlert.DEFAULT, AlertType.ERROR);
            }
            rs.closeRecordStore();
        } catch (Exception rse) {
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_DELETING) + accountID + Lang.get(Lang.FAILED) + " " + rse, MyAlert.DEFAULT, AlertType.ERROR);
        }
        // refresh menu
        mujMail.mainMenu();
    }

    /**
     * Removes all mail accounts from persistent <code>RecordStore</code> database.
     *
     * @param sure if false shows alert
     */
    public void deleteAll(boolean sure) {
        if (!sure) {
            mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_SYS_DEL_ALL_CONFIRM), MyAlert.DB_CLEAR_CONFIRM, AlertType.CONFIRMATION);
            return;
        }
        try {
            RecordStore.deleteRecordStore("ACCOUNTS");
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_AD_DELETE) + Lang.get(Lang.FAILED) + " " + ex, MyAlert.DEFAULT, AlertType.ERROR);
            return;
        }
        Settings.primaryEmail = Settings.notSetPE;
        mujMail.getMailAccounts().clear();
        mujMail.mainMenu();
    }

    /**
     * Starts loading of account from persisten database.
     * 
     * <p>Note: Loading of mail is done in separate thread.
     * <p>Note: 
     */
    public void loadAccounts() {
        accountsLoaded = false;
        Thread t = new Thread(this);
        t.start();
        t.setPriority(Thread.MAX_PRIORITY);
    }

    /** 
     * Load accounts in background.
     * Note: Used by {@link #loadAccounts()}
     */
    public void run() {
        busy = true;
        try {
            // First run create primary mail accounts, second Derived accounts
            RecordStore rs = RecordStore.openRecordStore("ACCOUNTS", true);
            int id, sizeOfRecord;
            byte[] data = new byte[70];

            try {
                if (DEBUG) System.out.println("DEBUG AccountSettings.run() - loading accounts");
                
                RecordEnumeration en = rs.enumerateRecords(null, null, false);
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
                while (en.hasNextElement()) {
                    try {
                        id = en.nextRecordId();
                        sizeOfRecord = rs.getRecordSize(id);
                        if (sizeOfRecord > data.length) {
                            data = new byte[sizeOfRecord + 30];
                            inputStream = new DataInputStream(new ByteArrayInputStream(data));
                        }
                        rs.getRecord(id, data, 0);
                        inputStream.reset();

                        inputStream.readByte();
                        // Only primary accounts now
                        MailAccount account = new MailAccountPrimary();
                        account.setRecordID( id );
                        account.setActive( inputStream.readBoolean() );
                        account.setEmail( inputStream.readUTF() );
                        account.setType( inputStream.readByte() );
                        account.setSSL(inputStream.readBoolean());
                        //#ifdef MUJMAIL_SSL
                        account.setSSLType(inputStream.readByte());
                        //#endif
                        account.setServer(inputStream.readUTF());
                        account.setPort(inputStream.readShort());
                        account.setUserName(inputStream.readUTF());
                        account.setPassword(inputStream.readUTF());
                        if (account.getType() == MailAccount.POP3) {
                            account.setProtocol(new POP3(account));
                        } else {
                            account.setIMAPPrimaryBox( inputStream.readUTF() );
                            account.setProtocol(new IMAP4(account));
                        }
                        account.setCopyToSrvSent(inputStream.readBoolean());
                        account.setCopyToSrvTrash(inputStream.readBoolean());
                        account.setCopyToSrvSentFolderName(inputStream.readUTF());
                        account.setCopyToSrvTrashFolderName(inputStream.readUTF());
                        mujMail.getMailAccounts().put(account.getEmail(), account);
                    } catch (Exception ex) {
                        if (DEBUG) {
                            System.out.println("Loading account failed");
                            System.out.println(ex);
                            ex.printStackTrace();
                        }
                        //try another one
                    }
                } // end While
                inputStream.close();
            } catch (Exception ex) {
                if (DEBUG) { ex.printStackTrace(); }
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AS_LOAD) + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
            } catch (Error er) {
                if (DEBUG) { er.printStackTrace(); }
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AS_LOAD) + Lang.get(Lang.FAILED) + ": " + er, MyAlert.DEFAULT, AlertType.ERROR);
            }
            data = null;
            if (DEBUG) System.out.println("DEBUG AccountSettings.run() - loading accounts..successfull");
            SMTP.getSMTPSingleton(mujMail).initAccount(); //now accounts should be ready?
            rs.closeRecordStore();
        } catch (Exception ex) {
            if (DEBUG) { ex.printStackTrace(); }
            mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_AS_LOAD) + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
        }
        busy = false;
        if (DEBUG) { System.out.println("DEBUG AccountSettings.run() ... account successfully loaded"); }

        // Set accounts to be retrieved by inbox
        mujMail.getInBox().actualizeActiveAccountList();

        synchronized(notifier) {
            accountsLoaded = true;
            notifier.notifyAll();
        }
    }
    
    /** 
     * Blocks and wait all accounts are loaded.
     *
     * <p>Note: Return immediately if all accounts were loaded before calling of this method.
     */
    public void waitForAccountsLoading() {
        if (DEBUG) { System.out.println("DEBUG AccountSettings.waitForAccountsLoading .. in"); }
        try {
            synchronized(notifier) {
                if (accountsLoaded) return;
                notifier.wait();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        if (DEBUG) { System.out.println("DEBUG AccountSettings.waitForAccountsLoading .. out"); }
    }
}
