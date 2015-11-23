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

package mujmail.mailboxes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

import mujmail.util.Functions;
import mujmail.InBox;
import mujmail.Lang;
import mujmail.MailDB;
import mujmail.Menu;
import mujmail.MujMail;
import mujmail.MyAlert;
import mujmail.MyException;
import mujmail.PersistentBox;
import mujmail.account.MailAccount;
import mujmail.account.MailAccountDerived;
import mujmail.ordering.comparator.MailBoxNameComparator;

/**
 * Manages user folders. Holds list of this folders. 
 * Take care about basic folder operation like creating, loading, removing.
 */
public class BoxList {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;
    
    private static final String dbName         = "_MailBoxesList"; /// Name of database with folders
    private static final String dbUserFolder   = "UserFolder_";    /// Prefix used for databases with user folders
    private int maxDBFieldSize = 128;                              /// Maximum size of one record store entry ... now 2*32 Unicode characters
    
    private MujMail mujMail; /// mujMail instance
    private Vector/*<InBox>*/ mailBoxes; /// list of mailBoxes stored this BoxList
    
    /**
     * Creates new holder for user folders
     * 
     * @param mujMail running mujmail instance
     */
    public BoxList( MujMail mujMail) {
        this.mujMail = mujMail;
        mailBoxes = new Vector(5);
    }
    
    /** 
     * Loads mail stored in persistent storage into mail boxes.
     * Also loads mail into standard mujmail folders (like InBox, Drafts, etc)
     * <p>
     * Note: Call only once, another call will add user folders again.
     */
    public void loadBoxes() {
        Thread lt = new Thread() {
            public void run() {
                loadingBoxesThread();
            }
        };
        lt.start();
    }
    
    /** Real worker for methoed {@link #loadBoxes} */
    private void loadingBoxesThread() {
        // Open box db
        try {
            if (DEBUG) { System.out.print("DEBUG BoxList.loadingBoxesThread .. Loading mailboxes\n"); }
            mujMail.loadDefaulFolders();
            if (DEBUG) { System.out.print("DEBUG BoxList.loadingBoxesThread .. DefaultMailBoxes loaded\n"); }
            RecordStore rs = Functions.openRecordStore(dbName, true);
            if (DEBUG) {
                    System.out.println("Record store: " + rs.getName());
                    System.out.println("    Number of rec  = " + rs.getNumRecords());
                    System.out.println("    Total size     = " + rs.getSize());
                    System.out.println("    Version        = " + rs.getVersion());
                    System.out.println("    Last modified  = " + rs.getLastModified());
                    System.out.println("    Size available = " + rs.getSizeAvailable());
                System.out.print("\n");
            }

            if (rs.getNextRecordID() == 1) {
                return;
            }
            RecordEnumeration re = rs.enumerateRecords(null, null, false);
            byte[] data = new byte[maxDBFieldSize];
            ByteArrayInputStream buffer = new ByteArrayInputStream(data);            
            DataInputStream stream = new DataInputStream(buffer);
            
            while (re.hasNextElement()) {
                int id = re.nextRecordId();
                // Ensure if buffer is big enought to hold all data
                int entrySize = rs.getRecordSize(id);
                if (entrySize > maxDBFieldSize) {
                    // Note: if maxDBFieldSize set correctly should not happned
                    data = new byte[entrySize + 32];
                    maxDBFieldSize = entrySize + 32;
                    buffer = new ByteArrayInputStream(data);
                    stream = new DataInputStream(buffer);
                }
                rs.getRecord( id, data, 0); // Element from record strore
                stream.reset();
                
                String folderName = stream.readUTF();
                String dbName     = stream.readUTF();
                
                InBox boxElement = new InBox(dbName, folderName);
                boxElement.setUserBoxListDBRecID(id);
                if (DEBUG) { System.out.println("BoxList.loadBoxes ... adding user box \"" + folderName + "\" with database \"" + dbName + "\""); }
                // TODO make it in some serial and not parallel way as now ... notify if mailbox is loaded
                mailBoxes.addElement(boxElement);
                MailDB  boxElementDB = boxElement.getMailDB();
                boxElementDB.loadDB(boxElement);
                synchronized (boxElementDB) {
                    boxElementDB.waitForTaskEnd();
                }
            }
            sort();
            if (DEBUG) System.out.println("Loading mailboxes ... done\n");
            System.gc();
            
        } catch (Exception ex) {
            // TODO: ma byt druhy obekt null?
            mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_MB_CREATING) + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
        }
    }
    
    /** 
     * Create new user folder. Shows setting form to customize mail box settings.
     * <p>Create persistentBox and add them into user folder box list. 
     */
    public synchronized void createPersistentBox() {
        // Generate new name for DB file
        int max = 0; // Maximum index used in folderName;
        String[] folders = RecordStore.listRecordStores();
        for(int i = 0; i < folders.length; i++) {
            if (folders[i].startsWith(dbUserFolder)) {
                // Parse numeric suffix
                int j = dbUserFolder.length();
                int jMax = folders[i].length();
                int val = 0;

                while (j < jMax) {
                    char c = folders[i].charAt(j);
                    if (!Character.isDigit(c)) break;
                    val = val * 10 + Character.digit(c,10);
                    j++;
                }
                
                if (val > max) {
                    max = val;
                }
            }
        }
        
        // Step 2 - find maximum in mailBox vector 
        //  (cover case if some mailbox hasn't created dbfiles yet)
        for(int i = 0; i < mailBoxes.size(); i++) {
            String DBFileName = ((PersistentBox)mailBoxes.elementAt(i)).getDBFileName();
            if (DBFileName.startsWith(dbUserFolder)) {
                // Parse numeric suffix
                int j = dbUserFolder.length();
                int jMax = DBFileName.length();
                int val = 0;

                while (j < jMax) {
                    char c = DBFileName.charAt(j);
                    if (!Character.isDigit(c)) break;
                    val = val * 10 + Character.digit(c,10);
                    j++;
                }
                
                if (val > max) {
                    max = val;
                }
            }
        }

        max++; // Now is max new empty suffix
        
        // Add entry into database
        String newBoxFolderName = "UserFolder " + Integer.toString(max);
        String newBoxDBName = dbUserFolder + Integer.toString(max);
        int recordID = -1;
        try {
            recordID = changeStoredUserBoxInfo( -1, newBoxFolderName, newBoxDBName);
        } catch (Exception ex) {
            mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_SAVING) + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
        }
        InBox newBox = new InBox(newBoxDBName, newBoxFolderName);
        newBox.setUserBoxListDBRecID(recordID);
        mailBoxes.addElement(newBox);
        sort();
        newBox.getMailDB().loadDB(newBox);
        // refresh is done in sorting
        // mujMail.getMenu().refresh(Menu.FOLDERS, false); 
        editUserMailBoxSettings( mailBoxes.indexOf(newBox));
    }
    
    /**
     * Sorts user mail boxes by name
     */
    public void sort() {
        Functions.sort(mailBoxes, new MailBoxNameComparator());
        mujMail.getMenu().refresh(Menu.FOLDERS, false);
    }
    
    /** 
     * Gets vector of user created PersistentBoxes
     * @return Vector where user mailboxes are stored
     */
    public final Vector getBoxList() {
        return mailBoxes;
    }
    
    /** 
     * Calculate space that holds user folders databases
     * @return User folders occupied db space
     */
    public synchronized int spaceOccupied() {
        int space = 0;
        for( int i = 0; i < mailBoxes.size(); i++) {
            space += ((PersistentBox)mailBoxes.elementAt(i)).getOccupiedSpace();
        }
        return space;
    }

    /**
     * Remove mails from all user boxes.
     * <p>
     * Is intended for clearing databases and creating space for new mails.
     */ 
    public synchronized void deleteAllMailsFromAllUserBoxesAndDB() {
        boolean exception = false;

        for( int i = 0; i < mailBoxes.size(); i++) {
            try {
                // Delete user boxes 
                ((PersistentBox)mailBoxes.elementAt(i)).deleteAllMailsFromBoxAndDB(true);
            } catch (Exception ex) {
                exception = true;
                if (DEBUG) {
                    System.out.println("DEBUG BoxList.deleteAllUserBoxes - exception " + ex);
                    ex.printStackTrace();
                }
            }
        } // for cycle
        if (exception) { 
            mujMail.alert.setAlert(this, null, Lang.get(Lang.EXP_DB_CANNOT_DEL_HEADER), MyAlert.DEFAULT, AlertType.ERROR);
        }
    }

    /**
     * Shows edit form for user mail box. 
     * Allows setting user folder properties like name of retrieved accounts.
     *
     * @param index Index of mailbox in {@link #mailBoxes} vector
     */
    public void editUserMailBoxSettings(int index) {
        if (index < 0 || index >= mailBoxes.size()) {
            if (DEBUG) { System.out.println("DEBUG BoxList.editUserMailBoxSettings(int) - unknown MailBox" + index); }
            return;
        }
        InBox box = (InBox)mailBoxes.elementAt(index);
        
        // This shows new edit form
        UserMailBoxEditForm editForm = new UserMailBoxEditForm(box);
       
    }
    
    /** 
     * Edits or adds information about user box in persisten database
     * 
     * @param recordID if -1 adding new box into database, otherwise position of database record to edit
     * @param newBoxFolderName Data to by stored in db. User box folder name.
     * @param newBoxDBName Data to by stored in db. User box database name (prefix).
     * @return recordID where adding (update take place)
     * @throws mujmail.MyException If stroring failed, not existing database, not existing record, not enoght space
     * Note: recordID can be retrieve from inbox by calling {@link InBox#getUserBoxListDBRecID()}
     */
    protected synchronized int changeStoredUserBoxInfo(int recordID, String newBoxFolderName, String newBoxDBName) throws MyException {
        if (DEBUG) { System.out.println("BoxList.changeStoredUserBoxInfo( recordID=" + recordID + ", newBoxFolderName=" + newBoxFolderName + ", newBoxDBName=" + newBoxDBName + ")"); }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(buffer);
        RecordStore rs = null;
        try {
            stream.writeUTF(newBoxFolderName);
            stream.writeUTF(newBoxDBName);
            stream.flush();
            stream.close();

            rs = Functions.openRecordStore(dbName, true);
            // Adding new db entry of editting existing
            if (recordID == -1) {
                recordID = rs.addRecord(buffer.toByteArray(), 0, buffer.size());
            } else {
                rs.setRecord(recordID, buffer.toByteArray(), 0, buffer.size());
            }
        } catch (Exception ex) {
            if (DEBUG) { System.out.println("BoxList.changeStoredUserBoxInfo(...) exception"); ex.printStackTrace(); }
            if (rs != null) Functions.closeRecordStore(rs);
            throw new MyException(MyException.DB_CANNOT_SAVE, newBoxFolderName);
        } finally {
            if (rs != null) Functions.closeRecordStore(rs);
        }
        return recordID;
    }

    /** 
     * Remove mailbox from system. 
     * Remove all it's databases, remove from runtime user mailbox lists.
     * 
     * @param index Index of mailbox to remove in {@link #mailBoxes} vector
     */
    public synchronized void removeUserMailBox(int index) {
        // Ensure that removing mailbox managed by this instance
        if (index < 0 || index >= mailBoxes.size()) {
            if (DEBUG) { System.out.println("DEBUG BoxList.removeUserMailBox(InBox) - unknown MailBox"); }
            return;
        }
        InBox toRemove = (InBox)mailBoxes.elementAt(index);
        // Remove mailBox databases
        toRemove.removeAllDBs();

        // Remove from runtime representation
        mailBoxes.removeElementAt(index);

        // Remove from presistent database "MailBoxesList" 
        try {
            RecordStore rs = RecordStore.openRecordStore(dbName, true);
            rs.deleteRecord( toRemove.getUserBoxListDBRecID());
            rs.closeRecordStore();
        } catch (Exception ex) {
            if (DEBUG) {
                System.out.println("DEBUG - BoxList.removeUserMailBox " + toRemove + " removing from DB failed");
                ex.printStackTrace();
            }
            mujMail.getAlert().setAlert(this, null, Lang.get(Lang.ALRT_DELETING) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
        }
        sort();
    }
       
    /**
     * Check if given box retrieves specified account and return proof.
     * <p>
     * It takes care about deriving accounts. So if exist derieved account 
     * (to given) if return derived as proof.
     * 
     * @param box Box whose accounts check
     * @param ma Mail account to check
     * @return null if not retrieved, proofing primary or derived account
     */
    static private MailAccount retrievesBoxAccount(InBox box, MailAccount ma) {
        // Check all accounts if direct case or indirect derived  case
        for(Enumeration e = box.getAccounts().elements(); e.hasMoreElements();) {
            MailAccount boxAcc = (MailAccount)e.nextElement();
            if ( ma == boxAcc) {
                return boxAcc;
            } else if (boxAcc instanceof MailAccountDerived && 
               ((MailAccountDerived)boxAcc).getSourceAccount() == ma) {
                return boxAcc; // Derived account
            }
        }
        return null; // not found
    }

    /** Class that represents edit form for user mailboxes */
    private class UserMailBoxEditForm extends Form implements CommandListener{
        
        private Command ok;
        private TextField userBoxName;

        /** Box that we are edditting */
        private InBox editedBox; 
        /** Choice group with account list */
        private ChoiceGroup accountSelector;
        /** Holds string for users */
        private StringItem imapFolderString;
        
        /** Holds count of accounts shown in chice box */ 
        private int accountsShown;
        
        /** Windows that was shown before this form and 
         *   that will be restored after end of mailbox settings.
         */
        private Displayable previousScreen;
        
        /**
         * Create and show form for setting user mail box properties.
         * After saving changes return to original screen.
         * @param ib Box that we are editting
         */
        public UserMailBoxEditForm(InBox ib) {
            //super( (create?Lang.get(Lang.ABT_ABOUT):Lang.get(Lang.ABT_ABOUT)) );
            super("Folder settings");
            
            editedBox = ib;
            
            ok = new Command("OK", Command.OK, 0);
            addCommand(ok);

            // Entry for folder name
            userBoxName = new TextField(Lang.get(Lang.TB_FOLDER_NAME), ib.getName(), 64, TextField.ANY);
            accountSelector = new ChoiceGroup(Lang.get(Lang.TB_RETRIVE_ACCOUNTS), Choice.MULTIPLE);
            imapFolderString = new StringItem(Lang.get(Lang.TB_IMAP_FOLDERS), null);
            setCommandListener(this);

            // Wait until all accounts are loaded
            mujMail.getAccountSettings().waitForAccountsLoading();
            
            
            append(userBoxName);
            append(accountSelector);

            boolean anyIMAPAccount = false; // Mark if any Imap account found

            // Counts primary accounts
            for(Enumeration e = mujMail.getMailAccounts().elements(); e.hasMoreElements();) {
                MailAccount ma = (MailAccount)e.nextElement();
                accountsShown++;
                int pos = accountSelector.append(ma.getEmail(), null);
                // Check if current user box uses this account for retrieve mails (and check it)
                MailAccount retAc = retrievesBoxAccount(ib, ma);
                if (retAc != null) {
                    accountSelector.setSelectedIndex(pos, true);
                } else {
                    retAc = ma;
                }
                if (retAc.isIMAP()) {
                    if (!anyIMAPAccount) {
                        append(imapFolderString);
                    }
                    // add textbox for imap folder settings
                    append( new TextField( ma.getEmail(), retAc.getIMAPPprimaryBox(), 1000, TextField.ANY));
                    anyIMAPAccount = true;
                }
            }

            // store original windows and show this form
            MujMail.MyDisplay disp = mujMail.getDisplay();
            previousScreen = disp.getCurrent();
            disp.setCurrent(this);
        }

        /** Pernamently saves new setting of user folder into databases */
        void updateUserBoxSettings() {
           
            try {
                // A) update name
                editedBox.setName(userBoxName.getString());
                // Store new name into database
                changeStoredUserBoxInfo( editedBox.getUserBoxListDBRecID(), userBoxName.getString(), editedBox.getDBFileName());
                
                //TODO Wait to time when editedBox not busy ... no retrieve task take place
                
                // Change accounts to be retrieved
                Vector /*<MailAccount>*/ accounts = new Vector/*<MailAccount>*/();
                  
                boolean[] flags = new boolean[accountsShown];
                accountSelector.getSelectedFlags(flags);
                Hashtable accList = mujMail.getMailAccounts();
                for(int i = 0; i < accountsShown; i++) {
                      if (flags[i]) { // selected account
                          MailAccount ma = (MailAccount)accList.get(accountSelector.getString(i));
                          if (ma.isIMAP()) {
                              // retrieve folder strings
                              // Iterate over form elements and found text with imap folders for this (ma) account
                              String imapFolders = "INBOX";
                              for( int i1 = 3; i1 < size(); i1++) {
                                  Item item = this.get(i1);
                                  if ((item instanceof TextField) && ((TextField)item).getLabel().equals(ma.getEmail())) {
                                      imapFolders = ((TextField)item).getString();
                                  }
                              }
                              MailAccount newMa = new MailAccountDerived(ma, imapFolders);
                              accounts.addElement(newMa);
                          } else {
                            // POP3
                              accounts.addElement(ma);
                          }
                      }
                  }
                  editedBox.saveBoxRetrieveAccounts(accounts);
            } catch (Exception ex) {
                if (BoxList.DEBUG) { System.out.println("DEBUG BoxList.updateUserBoxSettings - exception"); ex.printStackTrace(); }
                mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_SAVING) + Lang.get(Lang.FAILED) + ": " + ex, MyAlert.DEFAULT, AlertType.ERROR);
            }
            Functions.sort(mailBoxes, new MailBoxNameComparator());
            // Show menu and update databases in background
            mujMail.getMenu().refresh(Menu.FOLDERS, false);
        }
        
        public void commandAction(Command c, Displayable d) {
            if (BoxList.DEBUG) { System.out.println("DEBUG BoxList.UserMailBoxEditForm.commandAction(command=" + c + ", Displayable=" + d + ")"); }

            if (c == ok) {
                updateUserBoxSettings();
                mujMail.getDisplay().setCurrent(previousScreen);
            }
        }
    } 
}
