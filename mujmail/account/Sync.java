//#condition MUJMAIL_SYNC
package mujmail.account;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 Nodir Yuldashev <y_nodir@yahoo.com>
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

import mujmail.util.Callback;
import mujmail.util.StartupModes;
import mujmail.protocols.*;
import mujmail.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import mujmail.protocols.IMAP4;

//#ifdef MUJMAIL_FS
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;
import mujmail.ui.TextInputDialog;
import mujmail.ui.FileSystemBrowser;
//#endif

/**
 * Is used for saving and retriving mujmail setting.
 * <p>Settings can be stored into file or in mujMail server. 
 * Synchchronize mail accounts, settings and adressbook.
 */
public class Sync extends IMAP4 implements CommandListener {
    /** Flag signals if we want to print debug prints */
    public static final boolean DEBUG = false;
    
    private MujMail        main;
    private MailAccount    serverAcct;
    /** Synchronisation type selector. <p>{@link #BACKUP} <p>{@link #RESTORE} */
    private int            ACTION;
    /** Synchronisation mode selector. <p>{@link #LOCAL} <p>{@link #REMOTE} */
    private int            MODE;
    /** Path where save file if saving/loading configuration from file */
    private String filePath;
    private OutputStream dos;
    private InputStream dis;
    
    //#ifdef MUJMAIL_FS
    private SyncModeDialog smDlg;
    private MyFileConnection conn;
    //#endif

    // ACTION constants
    /** Work mode - do BACKUP */
    public static final int BACKUP  = 0;
    /** Work mode - Restore configureation */
    public static final int RESTORE = 1;
    // Synchronization modes
    //#ifdef MUJMAIL_FS
    /** Saving mode - save/restore from file */
    public static final int LOCAL  = 0;
    //#endif
    /** Saving mode - save/restore into server */
    public static final int REMOTE = 1;
	
    /** 
     * Creates new synchronization object.
     * 
     * @param mujMailServer Account that can by used for connetiong to mail mujMail server.
     */
    public Sync(MujMail mujMail, MailAccount mujMailServer) {
        super(mujMailServer);
        main   = mujMail;
        serverAcct = mujMailServer;
    }

    /** 
     * Is used for setting action to do
     * @param action Work you wanted to do 
     * @see #BACKUP
     * @see #RESTORE
     */
    public void setAction(int action) {
        this.ACTION = action;
    }

    
    //#ifdef MUJMAIL_FS
    /**
     * Set diaglog that is used for setting synchronisation mode
     * @param smDlg Dialog that shows local/remote selection
     */
    public void setSmDlg(SyncModeDialog smDlg) {
        this.smDlg = smDlg;
    }

    public SyncModeDialog getSmDlg() {
        return smDlg;
    }
    //#endif
    
    //#ifdef MUJMAIL_FS
    /**
     * Form used for choosing mode of synchronisation.
     * @see #LOCAL
     * @see #REMOTE
     */
    public class SyncModeDialog extends Form {
        ChoiceGroup syncModeCG;
        Command Ok, Cancel;
        
        public SyncModeDialog(String title) {
            super(title);
            //Set ACTION
            syncModeCG = new ChoiceGroup(Lang.get(Lang.SYNC_SELECT_MODE), ChoiceGroup.EXCLUSIVE);
            syncModeCG.insert(0, Lang.get(Lang.SYNC_MODE_LOCAL), null);
            syncModeCG.insert(1, Lang.get(Lang.SYNC_MODE_REMOTE), null);
            append(syncModeCG);
            
            Ok = new Command(Lang.get(Lang.BTN_OK), Command.OK, 0);
            Cancel = new Command(Lang.get(Lang.BTN_CANCEL), Command.CANCEL, 1);
            
            addCommand(Ok);
            addCommand(Cancel);
            setCommandListener(main.getSync());
        }
    }
    //#endif

    //#ifdef MUJMAIL_FS
    /**
     * Used to add attached file. Instance of this class is passed to instance of
     * class FileSystemBrowser and called by it when some file is chosen.
     */
    private class RestoreFSBrowserOKAction implements Callback {
        
        public void callback(Object called, Object message) {
            // if user did not choose any file, no action is needed
            if (((String)message).equals("canceled")) 
                return;
            FileSystemBrowser fsBrowser = (FileSystemBrowser)called;
            filePath = fsBrowser.getSelectedURL();
            restoreData(MODE);
        }
    }
    //#endif
    
    //#ifdef MUJMAIL_FS
    /**
     * The method callback will be called after user selects directory to export
     * the settings.
     * 
     * Lets user write the filename of the exported settings file (default will be 
     * "mujmail.cfg").
     * 
     */
    private class BackupFSBrowserOKAction implements Callback {
        
        public void callback(Object called, Object message) {
            // if user did not choose any file, no action is needed
            if (((String) message).equals("canceled"))
                return;
            FileSystemBrowser fsBrowser = (FileSystemBrowser) called;
            
            // lets user write the filename
            String filename = "mujmail.cfg";
            TextInputDialog dialog = new TextInputDialog(Lang.get(Lang.FS_BROWSER_ENTER_FILE_NAME),
                    filename, 55, TextField.ANY);
            dialog.start(((FileSystemBrowser) called).getDisplayable(),
                    new ChooseNameOfConfigFile(fsBrowser.getSelectedURL()));
        }
    }
    //#endif

    //#ifdef MUJMAIL_FS   
    /**
     * This class implements Callback interface for file name
     * selection when backing up settings locally
     */
    private class ChooseNameOfConfigFile implements Callback {
        private final String url;
        
        public ChooseNameOfConfigFile(String url) {
            this.url = url;
        }
        
        public void callback(Object called, Object message) {
            filePath = url + (String)message;
            /**
             * Function backupData() has to be started in a new thread,
             * as current thread is an event handler thread.
             * Otherwise deadlock can occur.
             */
            Thread th = new Thread () {
            	public void run() {
                    backupData(MODE);            		
            	}
            };
            th.start();
            
            main.getDisplay().setCurrent(main.getMenu());
        }
    }
    //#endif
       
    //#ifdef MUJMAIL_FS
    /** 
     * Shows dialog where user can select synchronisation mode
     * @see #LOCAL
     * @see #REMOTE
     */
    public void startSyncDlg(String title) {
        smDlg = new SyncModeDialog(title);
        main.getDisplay().setCurrent(smDlg);
    }
    //#endif
    
    protected String execute(String command, boolean resultOnly) throws MyException {
        //#ifdef MUJMAIL_FS
        if (MODE == REMOTE) {
        //#endif
            return super.execute(command, resultOnly);
        //#ifdef MUJMAIL_FS
        } else { //MODE == LOCAL
            try {
                dos.write(command.getBytes());
            } catch (Exception ex) {
                MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_IO_ERROR), AlertType.ERROR);
            }
            return "A01";
        }
        //#endif
    }
    
    /**
     * This function backs up all user data to given output stream.
     * Output stream can be either file or network address.
     * Caller must prepare the OutputStream before calling this function, i.e.
     * open and close it after calling this function.
     * 
     * @param syncMode		indicates where to backup data
     */
    public void backupData(int syncMode) {
        //Open OutputStream
        //#ifdef MUJMAIL_FS
        if (syncMode == LOCAL) {
            try {
                // creates the file
                conn = FilesystemFactory.getFileConnection(filePath, Connector.READ_WRITE);
                if (conn.exist()) {
                    // the file already exist
                    MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_FILE_EXISTS), AlertType.WARNING);
                    return;
                } else {
                    // the file does not exist, create new
                    conn.create();
                    dos = conn.openOutputStream();
                }
            } catch (Exception ex) {
                MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_OPEN_FILE), AlertType.ERROR);
                ex.printStackTrace();
                return;
            }
        } else if (syncMode == REMOTE) {
        //#endif
            //open extended IMAP4 connection to mujMail server
            //TODO open() performs authorization
            try {
                
                if (isConnected() == false) 
                    open(null);
                
                //Send "Xmujmail-syncsrv"
                String tag = execute("Xmujmail-syncsrv " + serverAcct.getUserName() + " " + serverAcct.getPassword(), false);
                
                if (DEBUG) { System.out.println("DEBUG Sync.backupData - Waiting for line"); }
                
                //Parse reply
                String reply = connection.getLine();
                if (DEBUG) { System.out.println("DEBUG Sync.backupData - Line got: "+reply); }
                
                if (reply.startsWith(tag)) {
                    if (DEBUG) { System.out.println("DEBUG Sync.backupData - Line starts with tag"); }
                    String errorTag = tag + " BAD ";
                    if (reply.startsWith(errorTag)) {
                        if (DEBUG) { System.out.println("DEBUG Sync.backupData - Error report IMAP4.synchronizeServer:" + reply); }
                        throw new MyException(MyException.PROTOCOL_BASE, "200: " + "Internal error - bad syntax" + reply.substring( errorTag.length()));
                    }
                    
                    String problemTag = tag + " NO ";
                    if (reply.startsWith(problemTag)) {
                        if (DEBUG) { System.out.println("DEBUG Sync.backupData - Error report IMAP4.synchronizeServer:" + reply); }
                        throw new MyException(MyException.PROTOCOL_CANNOT_GET_URL, "200: " + Lang.get(Lang.ALRT_INPL_IMAP_GETURL_NO_PAGE) + "Debug server reply:" + reply);
                    }
                    //close(null, null);
                    return;
                }
                
                if (DEBUG) { System.out.println("DEBUG Sync.backupData - Checking for correct answer: -* " + tag + "Ready-"); }
                //Check for correct reply: e.g. "* 001 Ready"
                if (reply.startsWith("* " + tag + "Ready") == false) {
                    throw new MyException(MyException.PROTOCOL_BASE, "200: " + "Internal error - unknown server reply 1- " + reply);
                }
                if (DEBUG) { System.out.println("DEBUG Sync.backupData - Correct answer received"); }
                
            } catch (MyException ex) {
                main.alert.setAlert(null, null, Lang.get(Lang.EXP_PROTOCOL_CANNOT_CONNECT), MyAlert.DEFAULT, AlertType.WARNING);
                //close(null, null);
                return;
            }
        //#ifdef MUJMAIL_FS
        }
        //#endif
        
        try {
            //*** SYNC ITEMS ***
            if (DEBUG) { System.out.println("DEBUG Sync.backupData - Backing up accounts"); }
            backupAccounts(main.getMailAccounts());
            
            if (DEBUG) { System.out.println("DEBUG Sync.backupData - Backing up addressbook"); }
            backupAddressBook(main.getAddressBook());
            
            backupSettings(main.getSettings());
        } catch (MyException ex) {
            ex.printStackTrace();
        }
        
        //close OutputStream
        //#ifdef MUJMAIL_FS
        if (syncMode == LOCAL) {
            try {
                if (DEBUG) { System.out.println("DEBUG Sync.backupData -CLOSING OUTPUTSTREAM"); }
                dos.close();
                conn.close();
                if (DEBUG) { System.out.println("DEBUG Sync.backupData - OUTPUTSTREAM CLOSED"); }
            } catch (Exception e) {
                MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_CLOSE_FILE), AlertType.ERROR);
            }
        } else if (syncMode == REMOTE) {
        //#endif
            //String tag, reply;
            try {
                execute("Done", false);
                connection.getLine();
            } catch (MyException ex) {
            	//close(null, null);
                ex.printStackTrace();
            }
            //TODO Error handling
        //#ifdef MUJMAIL_FS
        }
        //#endif
    }
    
    /**
     * This function restores mujMail user data from given location.
     *
     *  @param syncMode            indicates where to restore user data from
     */
    public void restoreData(int syncMode) {
        String messageHeader = null;
        
        //#ifdef MUJMAIL_FS
        if (syncMode == LOCAL) {
            //choose file
            //open file as InputStream
            try {
                conn = FilesystemFactory.getFileConnection(filePath, Connector.READ);
                dis = conn.openInputStream();
            } catch (Exception ex) {
                MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_OPEN_FILE), AlertType.ERROR);
                ex.printStackTrace();
            }
            messageHeader = "Syncentry ";
        } else if (syncMode == REMOTE) {
        //#endif
            //open srv as InputStream
            try {
                if (isConnected() == false) {
                    open(null);
                }
                //Send "Xmujmail-synccli"
                execute("Xmujmail-synccli " + serverAcct.getUserName() + " " + serverAcct.getPassword(), false);
            } catch (MyException ex) {
                main.alert.setAlert(null, null, Lang.get(Lang.EXP_PROTOCOL_CANNOT_CONNECT), MyAlert.DEFAULT, AlertType.WARNING);
            }
            messageHeader = "* Syncentry";
        //#ifdef MUJMAIL_FS
        }
        //#endif
        
        try {
            //Receive first account
            String message = getData();
            //While this message is not "Done"
            while (message.startsWith(messageHeader)) {
                //parse message
                String syncDataStr = message.substring(message.indexOf(messageHeader) + messageHeader.length());
                
                String entryType = syncDataStr.substring(syncDataStr.indexOf("EntryType: ") + 11,
                                                         syncDataStr.indexOf("\n"));
                
                //get rid of first line, which contains "EntryType: blahblah"
                syncDataStr = syncDataStr.substring(syncDataStr.indexOf("\n") + 1);
                
                if ("Account".equals(entryType)) {
                    MailAccount account = MailAccountPrimary.parseAccountString(syncDataStr);
                    String accountKey   = account.getEmail();
                    if (account.getType() == MailAccount.POP3) {
                        account.setProtocol( new POP3(account) );
                    } else {
                        account.setProtocol( new IMAP4(account) );
                    }
                    //save argument as account
                    main.getAccountSettings().saveAccount(accountKey, account);
                } else if ("Contact".equals(entryType)) {
                    AddressBook addressBook = main.getAddressBook();
                    AddressBook.Contact contact = AddressBook.Contact.parseContact(syncDataStr);
                    
                    //remove the contact if it already exists
                    AddressBook.Contact existingContact = (AddressBook.Contact)addressBook.getEmailHash().get(contact.getEmail());
                    if (existingContact != null) {
                        int index = main.getAddressBook().getAddresses().indexOf(existingContact);
                        main.getAddressBook().delete(index, true);
                    }
                    
                    main.getAddressBook().saveContact(contact);
                } else if ("Settings".equals(entryType)) {
                    main.getSettings().parseAndSetup(syncDataStr);
                }

                //get next syncData
                message = getData();
                if (message == null)
                    break;
            }
        } catch (Exception ex) {
            MujMail.mujmail.alert.setAlert("Cannot retrieve configuration data", AlertType.ERROR);
            return;
        }
        
        if (DEBUG) { System.out.println("DEBUG Sync.restoreData - RESTORE ENDED: MODE="+MODE); }
        //#ifdef MUJMAIL_FS
        if (MODE == LOCAL) {
            try {
                if (DEBUG) { System.out.println("DEBUG Sync.restoreData - CLOSING CONNECTION"); }
                dis.close();
                conn.close();
                if (DEBUG) { System.out.println("DEBUG Sync.restoreData - CONNECTION CLOSED"); }
            } catch (Exception ex) {
                MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_CLOSE_FILE), AlertType.ERROR);
            }
        }
        //#endif
    }
    
    /**
     * Backs up mail accounts created by user.
     * @param accounts List of accounts to be saved.
     */
    protected synchronized void backupAccounts(Hashtable accounts) throws MyException {
        //String tag;
        if (accounts.isEmpty()) {
            return;
        }
        
        Enumeration accountsList = accounts.elements();
        //While exists non-sent account
        while (accountsList.hasMoreElements()) {
            //send this account
            MailAccount acct = (MailAccount)accountsList.nextElement();
            execute("Syncentry EntryType: Account\n" + acct.toString(), false);
        }
    }
    
    /**
     * This function backs up address book. Throws
     * MyException exception.
     */
    protected void backupAddressBook(AddressBook addressbook) throws MyException {
        
        Vector addresses = addressbook.getAddresses();
        if (addresses.isEmpty()) return;
        
        Enumeration addressList = addresses.elements();
        
        //While exists non-sent account
        while (addressList.hasMoreElements()) {
            //send this account
            AddressBook.Contact contact = (AddressBook.Contact)addressList.nextElement();
            execute("Syncentry " + "EntryType: Contact\n" + contact.toString(), false);
            
            //parse reply
/*          connection.getLine();
            if (reply.startsWith(tag + "OK") == false)
                throw new MyException(MyException.PROTOCOL_BASE,
                                      "200: " + "Internal error - unknown server reply 2 - " + reply); */
        }
    }

    protected void backupSettings(Settings settings) throws MyException {
        //String tag, reply;
        if (settings == null) return;
        
        execute("Syncentry " + "EntryType: Settings\n" + settings.toString(), false);
        
        return;
    }

    /**
     * This function gets synchronization data string, which is a
     * string of characters of the following structure:
     *
     * Param1: Value1\nParam2: Value2\n...\nLastParam: LastValue\n\n
     *
     * @return                    returns synchronization data string
     */
    protected String getData() {
        String str = null;
        StringBuffer sb = null;
        StringBuffer result = null;
        int ch;
        
        //#ifdef MUJMAIL_FS
        if (MODE == LOCAL)
            sb = new StringBuffer();
        //#endif
        
        //Get first line
        try {
            
            if (MODE == REMOTE) {
                str = connection.getLine();
            }
            //#ifdef MUJMAIL_FS
            else {
                // MODE == LOCAL
                try {
                    while ((ch = dis.read()) != -1) {
                        sb.append((char)ch);
                        if ((char)ch == '\n')
                            break;
                    }
                    if (ch == -1)
                        return null;
                    str = sb.toString();
                    sb.delete(0, sb.length());
                } catch (IOException ex) {
                    MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_FILE_READ_ERROR), AlertType.WARNING);
                }
            }
            //#endif
            
            //Case when we receive "TAG OK Completed"
            if (str.length() > 1 && str.indexOf(":") == -1) {
                return null;
            }
    
            result = new StringBuffer(str);
            while (!str.startsWith("\n")) {
                if (MODE == REMOTE) {
                    str = connection.getLine();
                }
                //#ifdef MUJMAIL_FS
                else {
                    // MODE == LOCAL
                    try {
                        while ((ch = dis.read()) != -1) {
                            sb.append((char)ch);
                            if ((char)ch == '\n')
                                break;
                        }
                        if (ch == -1)
                            return null;
                        str = sb.toString();
                        sb.delete(0, sb.length());
                    } catch (IOException ex) {
                        MujMail.mujmail.alert.setAlert(Lang.get(Lang.EXP_FS_FILE_READ_ERROR), AlertType.WARNING);
                    }
                }
                //#endif
                result.append(str);
            }
            
            //Eat up newline
            if (MODE == REMOTE)
                connection.getLine();
        }
        catch(MyException e) {
            e.printStackTrace();
        }

        if (DEBUG) { System.out.println("DEBUG Sync.getData - RESULT OF GETDATA=START"+result+"END"); }
        return result.toString();
    }

    public void commandAction(Command c, Displayable d) {
        //#ifdef MUJMAIL_FS
        if (d == smDlg) {
            if (c == smDlg.Ok) {
                MODE = smDlg.syncModeCG.getSelectedIndex();
        //#else
//#                     MODE = Sync.REMOTE; // No File system support --> only remote synchronisation
        //#endif
                if (this.ACTION == Sync.BACKUP) {
                    if (DEBUG) { System.out.println("DEBUG Sync.CommandAction - MODE="+MODE); }
                    if (MODE == REMOTE) {
                    	// Backup procedure has to be started in a
                    	// different thread than event handler thread
                    	// (thread running commandAction()), otherwise
                    	// deadlock can occur
                    	Thread th = new Thread() {
                    		public void run() {
                                backupData(MODE);                    			
                    		}
                    	};
                    	th.start();
                        main.getDisplay().setCurrent(main.getMenu());
                    }
                    //#ifdef MUJMAIL_FS
                    else { // MODE == LOCAL
                        if (DEBUG) { System.out.println("DEBUG Sync.CommandAction - Starting FileSystemBrowser"); }
                        Callback action = new BackupFSBrowserOKAction();
                        FileSystemBrowser FSBrowser = new FileSystemBrowser(this.main, this.main.getMenu(), action, FileSystemBrowser.ChoosingModes.DIRECTORIES, Lang.get(Lang.FS_BROWSER_SELECT_FILE));
                        FSBrowser.startBrowser(StartupModes.IN_NEW_THREAD);
                    }
                    //#endif
                } else { //RESTORE
                    if (DEBUG) { System.out.println("DEBUG Sync.CommandAction - MODE="+MODE); }
                    if (MODE == REMOTE) {
                    	// Backup procedure has to be started in a
                    	// different thread than event handler thread
                    	// (thread running commandAction()), otherwise
                    	// deadlock can occur
                    	Thread th = new Thread() {
                    		public void run() {
                                restoreData(MODE);                    			
                    		}
                    	};
                    	th.start();
                        //main.getDisplay().setCurrent(main.getMenu());
                    }
                    //#ifdef MUJMAIL_FS
                    else { // MODE == LOCAL
                        if (DEBUG) { System.out.println("DEBUG Sync.CommandAction - Starting FileSystemBrowser"); }
                        Callback action = new RestoreFSBrowserOKAction();
                        FileSystemBrowser FSBrowser = new FileSystemBrowser(this.main, this.main.getMenu(), action, FileSystemBrowser.ChoosingModes.FILES, Lang.get(Lang.FS_BROWSER_SELECT_FILE));
                        FSBrowser.startBrowser(StartupModes.IN_NEW_THREAD);
                    }
                //#endif
                }
            //#ifdef MUJMAIL_FS
            } else if (c == smDlg.Cancel) {
                main.getDisplay().setCurrent(main.getMenu());
            }
            //#endif
        //#ifdef MUJMAIL_FS
        }
        //#endif
    }

}

