package mujmail;
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

import java.util.Hashtable;
import javax.microedition.lcdui.AlertType;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import mujmail.util.Functions;


/**
 * Holds list (ID) of all ever seen mails.
 * Is used to recognize newly coming e-mails.
 * Note: Singleton, only one instance exists
 */
public class MailDBSeen {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false; /// Debugging output for this file
    
    public static final String DB_FILE = "MAIL_IDs"; /// Name of database where seen mail ids stored
    
    private Hashtable/*<String,int>*/ msgIDsHash; /// Maps all mails that were once downloaded to the application
    // int in hash table represent RecordID from Database file, it's needed for selective mail delete

    private final MujMail mujMail; /// mujMail instance
    
    MailDBSeen(MujMail mujMail) {
        this.mujMail = mujMail;
        msgIDsHash   = new Hashtable(1);
        
        // Load seen db in separate thread
        Thread thr1 = new Thread() {
            public void run() {
                init();
            }
        };
        thr1.start();        
    }

    /**
     * Check if mail was added into seen mail db. We don't download seen mails.
     * @param accountID Account to which mail become (email adress).
     * @param messageID Mail identifier to check.
     * @return true if mail seen
     */
    public boolean wasMailSeen(String accountID, String messageID) {
        if (DEBUG) { System.out.println("MailDBSeen::wasMailSeen ... " + accountID + "@" + messageID); }
        if (Settings.downOnlyNeverSeen) { //let's check if it was once downloaded
            return msgIDsHash.containsKey(accountID + "@" + messageID);
        }
        return false;
    }

    public synchronized void addSeen(MessageHeader header) throws MyException {
        if (DEBUG) { System.out.println("MailDBSeen::addSeen ... " + header.toString()); }

        if (header == null) return;
        // TODO: Folder not iteresting?? read specification
        String ID = new String(header.getAccountID() + "@" + header.getMessageID()); 
        if (!msgIDsHash.containsKey(ID)) {
            // Add into DB
            int recordID = -1;
            RecordStore msgIDs = null;
            try { //lets mark it as seen
                msgIDs = Functions.openRecordStore(DB_FILE, true);
                recordID = msgIDs.addRecord(ID.getBytes(), 0, ID.length());
            } catch (Exception ex) {
                throw new MyException(MyException.DB_CANNOT_SAVE_MSGID);
            } finally {
                Functions.closeRecordStore(msgIDs);
            }
                
           // Add into runtime representation - hashtable
            msgIDsHash.put(ID, new Integer(recordID));
        }
    }
    
    /**
     * Selective remove
     * Remove mail from database of seen mails.
     * @param accountID Account to which mail become (email adress).
     * @param messageID Mail identifier to check.
     */
    public synchronized void deleteSeen(String accountID, String messageID) {
        if (DEBUG) { System.out.println("MailDBSeen::removeSeen ... " + accountID + "@" + messageID); }
        String ID = new String(accountID + "@" + messageID);
        if (msgIDsHash.containsKey(ID)) {
            Integer n = (Integer)msgIDsHash.get(ID);

            // Remove from DB
            RecordStore msgIDs = null;
            try {
                msgIDs = Functions.openRecordStore(DB_FILE, true);
                msgIDs.deleteRecord(n.intValue());
            } catch (Exception ex) {
                // Removing failed
                mujMail.alert.setAlert(null, null, Lang.get( Lang.EXP_DB_CANNOT_DEL_MSGID), MyAlert.DEFAULT, AlertType.WARNING);
                ex.printStackTrace();
            } finally {
                Functions.closeRecordStore(msgIDs);
            }

            // Remove from Hashtable
            msgIDsHash.remove(ID);
        }
        if (DEBUG) { System.out.println("MailDBSeen::removeSeen ... end"); }
    }
    
    /**
     * Clear database and it's run time copy. 
     * Sets mujMail into initial state where no mail was seen.
     * @param sure if false shows confirmation
     */
    public void deleteAll(boolean sure) {
        if (!sure) {
            //TODO:- is null,null ok?
            mujMail.alert.setAlert(null, null, Lang.get(Lang.ALRT_SYS_DEL_ALL_CONFIRM), MyAlert.DB_CLEAR_CONFIRM, AlertType.CONFIRMATION); 
            return;
        }
        try {
            RecordStore.deleteRecordStore(DB_FILE); // db file
            msgIDsHash.clear(); // hashtable
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, mujMail.getMenu(), ex.getMessage(), MyAlert.DEFAULT, AlertType.ERROR);
        }
    }
    
    /**
     * Loads DB into internal runtime hastable storrage.
     * Loads message IDs of all mails that were once downloaded 
     */
    private void init() {
        if (DEBUG) { System.out.println("Initing mail hash"); }

        RecordStore msgIDs = null;
        try {
            msgIDs = Functions.openRecordStore(DB_FILE, true);
            int count = msgIDs.getNumRecords();
            msgIDsHash = new Hashtable((int)(1.3 * count));
            if ( count > 0) {
                RecordEnumeration enumeration = msgIDs.enumerateRecords(null, null, false);
                String ID;
                int recordID;
                while (enumeration.hasNextElement()) {
                        recordID = enumeration.nextRecordId();
                        ID = new String(msgIDs.getRecord(recordID));
                        msgIDsHash.put(ID, new Integer(recordID)); //? Why store RecordIDs??
                }
            }
        } catch (Exception ex) {
            mujMail.alert.setAlert( Lang.get( Lang.EXP_DB_CANNOT_LOAD_MSGID), AlertType.ERROR);
        } catch (Error er) {
            mujMail.alert.setAlert( Lang.get( Lang.EXP_SYS_OUT_OF_MEMORY), AlertType.ERROR);
        } finally {
            if (msgIDs != null) Functions.closeRecordStore(msgIDs);
        }
        if (DEBUG) { System.out.println("Initing mail hash finished"); }
    }
    
    public int getOccupiedSpace() {
        return Functions.spaceOccupied(DB_FILE);
    }

}
