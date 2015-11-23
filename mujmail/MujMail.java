/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2006 Martin Stefan <martin.stefan@centrum.cz>
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

package mujmail;


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
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Alert;

import mujmail.account.AccountSettings;
import mujmail.account.MailAccount;
//#ifdef MUJMAIL_SYNC
import mujmail.account.MailAccountPrimary;
//#endif
import mujmail.protocols.SMTP;
import mujmail.threading.Algorithm;
import mujmail.util.Functions;
//#ifdef MUJMAIL_SEARCH
import mujmail.search.SearchBox;
//#endif
//#ifdef MUJMAIL_SYNC
import mujmail.account.Sync;
//#endif
//#ifdef MUJMAIL_USR_FOLDERS
import mujmail.mailboxes.BoxList;
//#endif
//#ifdef MUJMAIL_DEBUG_CONSOLE
import mujmail.debug.DebugConsole;
//#endif

public class MujMail extends MIDlet implements CommandListener {

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public static final String VERSION = "v1.08.08";
    public static final int BUILD = 20090402;

    public static Properties props;

    //private Lang language = new Lang();
    private MyDisplay myDisplay = new MyDisplay(this);
    public MyAlert alert = new MyAlert(this);
    private Settings settings = new Settings(this);
    private final AccountSettings accountSettings = new AccountSettings(this);
    private AddressBook addressBook = new AddressBook(this);
    final OutBox outBox;
    final PersistentBox sentBox;
    //#ifdef MUJMAIL_SEARCH
    final private SearchBox searchBox;
    //#endif
    private final InBox inBox;
    final OutBox draft;
    private final Trash trash;
    //#ifdef MUJMAIL_SYNC
    private final Sync sync;
    //#endif
    public MailForm mailForm = new MailForm(this);
    public SendMail sendMail = new SendMail(this);
    About about = new About(this);
    //private Debug debug = null;
    private Hashtable/*<String, MailAccountPrimar>*/ mailAccounts;
    ClearDBSelect clearDBSelect;
    private Menu menu = null;
    private boolean initialised = false;
    //#ifdef MUJMAIL_USR_FOLDERS
    private BoxList userMailBoxes;
    //#endif

    Displayable lastDisplay; //last screen displayed before the application goes minimized, paused..
    
    private MailDBSeen mailDBSeen;
    private MailDBManager mailDBManager; 
    
    public static MujMail mujmail; //to get pointer to mujmail easier from anywhere

    /**
     * Represents the Display object. Provides more functionality than
     * javax.microedition.lcdui.Display.
     *
     * @see javax.microedition.lcdui.Display
     *
     */
    public static class MyDisplay {
        private final Display display;
        private boolean settingCurrEnabled = true;

        /**
         * Creates new instance of MyDisplay.
         * @param middlet the midlet that display object will be created.
         */
        public MyDisplay(MIDlet middlet) {
            display = Display.getDisplay(middlet);
        }

        /**
         * Disables setting current screen. This means that calling
         * {@link #setCurrent} will have no effect.
         */
        public void disableSettingCurrent() {
            settingCurrEnabled = false;
        }

        /**
         * Enables setting current screen. This means that calling
         * {@link #setCurrent} will set new screen as current.
         */
        public void enableSettingCurrent() {
            settingCurrEnabled = true;
        }

        /**
         * Sets nextDisplayable as current screen.
         *
         * @param nextDisplayable Displayable object that will be setted as
         *  current screen.
         * @see javax.microedition.lcdui.Display
         */
        public void setCurrent(Displayable nextDisplayable) {
            if (settingCurrEnabled) {
                display.setCurrent(nextDisplayable);
            }
        }

        /**
         * Sets alert as current screen.
         *
         * @param alert the alert to be setted as current screen.
         * @param nextDisplayable the Displayable object that will be setted
         *  as current screen after alert.
         * @see javax.microedition.lcdui.Display
         */
        public void setCurrent(Alert alert, Displayable nextDisplayable) {
            if (settingCurrEnabled) display.setCurrent(alert, nextDisplayable);
        }

        /**
         * Gets current screen.
         * @return gets current screen.
         * @see javax.microedition.lcdui.Display
         */
        public Displayable getCurrent() {
            return display.getCurrent();
        }
        
    }

    public MujMail() {
        if (DEBUG) { DebugConsole.printlnPersistent("MujMail() - start"); }
        mujmail = this;
        
        mailDBManager = new MailDBManager(); // Have to be created before any PersistentBox
        mailDBSeen = new MailDBSeen(this);

        outBox = new OutBox("OUTBOX", this, Lang.get(Lang.TB_OUTBOX));
        sentBox = new PersistentBox("SENTBOX", this, Lang.get(Lang.TB_SENTBOX));
        //#ifdef MUJMAIL_SEARCH
        searchBox = new SearchBox(this);
        //#endif
        inBox = new InBox("INBOX", Lang.get(Lang.TB_INBOX));
        draft = new OutBox("DRAFT", this, Lang.get(Lang.TB_DRAFT));
        trash = new Trash("TRASH", this, Lang.get(Lang.TB_TRASH));
        
        //#ifdef MUJMAIL_USR_FOLDERS
        userMailBoxes = new BoxList(this);
        //#endif

        //#ifdef MUJMAIL_SYNC
        sync = new Sync(this, new MailAccountPrimary(MailAccount.IMAP,
                "login@mujmail.org",
                true,
                Settings.mujMailSrvAddr,
                Short.parseShort(Settings.mujMailSrvPort),
                Settings.mujMailSrvLogin,
                Settings.mujMailSrvPasswd,
                false,
                (byte)0,
                false,
                false,
                "",
                ""));
        //#endif
        
        mailAccounts = new Hashtable();
        menu = new Menu(this);
        clearDBSelect = new ClearDBSelect(this);
        if (DEBUG) { DebugConsole.printlnPersistent("MujMail() - end"); };
    }

    public void pauseApp() {
        lastDisplay = myDisplay.getCurrent();
    }

    public void destroyApp(boolean unconditional) {
        initialised = false;
        getInBox().clearLastSafeMail();
        discServers(true);
        notifyDestroyed();
    }

    public void startApp() {
        if (DEBUG) { DebugConsole.printlnPersistent("MujMail.startApp - start"); };
        if (!Settings.password.equals("")) {
            EnterInitialPasswordForm initialPassword = new EnterInitialPasswordForm();
            getDisplay().setCurrent(initialPassword);
        } else {
            myStartApplication();
        }
        if (DEBUG) { DebugConsole.printlnPersistent("MujMail.startApp - end"); };
    }

    /**
     * The form for entering the password before starting the application.
     * If the password is correct, starts the application.
     * Allows user to clear all databases and than start the application without
     * entering password.
     */
    private class EnterInitialPasswordForm extends Form implements CommandListener {
        private TextField passwordText;
        private Command okCommand = new Command("OK", Command.OK, 0);
        private Command cancelCommand = new Command("Exit", Command.EXIT, 1);
        private Command clearAllDBCommand = new Command("Clear all databases", Command.ITEM, 2);

        public EnterInitialPasswordForm() {
            super("Enter password");

            StringItem description = new StringItem("", "To start the application, enter the password or clear all databases with mails and settings.");
            passwordText = new TextField("Password", "", 50, TextField.PASSWORD);

            append(description);
            append(passwordText);
            addCommand(okCommand);
            addCommand(cancelCommand);
            addCommand(clearAllDBCommand);
            setCommandListener(this);
        }

        public void commandAction(Command c, Displayable d) {
            if (c == cancelCommand) {
                destroyApp(true);
                return;
            }

            if (c == okCommand) {
                if (!Settings.password.equals(passwordText.getString())) {
                    return;
                }
            }

            if (c == clearAllDBCommand) {
                ClearDBSelect clearDB = new ClearDBSelect(mujmail);
                clearDB.clearDataBases(true, true);
            }

            mainMenu();
            // start the application
            myStartApplication();
        }

        

    }

    private void myStartApplication() {
        if (DEBUG) { DebugConsole.printlnPersistent("MujMail.myStartApplication - start"); };

        try {
        boolean showAccountForm = false;
        if (!initialised) {
            //first run, initiation needed
            initialised = true;
            showAccountForm = getAccountSettings().getNumAccounts() > 0 ? false : true;

            getSettings().initSortMode();

            //#ifdef MUJMAIL_USR_FOLDERS
            // Loads mails into all mailboxe in serial way
            userMailBoxes.loadBoxes();
            //#else
//#             // We have to asynchronous load standard mail boxes
//#             Thread boxLoader = new Thread() {
//#                 public void run() {
//#                     loadDefaulFolders();
//#                 }
//#             };
//#             boxLoader.start();
            //#endif

            Properties.showStartupAlerts(alert); // Show warnings about abilities of your mobile

            // We have to load accounts every time, even if no acocunt exists -> Synchronisation
            //    We have to set flag that accounts are loaded to be able add new user folders
            //    Otherwise if no account on start exists, creating User folder will block
            getAccountSettings().loadAccounts();
            // show an account form initialized by default values
            if (showAccountForm) {
                getAccountSettings().showAccount("DEFAULT_INIT");
                return;
            }
            // Toto Test
            if ( getMenu() != null ) {
                    getMenu().setSelectedTab(Menu.ACTION);
                    getMenu().refresh(Menu.ACTION, !showAccountForm);
            } else {
                System.out.println("MujMail.myStartApplication() - menu is null");
            }
        } else {
            myDisplay.setCurrent(lastDisplay);
        } //show the original display before that
        } catch ( Throwable t ) {
            System.out.println("MujMail.myStartApplication() - exception");
            t.printStackTrace();
        }
    }

    
    
    public synchronized void commandAction(Command c, Displayable d) {
        if (DEBUG) System.out.println("DEBUG MujMai.commandAction - start");
        if (c == getSettings().back) {
            settings.loadSettings();
            myDisplay.setCurrent(getMenu());
        } else if (c == getSettings().ok) {
            getSettings().saveSettings(true);
        }

        if (d == clearDBSelect) {
            if (c == clearDBSelect.OK) {
                clearDBSelect.clearDataBases(false, false);
            } else {
                myDisplay.setCurrent(getMenu());
            }
        } else if (d == getAccountSettings()) {
            if (c == getAccountSettings().back) {
            	//This function has to be called as it refreshes the menu
            	mainMenu();
            } else if (c == getAccountSettings().ok) {
                getAccountSettings().saveAccount(getMenu().getSelectedAccount(), null);
            }
        } else if (d == sendMail) {
            //#ifdef MUJMAIL_FS
            if (c == sendMail.getAttachementsAdder().attach) {
                sendMail.getAttachementsAdder().attachFileSelection();
            }
            
            if (c == sendMail.getAttachementsAdder().remove) {
                sendMail.getAttachementsAdder().removeAllAttachments();
            }
            //#endif
            if (c == sendMail.bc) {
                sendMail.addBc();
            } else if (c == sendMail.send) {
                sendMail.selectedField = null;
                OutBox box = (c == sendMail.draft) ? draft : outBox;
                if (box.isBusy()) {
                    alert.setAlert(null, sendMail, Lang.get(Lang.ALRT_SYS_BUSY), MyAlert.DEFAULT, AlertType.INFO);
                } else {
                    myDisplay.setCurrent(box);
                    box.sendSingle(box.addOutMail(sendMail));
                }
            } else if (c == sendMail.sendLater || c == sendMail.draft) {
                sendMail.selectedField = null;
                OutBox box = (c == sendMail.draft) ? draft : outBox;
                if (box.isBusy()) {
                    alert.setAlert(null, sendMail, Lang.get(Lang.ALRT_SYS_BUSY), MyAlert.DEFAULT, AlertType.INFO);
                } else {
                    if (sendMail.mode != SendMail.NORMAL) {
                        myDisplay.setCurrent(sendMail.callBox);
                    }
                    box.addOutMail(sendMail);
                    mainMenu();
                }
            } else if (c == sendMail.editBody) {
                sendMail.editBody();
            } else if (c == sendMail.clear) {
                sendMail.clear();
            } else if (c == sendMail.cancel) {
                sendMail.selectedField = null;
                if (sendMail.mode != SendMail.NORMAL) {
                    if (sendMail.mode == SendMail.FORWARD) {
                        sendMail.delete(sendMail.size() - 1);
                    }
                    sendMail.clear();
                    sendMail.mode = SendMail.NORMAL;
                }

                sendMail.showNextScreen();
            } else if (c == sendMail.addRcp) {
                getAddressBook().addEmails(sendMail);
            } else if (c == sendMail.preview) {
                mailForm.previewMessage();
            } else if (c == sendMail.chooseAccount) {
                sendMail.chooseAccounts();
            } 
        } else if (d == sendMail.editbodyTB) {
            if (c == sendMail.updateBody) {
                sendMail.updateBody();
            } else if (c == sendMail.cancelBody) {
                sendMail.cancelBody();
            }
        } else if (d == sendMail.accountsForm) {
            if (c == sendMail.ok) {
                sendMail.selectFromAccount();
            } else if (c == sendMail.cancel) {
                myDisplay.setCurrent(sendMail);
            }
        } else if (d == mailForm) {
            if (c == mailForm.back) {
                if (mailForm.getContext() == MailForm.MODE_BASIC) {
                    mailForm.back();
                } // it was an attachment's detail so return to attachment list
                else {
                    myDisplay.setCurrent(mailForm.attchList);
                }
            } else if (c == mailForm.listAttachments) {
                mailForm.listAttachments();
            } else if (c == mailForm.showAddresses) {
                mailForm.listMailAddr();
            } else if (c == mailForm.showHeader) {
                mailForm.showHeader(mailForm.msgHeader, mailForm);
            //#ifdef MUJMAIL_FS
            } else if (c == mailForm.exportToFS) {
                mailForm.exportToFilesystem();
            //#endif
            } else if (c == mailForm.forward) { 
                System.out.println("Forward from mailForm");
                sendMail.initForward(mailForm.callBox, mailForm);
            } //else if (c == mailForm.edit) {
                //commandAction(mailForm.callBox.edit, mailForm.callBox);
            //}
            else if (c == mailForm.edit) {
                MessageHeader om = mailForm.msgHeader;
                if (om != null) {
                    sendMail.edit(om, mailForm);
                }
            } else if (c == mailForm.reply) {
                if (getInBox().getSelectedIndex() < getInBox().getStorage().getSize()) {
                    sendMail.reply(getInBox().getSelectedHeader());
                }
            } else if (c == mailForm.quotedReply) {
                if (getInBox().getSelectedIndex() < getInBox().getStorage().getSize()) {
                    sendMail.quotedReply(getInBox().getSelectedHeader());
                }
            } else if (c == mailForm.replyAll) {
                if (getInBox().getSelectedIndex() < getInBox().getStorage().getSize()) {
                    sendMail.replyAll(getInBox().getSelectedHeader());
                }
            } else if (c == mailForm.delete) {
                myDisplay.setCurrent(mailForm.callBox);
                mailForm.callBox.commandAction(mailForm.callBox.delete, mailForm.callBox);
            }

        } else if (d == mailForm.mailAdrList) {
            if (c == mailForm.back) {
                myDisplay.setCurrent(mailForm);
            } else if (c == mailForm.addMailToBook) {
                mailForm.saveContacts();
            }
        } else if (d == mailForm.attchList) {
            if (c == mailForm.back) {
                if (mailForm.getContext() == MailForm.MODE_LIST) {
                    mailForm.back();
                } else {
                    mailForm.setContext(MailForm.MODE_BASIC);
                    myDisplay.setCurrent(mailForm);
                }
            } else if (c == mailForm.deleteAttachment) {
                mailForm.deleteBodyPart((byte) (mailForm.attchList.getSelectedIndex()));
            } else if (c == mailForm.redownAttchment) {
                mailForm.regetAndList(getInBox().getSelectedHeader(),
                        (byte) (mailForm.attchList.getSelectedIndex()));
            } else if (c == mailForm.showAddresses) {
                mailForm.listMailAddr();
            //#ifdef MUJMAIL_FS
            } else if (c == mailForm.exportBPToFS) {
                mailForm.exportBPToFS((byte) mailForm.attchList.getSelectedIndex());
            //#endif
            } else if (c == mailForm.displayAsText) {
                mailForm.viewBodyPart((byte) (mailForm.attchList.getSelectedIndex()), MailForm.BPViewingModes.AS_TEXT);
            } else if (c == mailForm.viewConverted) {
            	mailForm.viewBodyPart((byte) (mailForm.attchList.getSelectedIndex()), MailForm.BPViewingModes.CONVERTED );
            } else {
                mailForm.viewBodyPart((byte) (mailForm.attchList.getSelectedIndex()), MailForm.BPViewingModes.NOT_SPECIFIED);
            }
        } else if (d == mailForm.headerForm) {
            if (c == mailForm.back) {
                mailForm.setContext(MailForm.MODE_BASIC);
                mailForm.showPreviousScreen();
            }
        } else if (d == getAddressBook()) {
            if (c == getAddressBook().add) {
                getAddressBook().showCntForm(null);
            } else if (c == getAddressBook().delete) {
                getAddressBook().delete(getAddressBook().getSelectedIndex(), false);
            } else if (c == getAddressBook().edit) {
                getAddressBook().edit(getAddressBook().getSelectedIndex());
            } else if (c == getAddressBook().delAll) {
                getAddressBook().deleteAll(false);
            } else if (c == getAddressBook().view) {
                getAddressBook().view(getAddressBook().getSelectedIndex());
            } else if (c == getAddressBook().sendMail) {
                getAddressBook().sendMail(getAddressBook().getSelectedIndex());
            } else if (c == getAddressBook().mark) {
                getAddressBook().markEmail(getAddressBook().getSelectedIndex());
            } else if (c == getAddressBook().done) {
                getAddressBook().pasteEmails();
            } else if (c == getAddressBook().flipRcps) {
                getAddressBook().flipRecipients();
            } else if (c == getAddressBook().back) {
                getAddressBook().back();
            }
        } else if (d == getAddressBook().cntForm) {
            if (c == getAddressBook().cfBack) {
                myDisplay.setCurrent(getAddressBook());
            } else if (c == getAddressBook().cfSave) {
                getAddressBook().saveContactForm();
            }
        } else if (d == getAddressBook().viewForm) {
            myDisplay.setCurrent(getAddressBook());
        } else if (d == getSettings().sortForm) {
            if (c == getSettings().ok) {
                getSettings().saveSortSettings( getSettings().sortForm.box);
                final Enumeration enum1 = getSettings().sortForm.box.getStorage().getEnumeration();
                MessageHeader messageHeader;
                final Vector/*<MessageHeader>*/ messageHeaders = new Vector();
                while ( enum1.hasMoreElements() ) {
                	messageHeader = (MessageHeader)enum1.nextElement();
                	messageHeaders.addElement( messageHeader );
                }
                TheBox sortedBox = getSettings().sortForm.box; 
                sortedBox.setStorage( Algorithm.getAlgorithm().invoke( messageHeaders ) );
                sortedBox.resort();
                if ( sortedBox instanceof InBox ) {
                    ((InBox)sortedBox).setCurFirstUnread();
                }
            }
            myDisplay.setCurrent(getSettings().sortForm.box);
        } else if (d == about) {
            if (c == about.feedBack) {
                sendMail.subject.setString("mujMail feedback");
                sendMail.to.setString("support@mujMail.org");
                sendMail.writeMail(d);
            } else {
                myDisplay.setCurrent(getMenu());
            }
        } else if (d == alert.alertWindow) {
            MyAlert.AlertJob al = alert.lastJob;
            switch (al.mode) {
                case MyAlert.DB_CLEAR_CONFIRM:
                    if (c == alert.OK) {
                        if (al.callObject == getAddressBook()) {
                            getAddressBook().deleteAll(true);
                        } else if (al.callObject == getAccountSettings()) {
                            getAccountSettings().deleteAll(true);
                        } else if (al.callObject == clearDBSelect) {
                            clearDBSelect.clearDataBases(true, false);
                        } else {
                            ((TheBox) al.callObject).deleteAllMailsFromBoxAndDB(true);
                        }
                    }
                    break;
                case MyAlert.DEL_CONFIRM:
                    if (c == alert.OK) {
                        if (al.callObject == getAccountSettings()) {
                            getAccountSettings().deleteAccount(getMenu().getSelectedAccount(), true);
                        } else if (al.callObject == getAddressBook()) {
                            getAddressBook().delete(getAddressBook().getSelectedIndex(), true);
                        }
                    }
                    break;
                case MyAlert.EXIT_BUSY_CONFIRM:
                    if (c == alert.OK) {
                        destroyApp(false);
                    }
                    break;
                case MyAlert.DEFAULT:
                    break;
            }
            myDisplay.setCurrent(alert);
        }

    }
   
    /// Disconnect from connected servers (SMTP, POP..)
    public void discServers(boolean forcedClose) {
        SMTP.getSMTPSingleton(this).close(forcedClose, outBox);
        for (Enumeration e = getMailAccounts().elements(); e.hasMoreElements();) {
            MailAccount account = (MailAccount) e.nextElement();
            account.getProtocol().close(forcedClose, getInBox()); // Alf: Change
        }
        
        if(getInBox().isPushActive()){                
            getInBox().stopPush();
            getMenu().refreshAll(true);
        }
    }

    /** Loads defaults folders in mujMail serially.
     * (Inbox, OutBox, SentBox, Drafts, Trash)
     * Note: It's called from separate thread ( BoxList.loadingBoxesThread)
     */
    public void loadDefaulFolders() {
        getInBox().getMailDB().loadDB( getInBox());
        synchronized (getInBox().getMailDB()) {
            getInBox().getMailDB().waitForTaskEnd();
        }

        getOutBox().getMailDB().loadDB( getOutBox());
        synchronized (getOutBox().getMailDB()) {
            getOutBox().getMailDB().waitForTaskEnd();
        }

        getSentBox().getMailDB().loadDB( getSentBox());
        synchronized (getSentBox().getMailDB()) {
            getSentBox().getMailDB().waitForTaskEnd();
        }
        
        getTrash().getMailDB().loadDB( getTrash());
        synchronized (getTrash().getMailDB()) {
            getTrash().getMailDB().waitForTaskEnd();
        }

        getDraft().getMailDB().loadDB( getDraft());
        synchronized (getDraft().getMailDB()) {
            getDraft().getMailDB().waitForTaskEnd();
        }
    }        

    /**
     * Shows main menu on display
     */
    public void mainMenu() {
        getMenu().setSelectedTab(getMenu().getSelectedTab());
        getMenu().refresh(getMenu().getSelectedTab(), true);
    }

    /// Shows about form
    private class About extends Form {

        Command back, feedBack;

        public About(MujMail mujMail) {
            super(Lang.get(Lang.ABT_ABOUT));

            Image logo = Functions.getIcon("logo.png");
            append(logo);
            append(Lang.get(Lang.ABT_TEXT));

            back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
            feedBack = new Command(Lang.get(Lang.BTN_ABT_FEEDBACK), Command.HELP, 0);
            addCommand(back);
            addCommand(feedBack);

            setCommandListener(mujMail);
        }
    }

    /// Shows form for clearing databases
    public class ClearDBSelect extends Form {
        private static final byte CLR_DB_INBOX       = 0;
        private static final byte CLR_DB_OUTBOX      = CLR_DB_INBOX + 1;
        private static final byte CLR_DB_DRAFT       = CLR_DB_OUTBOX + 1;
        private static final byte CLR_DB_SENTBOX     = CLR_DB_DRAFT + 1;
        private static final byte CLR_DB_TRASH       = CLR_DB_SENTBOX + 1;
        //#ifdef MUJMAIL_USR_FOLDERS
        private static final byte CLR_DB_USERFOLDERS = CLR_DB_TRASH + 1;
        //#else
//#         private static final byte CLR_DB_USERFOLDERS = CLR_DB_TRASH;
        //#endif
        private static final byte CLR_DB_ACCOUNTS    = CLR_DB_USERFOLDERS + 1;
        private static final byte CLR_DB_ADRESSBOOX  = CLR_DB_ACCOUNTS + 1;
        private static final byte CLR_DB_MSGIDS      = CLR_DB_ADRESSBOOX + 1;
        private static final byte CLR_DB_SETTINGS      = CLR_DB_MSGIDS + 1;
        private static final byte CLR_DB_SIZE        = CLR_DB_SETTINGS + 1; // Lease this as last element

        ChoiceGroup DB_to_delete;
        Command OK, cancel;

        public ClearDBSelect(MujMail mujMail) {
            super(Lang.get(Lang.AC_CLEAR_DB));

            DB_to_delete = new ChoiceGroup(Lang.get(Lang.DB_SPACE_LEFT), Choice.MULTIPLE);
            DB_to_delete.append(Lang.get(Lang.TB_INBOX), null);
            DB_to_delete.append(Lang.get(Lang.TB_OUTBOX), null);
            DB_to_delete.append(Lang.get(Lang.TB_DRAFT), null);
            DB_to_delete.append(Lang.get(Lang.TB_SENTBOX), null);
            DB_to_delete.append(Lang.get(Lang.TB_TRASH), null);
            //#ifdef MUJMAIL_USR_FOLDERS
            DB_to_delete.append(Lang.get(Lang.TB_USERFOLDERS), null);
            //#endif
            DB_to_delete.append(Lang.get(Lang.AS_ACCOUNTS), null);
            DB_to_delete.append(Lang.get(Lang.AD_ADDRESSBOOK), null);
            DB_to_delete.append(Lang.get(Lang.MSGIDS_CACHE), null);
            DB_to_delete.append("Settings", null);
            DB_to_delete.setSelectedFlags(new boolean[CLR_DB_SIZE]); //set all to false
            append(DB_to_delete);

            OK = new Command(Lang.get(Lang.BTN_OK), Command.OK, 0);
            cancel = new Command(Lang.get(Lang.BTN_CANCEL), Command.BACK, 0);
            addCommand(OK);
            addCommand(cancel);

            setCommandListener(mujMail);
        }

        public void refresh() {
            DB_to_delete.setLabel(Lang.get(Lang.DB_SPACE_LEFT) + Functions.spaceLeft("SPACE_TEST") / 1024 + "kB");
            int spaceOccupied = getInBox().getOccupiedSpace();
            DB_to_delete.set(CLR_DB_INBOX, Lang.get(Lang.TB_INBOX) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = outBox.getOccupiedSpace();
            DB_to_delete.set(CLR_DB_OUTBOX, Lang.get(Lang.TB_OUTBOX) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = draft.getOccupiedSpace();
            DB_to_delete.set(CLR_DB_DRAFT, Lang.get(Lang.TB_DRAFT) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = getSentBox().getOccupiedSpace();
            DB_to_delete.set(CLR_DB_SENTBOX, Lang.get(Lang.TB_SENTBOX) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = getTrash().getOccupiedSpace();
            DB_to_delete.set(CLR_DB_TRASH, Lang.get(Lang.TB_TRASH) + " (" + spaceOccupied / 1024 + "kB)", null);

            //#ifdef MUJMAIL_USR_FOLDERS
            spaceOccupied = getUserMailBoxes().spaceOccupied();
            DB_to_delete.set(CLR_DB_USERFOLDERS, Lang.get(Lang.TB_USERFOLDERS) + " (" + spaceOccupied / 1024 + "kB)", null);
            //#endif

            spaceOccupied = Functions.spaceOccupied("ACCOUNTS");
            DB_to_delete.set(CLR_DB_ACCOUNTS, Lang.get(Lang.AS_ACCOUNTS) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = Functions.spaceOccupied("AddressBook");
            DB_to_delete.set(CLR_DB_ADRESSBOOX, Lang.get(Lang.AD_ADDRESSBOOK) + " (" + spaceOccupied / 1024 + "kB)", null);

            spaceOccupied = getMailDBSeen().getOccupiedSpace();
            DB_to_delete.set(CLR_DB_MSGIDS, Lang.get(Lang.MSGIDS_CACHE) + " (" + spaceOccupied / 1024 + "kB)", null);

            DB_to_delete.set(CLR_DB_SETTINGS, "Settings", null);

        }

        /**
         * @param sure
         * @param deleteAll deletes all settings independently on which was selected.
         */
        private void clearDataBases(boolean sure, boolean deleteAll) {
            boolean[] selected = new boolean[CLR_DB_SIZE];
            if (!deleteAll && clearDBSelect.DB_to_delete.getSelectedFlags(selected) == 0) {
                myDisplay.setCurrent(getMenu());
                return;
            }

            if (!sure) {
                alert.setAlert(clearDBSelect, getMenu(),Lang.get(Lang.ALRT_SYS_DEL_ALL_CONFIRM), MyAlert.DB_CLEAR_CONFIRM, AlertType.CONFIRMATION);
                return;
            }

            if (deleteAll || selected[CLR_DB_INBOX]) {
                getInBox().deleteAllMailsFromBoxAndDB(true);
            }
            if (deleteAll || selected[CLR_DB_OUTBOX]) {
                getOutBox().deleteAllMailsFromBoxAndDB(true);
            }
            if (deleteAll || selected[CLR_DB_DRAFT]) {
                getDraft().deleteAllMailsFromBoxAndDB(true);
            }
            if (deleteAll || selected[CLR_DB_SENTBOX]) {
                getSentBox().deleteAllMailsFromBoxAndDB(true);
            }
            if (deleteAll || selected[CLR_DB_TRASH]) {
                getTrash().deleteAllMailsFromBoxAndDB(true);
            }
            //#ifdef MUJMAIL_USR_FOLDERS
            if (deleteAll || selected[CLR_DB_USERFOLDERS]) {
                getUserMailBoxes().deleteAllMailsFromAllUserBoxesAndDB();
            }
            //#endif
            
            if (deleteAll || selected[CLR_DB_ACCOUNTS]) {
                getAccountSettings().deleteAll(true);
            }
            if (deleteAll || selected[CLR_DB_ADRESSBOOX]) {
                getAddressBook().deleteAll(true);
            }
            if (deleteAll || selected[CLR_DB_MSGIDS]) {
                getMailDBSeen().deleteAll(true);
            }
            if (deleteAll || selected[CLR_DB_SETTINGS]) {
                settings.restoreSettings();
            }

            clearDBSelect.DB_to_delete.setSelectedFlags(new boolean[CLR_DB_SIZE]); //set all to false
            
                mainMenu();
        }

    }
    
    /* ***************************
     *    getters and setters    *
     *****************************/

    public AccountSettings getAccountSettings() {
        return accountSettings;
    }

    public AddressBook getAddressBook() {
        return addressBook;
    }

    public void setAddressBook(AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    public MyAlert getAlert() {
        return alert;
    }

    public MyDisplay getDisplay() {
        return myDisplay;
    }
    
    public PersistentBox getDraft() {
        return draft;
    }
    
    public InBox getInBox() {
        return inBox;
    }

    /**
     * TODO: do not return hashtable but make methods for accessing accounts
     * @return
     */
    public Hashtable/*<String, MailAccountPrimar>*/ getMailAccounts() {
        return mailAccounts;
    }

    public void setMailAccounts(Hashtable mailAccounts) {
        this.mailAccounts = mailAccounts;
    }
    
    public MailDBManager getMailDBManager() {
        return mailDBManager;
    }
    
    public MailDBSeen getMailDBSeen() {
        return mailDBSeen;
    }
    
    public Menu getMenu() {
        return menu;
    }
    
    public PersistentBox getOutBox() {
        return outBox;
    }
    
    //#ifdef MUJMAIL_SEARCH
    public SearchBox getSearchBox() {
        return searchBox;
    }
    //#endif

    public PersistentBox getSentBox() {
        return sentBox;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    //#ifdef MUJMAIL_SYNC    
    public Sync getSync() {
        return sync;
    }
    //#endif

    public Trash getTrash() {
        return trash;
    }

    //#ifdef MUJMAIL_USR_FOLDERS
    public BoxList getUserMailBoxes() {
        return userMailBoxes;
    }
    //#endif

}
