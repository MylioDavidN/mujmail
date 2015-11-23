package mujmail;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;

import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.PersistentBox;
import mujmail.account.MailAccount;
import mujmail.account.MailAccountPrimary;
import mujmail.ordering.ComparatorStrategy;
import mujmail.ordering.Criterion;
import mujmail.ordering.Ordering;
import mujmail.protocols.InProtocol;
import mujmail.tasks.StoppableProgress;
//#ifdef MUJMAIL_USR_FOLDERS
import mujmail.account.MailAccountDerived;
//#endif

/**
 * Stores mails downloaded from email accounts.
 */
public class InBox extends PersistentBox {
    
    /** The name of this source file */
    private static final String SOURCE_FILE = "InBox";

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public final Command retrieve;
    public final Command redownload;

    /** to map stored mails in the vector storage */
    private Hashtable/*<String, MessageHeader>*/ msgIDs;
    
    boolean syncOK = true; //a flag that during Servers->InBox sync everything went OK 

    //mailsOnServers counts how many mails are currently on servers. 
    //-1 indicates that the state is unknown or synchronization is not run yet
    int mailsOnServers = -1;

    /** Flag indicates whether in box have to be sorted again */
    private boolean needResort;
    private Timer pollTimer;
    private int unreadMails = 0; /// Number of mail not readed by user
    private MessageHeader lastSafeMail;
    private boolean pushActive = false;

//    private ThreadedEmails storage = new ThreadedEmails();

    /** Holds list of (active) accounts to be retrieved into this folder */
    private Vector/*<MailAccount>*/ accounts = new Vector/*<MailAccount>*/();

    //#ifdef MUJMAIL_USR_FOLDERS    
    /** 
     *  Holds index in recordstore userMailBox database, 
     *    where info about this InBox instance take place.
     *  Note: Only for BoxList class purposes.
     *  Note: Used only for user mail boxes.
     */
    private int userBoxListDB_recordID = -1;
    //#endif
    
    /**
     * Creates inbox.
     * 
     * @param DBFile the identifier of RMS database where the mails of this box
     *  will be stored.
     * @param mMail the main object in the application
     * @param name the name of the box
     */
    public InBox(String DBFile, String name) {
        super(DBFile, MujMail.mujmail, name);
        
        //msgIDs = new Hashtable();
        retrieve = new Command(Lang.get(Lang.BTN_RTV_NEW_MAILS), Command.ITEM, 8);
        redownload = new Command(Lang.get(Lang.BTN_TB_REDOWNLOAD), Command.ITEM, 6);
        addCommand(redownload);
        addCommand(retrieve);
        
        //#ifdef MUJMAIL_USR_FOLDERS
        // Load accounts for this folder
        Thread t = new RetrieveBoxAccountsTask(RetrieveBoxAccountsTask.ACCOUNTS_LOAD);
        t.start();
        //#endif
    }

    /**
     * Returns true if the synchronization is running.
     * @return true if the synchronization is running.
     */
    public boolean isSyncRunning() {
        //mailsOnServers counts how many mails are currently on servers. 
        //-1 indicates that the state is unknown or synchronization is not run yet
        return (mailsOnServers != -1);
    }

    public void showBox() {
        // Show us only if paint can show progress with loading DB or inbox content
        if (getMailDB().getDBLoadingTask() != null) {
            super.showBox();
        }
        setCurFirstUnread();
    }

    public boolean isNeedResort() {
        return needResort;
    }

    public void setNeedResort(boolean needResort) {
        this.needResort = needResort;
    }
    
    private class Polling extends TimerTask {

        public void run() {
            for (Enumeration e = getMujMail().getMailAccounts().elements(); e.hasMoreElements();) {
                MailAccount account = (MailAccount) e.nextElement();
                if (account.isPOP3() && account.isActive()) {
                    account.getProtocol().poll(InBox.this);
                }
            }
        }
    }
    
    /*
     * Pushing Thread: Starts new push mail IMAP session for the specified account
     */
    private class Pushing extends Thread{
        private final MailAccount pushAccount;

        public Pushing(MailAccount pushAccount) {
            this.pushAccount = pushAccount;
        }
        
        
        public synchronized void run(){  
            pushAccount.startPushing();
            //pushActive = false;				WHY??? This way pushing won't ever start  
        }
    }
    
    /**
     * If some error during synchronization occurs, set this.
     */
    public void synchronizationError() {
        syncOK = false;
    }
    


    public void exit() {
        if (Settings.delOnExit) {
            deleteMarkedFromBoxAndDB();
        }
        
        super.exit();
    }
    
    

    protected boolean isBusy() {
        if (getMailDB().isBusy() || InProtocol.isBusyGlobal()) {
            return true;
        }

        return false;
    }

    /**
     * If there is polling not active, enables polling, if it is active, 
     * cancels polling.
     */
    public void poll() {
        if (pollTimer == null) {
            pollTimer = new Timer();
            pollTimer.scheduleAtFixedRate(new Polling(), 0, Settings.pollInvl >= 5 ? Settings.pollInvl * 1000 : 5 * 1000);
        } else {
            pollTimer.cancel();
            pollTimer = null;
        }
    }

    /**
     * Increases the number of unread messages.
     * 
     * @param val value to increase number of unread e-mails
     */
    public synchronized void changeUnreadMails(int val) {
        unreadMails += val;
    }

    //init the hash of mails in the vector storage
    public void initHash() {
        if (DEBUG) { System.out.println("DEBUG InBox.initHash() - " + this.getName()); }
        msgIDs = new Hashtable(storage.getSize());
        MessageHeader header;
        Enumeration messages = storage.getEnumeration();
        while (  messages.hasMoreElements() ) {
            header = (MessageHeader) messages.nextElement();
            msgIDs.put(header.getAccountID() + "@" + header.getMessageID(), header);
        }
    }

    /** Checks if we see this many sometime or it's new comming email
     *  @param ID Identification of email
     *  @return true if seen mail
     */
    public boolean wasOnceDownloaded(String accountID, String messageID) {
        if (DEBUG) { System.out.println("DEBUG InBox.wasOnceDownloaded(String, String)"); }
        if (msgIDs.containsKey(accountID + "@" + messageID)) { //if it is in the storage
            return true;
        }
        return mujMail.getMailDBSeen().wasMailSeen(accountID, messageID);
    }

    //increase mailsOnServer counter
    public synchronized void newMailOnServer() {
        if (mailsOnServers != -1) //do it only when synchronization is running
        {
            ++mailsOnServers;
        }
    }

    // TODO (Betlista): integrate threads here
//    public synchronized void addToStorage(MessageHeader message) {
//        if (message != null && storage != null) {
//            storage.addElement(message);
//        }
//    }

    public synchronized void addToMsgIDs(MessageHeader message) {
        if (message != null && msgIDs != null) {
            msgIDs.put(message.getAccountID() + "@" + message.getMessageID(), message);
        }
    }

    public synchronized void addToOnceDownloaded(MessageHeader header) {
        try { //lets mark it as seen
            mujMail.getMailDBSeen().addSeen(header);
        } catch (MyException ex) {
            report(Lang.get(Lang.ALRT_SAVING) + Lang.get(Lang.MSGIDS_CACHE) + Lang.get(Lang.FAILED), SOURCE_FILE);
        }
    }

    //synchronizes content of inbox exactly to the active servers. 
    //so mails that were deleted on the server or mails of inactive accounts will be deleted in the inbox as well
    public void serversSync(StoppableProgress progress) {
        //if something gone wrong during checking mails that are on the server
        if (!syncOK) {
            syncOK = true;
            mailsOnServers = -1; //defaults the value, indicating that sync is not running
            stop(); //stop all operation on servers
            getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_SYNCHRONIZING) + Lang.get(Lang.FAILED) + Lang.get(Lang.ALRT_SYS_EXCEPTION_AROSED), MyAlert.DEFAULT, AlertType.ERROR);
            return;
        }

        if (mailsOnServers == -1 && !hasAccountToRetrieve()) {
            getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NO_ACCOUNT_SET_ACTIVE), MyAlert.DEFAULT, AlertType.WARNING);
        } else {
            //first run of serversSync. download new mails
            if (mailsOnServers == -1) {
                syncOK = true;
                mailsOnServers = 0; //initiate the counter and indicate that sync is running
                //if (Settings.minInBoxDBSpace != -1 && Functions.spaceLeft(DBFile) * 1024 < Settings.minInBoxDBSpace) {
                    //deleteAllMailsFromBoxAndDB(true);
                //}
                retrieve();
            } else if (!InProtocol.isBusyGlobal()) { //second run. downloading completed. 
                synchronized (storage) { //lock the storage to avoid from modifying
                    //content of servers inboxes and our inbox are the same
                    if (storage.getSize() == mailsOnServers) {
                        mailsOnServers = -1;
                        return;
                    }
                    //now, markAsDeleted all the mails that are not on servers but are stored in the phone			
                    progress.setTitle(Lang.get(Lang.ALRT_SYNCHRONIZING) + Lang.get(Lang.ALRT_WAIT));
                    progress.updateProgress(0, 0);

                    MessageHeader message;
                    int toDelete = storage.getSize() - mailsOnServers;
                    MailAccount account;
                    Enumeration messages = storage.getEnumeration();
                    while ( messages.hasMoreElements() ) {
                        message = (MessageHeader) messages.nextElement();
                        account = (MailAccount) getMujMail().getMailAccounts().get(message.getAccountID());
                        //the mail is not on the servers anymore, only on the mobile from previous sessions
                        if (account != null && account.isActive() && !account.getProtocol().containsMail(message)) {
                            if (!message.deleted) {
                                ++deleted;
                                message.deleted = true;
                            }
                            if (--toDelete == 0) {
                                break;
                            }
                        }
                    }
                    mailsOnServers = -1;
                }
                deleteMarkedFromBoxAndDB();//also markAsDeleted the mails that were marked in the inbox.							
            }
        }
    }

    /**
     * @return true if there is any account to retriveve by instance of box, false otherwise
     */
    public boolean hasAccountToRetrieve() {
        return accounts.size() > 0;
    }

    //#ifdef MUJMAIL_USR_FOLDERS
    /** Persistently save (and actualize) set of accounts
     *  that should be retrived by instance of InBox */
    public void saveBoxRetrieveAccounts(Vector/*<MailAccount>*/ newAccounts) {
        if (accounts == null) {
            return;
        }
                
        accounts = newAccounts;
        RetrieveBoxAccountsTask saveTask = new RetrieveBoxAccountsTask(RetrieveBoxAccountsTask.ACCOUNTS_SAVE);
        saveTask.start();
    }
    //#endif
    
    /**
     * Get list of accounts that retrieve this box
     * @return Vector of accout to be retrieve by box
     */
    public Vector/*<MailAccount>*/ getAccounts() {
        return accounts;
    }

    /**
     * Changes box to retrieve all active mail accounts.
     * Note: Call if this folder is set (unset) as default mujMail Inbox folder.
     */
    public void actualizeActiveAccountList() {
        accounts.removeAllElements();
        for (Enumeration e = getMujMail().getMailAccounts().elements(); e.hasMoreElements();) {
            MailAccount account = (MailAccount) e.nextElement();
            if (account.isActive()) {
                accounts.addElement( account);
            }
        }
    }
    
    /**
     * Retrieves new mails from all accounts.
     * Starts retrieving mails in new thread. Does not retrieve mails from all
     * accounts simultaneously, but serializes retrieving mails.
     */
    public void retrieve() {
        Thread thread = new Thread() {

            public void run() {
                if (!hasAccountToRetrieve()) {
                    getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NO_ACCOUNT_SET_ACTIVE), MyAlert.DEFAULT, AlertType.WARNING);
                }
                //if (Settings.minInBoxDBSpace != -1 && Functions.spaceLeft(DBFile) * 1024 < Settings.minInBoxDBSpace) {
                //deleteAllMailsFromBoxAndDB(true);
                //}
                setNeedResort(true);
                for (Enumeration e = accounts.elements(); e.hasMoreElements();) {
                    MailAccount account = (MailAccount) e.nextElement();
                    InProtocol.waitForNotBusyGlobal();
                    account.getProtocol().getNewMails(InBox.this);
                }
            }
        };
        thread.start();

    }

    //from one account
    public void retrieveOne(String accountID) {
        if (accountID != null && getMujMail().getMailAccounts().get(accountID) != null) {
            MailAccount account = (MailAccount) getMujMail().getMailAccounts().get(accountID);
            if (account.isActive()) {
                //if (Settings.minInBoxDBSpace != -1 && Functions.spaceLeft(DBFile) * 1024 < Settings.minInBoxDBSpace) {
                    //deleteAllMailsFromBoxAndDB(true);
                //}
                setNeedResort(true);
                account.getProtocol().getNewMails(this);
            } else {
                getMujMail().alert.setAlert(this, null, accountID + " " + Lang.get(Lang.INACTIVE), MyAlert.DEFAULT, AlertType.WARNING);
            }
        }
    }
    
    /** Try to found Mail account with which we download this email
     * 
     * @param header Mail header which account we are interesting
     * @return Mail account where email come from.
     */
    private MailAccount findAccountForHeader(MessageHeader header) {
        if (header == null) {
            return null;
        }
        // First step look in accounts of this box
        for(Enumeration e = getAccounts().elements(); e.hasMoreElements(); ) {
            MailAccount account = (MailAccount)e.nextElement();
            if (account.getEmail().equals(header.getAccountID())) {
                // Found correct acocunt
                return account;
            }
        }
        // Second step (box settings changed or mail moved)
        MailAccount account = (MailAccount) getMujMail().getMailAccounts().get(header.getAccountID());
        if (account == null) { //all ready deleted account (account renamed, ...)
            return null;
        }
        return account;
    }

    /**
     * Downloads a body of the mail given by header.
     * @param header the header of mail which body will be deleted
     */
    public void getBody(MessageHeader header) {
        MailAccount account = findAccountForHeader(header);
        if (account == null) {
            getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NONEXIST) + ": " + header.getAccountID(), MyAlert.DEFAULT, AlertType.WARNING);
            synchronized (header) {
                header.notify();
            }
            return;
        }
        if (Settings.safeMode) //we are reusing the lastSafeMail DBfile
        {
            clearLastSafeMail();
        }
        account.getProtocol().getBody(header, this);
    }

    /**
     * Redownloads a mail.
     * @param header identifies the mail which will be redownloaded
     * @param mode value -1 for redownloading the whole mail
     *  higher values for a concrete bodypart
     */
    public void regetBody(MessageHeader header, byte mode) {
        MailAccount account = findAccountForHeader(header);
        if (account == null) {
            getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NONEXIST) + ": " + header.getAccountID(), MyAlert.DEFAULT, AlertType.WARNING);
            if (mode != -1) { //if we just wanted to redownload a particular bodypart,
                synchronized (header) {
                    header.notify();// then we should wake up the mailForm, which triggered this procedure
                }
            }
            return;
        }
        if (Settings.safeMode) {
            clearLastSafeMail();
        }
        account.getProtocol().regetBody(header, mode, this);
    }

    public void clearLastSafeMail() {
        if (getLastSafeMail() != null) {
            try {
                mailDB.clearDb(false); // TODO Alf - work in no safe mode select? ... safe mode detection
            } catch (Exception ex) {
            }
            getLastSafeMail().deleteAllBodyParts();
            try {
                getMailDB().saveHeader(getLastSafeMail()); //update new info that the mail no longer has some content
            } catch (Exception ex) {
            }
            setLastSafeMail(null);
        }
    }
    /** 
     * Delete all mails a databases that uses InBox.
     * Note: Used if removing user mailbox from mujMail
     */
    public void removeAllDBs() {
         try {
             mailDB.clearDb(true);
         } catch (Exception ex) {
             // Some deleting failed ... typically database was not created
             if (DEBUG) { System.out.println("DEBUG InBox.removeAllDBs - cleardb problem"); ex.printStackTrace(); }
         }
        //#ifdef MUJMAIL_USR_FOLDERS
         try {
             // remove accounts database (acount list to retrieve)
             RecordStore.deleteRecordStore(getDBFileName() + "_ACC");
         } catch (Exception ex) {
             if (DEBUG) { System.out.println("DEBUG InBox.removeAllDBs - accounts db problem"); ex.printStackTrace(); }
         }
         //#endif
    }

    public void deleteAllMailsFromBoxAndDB(boolean sure) {
        super.deleteAllMailsFromBoxAndDB(sure);
        if (sure) {
            setLastSafeMail(null);
            if ( msgIDs != null ) {
            	msgIDs.clear();
            }
            unreadMails = 0;
        }
    }

    /**
     * Deletes mails from inbox and from database.
     * If Settings.moveToTrash is true and Settings.safeMode is false, moves
     * mails to the thrash. 
     * If Settings.delMailFromServer is true, deletes mails from mails server
     * as well.
     */
    public void deleteMarkedFromBoxAndDB() {
        MessageHeader header;
        MailAccount account;
        int x = 0;
        //lets take all the mails that were marked as read		 
        synchronized (storage) {
            Enumeration messages = storage.getEnumeration();
            while ( messages.hasMoreElements() ) {
                header = (MessageHeader) messages.nextElement();
                if (header.deleted) {
                    msgIDs.remove(header.getAccountID() + "@" + header.getMessageID());
                    if (header.readStatus == MessageHeader.NOT_READ) {
                        --unreadMails;
                    }
                    if (Settings.delMailFromServer) {
                        account = (MailAccount) getMujMail().getMailAccounts().get(header.getAccountID());
                        //get appropriate mailAccount
                        if (account != null && account.isActive()) {
                            account.getProtocol().addDeleted(header);
                        } //put them to apropriate servers list
                    }
                    ++x;
                }
                if (x == deleted) //if we detected all deleted mails, we can end it here
                {
                    break;
                }
            }
        }
        if (!isSyncRunning() && Settings.delMailFromServer) {
            for (Enumeration e = getMujMail().getMailAccounts().elements(); e.hasMoreElements();) {
                account = (MailAccount) e.nextElement();
                if (account.isActive()) {
                    account.getProtocol().removeMsgs(this);
                }
            }
        }

        super.deleteMarkedFromBoxAndDB(); //markAsDeleted them from DB and storage vector
    }

    protected void hideButtons() {
        if (InProtocol.isBusyGlobal()) {
            addCommand(stop);
        }
        if (isBusy()) {
            removeCommand(retrieve);
        } else {
            if ( storage.getSize() == 0 ) { // if storage is empty
                addCommand(retrieve);
            }
            removeCommand(stop);
        }
        if (!btnsHidden) {
            removeCommand(redownload);
        }
        super.hideButtons();
    }

    protected void showButtons() {
        if (btnsHidden) {
            addCommand(retrieve);
            addCommand(redownload);
            removeCommand(stop);
            super.showButtons();
        }
    }

    protected void keyPressed(int keyCode) {
        if (isBusy()) {
            return;
        }
        final MessageHeader messageHeader = getMessageHeaderAt( getSelectedIndex() );
        if (keyCode == '3' && messageHeader != null) {	//key 3 changes readStatus of a mail		
            MessageHeader header = messageHeader;
            header.readStatus = (byte) (1 - header.readStatus);
            shiftSelectedIndex( true );
            if (header.readStatus == MessageHeader.READ) {
                getMujMail().getInBox().changeUnreadMails(-1);
            } else {
                getMujMail().getInBox().changeUnreadMails(1);
            }
            repaint();
            try {
                getMailDB().saveHeader(header); //dont forget to write it to the DB						
            } catch (MyException ex) {
                report(ex.getDetails(), SOURCE_FILE);
            }
        }
        else {
        	super.keyPressed(keyCode);
        }
    }

    /**
     * Moves the cursor on the first unread mail.
     */
    public void setCurFirstUnread() {
        if (unreadMails != 0) {
            Enumeration messages = storage.getEnumeration();
            MessageHeader message;
            cur = 0;
            empties = 0;
            while ( messages.hasMoreElements() ) {
                message = (MessageHeader)messages.nextElement();
                if ( message.readStatus == MessageHeader.NOT_READ) {
                    break;
                }
                shiftSelectedIndex(true);
            }
        }
    }

    public void stop() {
        removeCommand(stop);
        for (Enumeration e = getMujMail().getMailAccounts().elements(); e.hasMoreElements();) {
            MailAccount account = (MailAccount) e.nextElement();
            if (account.isActive()) {
                account.getProtocol().stop();
            }
        }
    }

    public MessageHeader storeMail(MessageHeader header) {
        header = super.storeMail(header);
        if (header != null && header.readStatus == MessageHeader.NOT_READ) {
            ++unreadMails;
            msgIDs.put(header.getAccountID() + "@" + header.getMessageID(), header);
        }
        return header;
    }
    
    /**
     * Returns true if there is some account with push active. That means that
     * IMAP command idle was executed and it is waiting for new mails.
     * 
     * @return true if there is some account with push active.
     */
    public boolean isPushActive() {
        return pushActive;
    }
    
    /*
     * Push mail: Push mail or IDLE capability is an possible IMAP capability (RFC 2177) but must not be implemented in each server. 
     * We start a new thread and new IMAP session for each active account if "Push mail" is activate in the settings --> TODO!
     * At first we have to proof the IDLE capability for each IMAP server (TODO: not implemented yet). It can be done in two ways:
     *      1) execute "CAPABILITY" command and look for "IDLE" --> BUT: some servers "hide" the IDLE capability
     *      2) choose the box and execute "IDLE", then look for response
     * The second way is the better one, because of servers that hide their IDLE capability.
     */
    public synchronized void push(){
        if(!pushActive){
            if (!hasAccountToRetrieve()) {                
                getMujMail().alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NO_ACCOUNT_SET_ACTIVE), MyAlert.DEFAULT, AlertType.WARNING);
            } else {
                //TODO Proof IMAP capability here? 
                pushActive = true;
                for (Enumeration e = getMujMail().getMailAccounts().elements(); e.hasMoreElements();) {
                    MailAccount pushAccount = (MailAccount) e.nextElement();
                    
                    if (pushAccount.isActive() && pushAccount.getProtocol().isImap()) {                        
                        pushAccount.prepareForPushing(this);                        
                        Pushing push = new Pushing(pushAccount);                 
                        push.start();
                        
                    }
                }
            }
        } else {
            pushActive = false;
        }
    
    }
    
    /**
     * Stops pushing new mails in all accounts.
     */
    public synchronized void stopPush() {
        pushActive = false;
    }
    
    /**
     * Called from MailDB after loading db (loadDB) is done
     * Note: Overwrites generic body
     */
    public void loadedDB() {
        initHash(); //init the map of the mails in the Inbox
        resort(); //its needed to resort the box according to the settings
    }

    public void commandAction(Command c, Displayable d) {
        super.commandAction(c, d);

        if (c == retrieve) {
            retrieve();
        } else if (c == redownload) {
            regetBody( getSelectedHeader(), (byte) -1); //redownloading completed mail
        } else if (c == stop) {
            stop();
        }
    }

    /* ***************************
     *    getters and setters    *
     *****************************/
    public MessageHeader getLastSafeMail() {
        return lastSafeMail;
    }

    public void setLastSafeMail(MessageHeader lastSafeMail) {
        this.lastSafeMail = lastSafeMail;
    }
    
    public int getUnreadMailsCount() {
        return unreadMails;
    }
    
    //#ifdef MUJMAIL_USR_FOLDERS    
    public int getUserBoxListDBRecID() {
        return userBoxListDB_recordID;
    }

    public void setUserBoxListDBRecID(int userBoxListDBRecID) {
        userBoxListDB_recordID = userBoxListDBRecID;
    }
    //#endif

    //#ifdef MUJMAIL_USR_FOLDERS    
    /**
     * Loads or saves (in background) accounts that 
     *  have to be retrieved by (user folder).
     * <p>
     * Loading is done automatically during creating folder {@link #InBox}
     *
     * @see mujMail.InBox#saveBoxRetrieveAccounts 
     */
    private class RetrieveBoxAccountsTask extends Thread {
        public static final byte ACCOUNTS_LOAD = 1;
        public static final byte ACCOUNTS_SAVE = 2;
        
        private byte mode;
        RetrieveBoxAccountsTask(byte mode) {
            this.mode = mode;
        }

        public void run() {
            if (InBox.DEBUG) { System.out.println("DEBUG RetrieveBoxAccountsTask.run()"); }
            mujMail.getAccountSettings().waitForAccountsLoading();
            if (InBox.DEBUG) { System.out.println("DEBUG RetrieveBoxAccountsTask.run() after loading of accounts"); }
            try {
                String dbName = getDBFileName() + "_ACC";
                switch (mode) {
                    case ACCOUNTS_LOAD:
                        RecordStore rs1 = RecordStore.openRecordStore(dbName, true);
                        RecordEnumeration en = rs1.enumerateRecords(null, null, false);

                        byte[] data1 = new byte[128];
                        ByteArrayInputStream buffer1 = new ByteArrayInputStream(data1);
                        DataInputStream stream1 = new DataInputStream(buffer1);

                        while (en.hasNextElement()) {
                            int id = en.nextRecordId();
                            int sizeOfRecord = rs1.getRecordSize(id);
                            if (sizeOfRecord > data1.length) {
                                data1 = new byte[sizeOfRecord + 30];
                                buffer1 = new ByteArrayInputStream(data1);
                                stream1 = new DataInputStream(buffer1);
                            }
                            rs1.getRecord(id, data1, 0);
                            stream1.reset();
                            
                            String accountType = stream1.readUTF();
                            if (MailAccountPrimary.CLASS_TYPE_STRING.equalsIgnoreCase(accountType)) {
                                String email = stream1.readUTF();
                                MailAccount ma = (MailAccount)mujMail.getMailAccounts().get(email);
                                if (ma == null) {
                                    if (true || InBox.DEBUG) { System.out.println("Error inbox " + getName() + " mail account " + email + " not exists - willnot be retrieved"); }
                                } else {
                                    accounts.addElement(ma);
                                }
                            } else if (MailAccountDerived.CLASS_TYPE_STRING.equalsIgnoreCase(accountType)) {
                                String email = stream1.readUTF();
                                String imapFld = stream1.readUTF();
                                MailAccount ma = (MailAccount)mujMail.getMailAccounts().get(email);
                                if (ma == null || imapFld == null) {
                                    if (true || InBox.DEBUG) { System.out.println("Error inbox " + getName() + " mail account " + email + " (DERIVED) not exists - will not be retrieved"); }
                                } else {
                                    accounts.addElement( new MailAccountDerived(ma, imapFld));
                                }
                            }
                        }
                        stream1.close();
                        buffer1.close();
                        rs1.closeRecordStore();
                        break;
                    case ACCOUNTS_SAVE:
                        // Clear currently saved
                        try {
                            RecordStore.deleteRecordStore(dbName);
                        } catch (Exception e) {
                            // record store not exists
                        }
                        RecordStore rs2 = RecordStore.openRecordStore(dbName, true);
                        for(int i = 0; i < accounts.size(); i++) {
                            MailAccount ma = (MailAccount)accounts.elementAt(i);

                            ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
                            DataOutputStream stream2 = new DataOutputStream(buffer2);
                            // 1. case Primary email (POP3)
                            if (ma.getAccountClassType() == MailAccount.ACCOUNT_CLASS_TYPE_PRIMARY) {
                                stream2.writeUTF(MailAccountPrimary.CLASS_TYPE_STRING);
                                stream2.writeUTF(ma.getEmail());
                            } else if (ma.getAccountClassType() == MailAccount.ACCOUNT_CLASS_TYPE_DERIVED) {
                            	//#ifdef MUJMAIL_SYNC
                                stream2.writeUTF(MailAccountDerived.CLASS_TYPE_STRING);
                                //#endif
                                stream2.writeUTF(ma.getEmail());
                                stream2.writeUTF(ma.getIMAPPprimaryBox());
                            }
                            stream2.flush();
                            rs2.addRecord(buffer2.toByteArray(), 0, buffer2.size());
                            stream2.close();
                            buffer2.close();
                            if (InBox.DEBUG) { System.out.println("RBAT - successfully saved " + ma.getEmail()); }
                        }
                        rs2.closeRecordStore();
                        break;
                }
            } catch (Exception e) {
                System.out.println(e.toString());
                e.printStackTrace();
            }
        }
    }
    //#endif
}
