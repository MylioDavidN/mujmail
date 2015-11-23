/*
MujMail - Simple mail client for J2ME
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import mujmail.account.MailAccount;
import mujmail.tasks.TasksManager;
import mujmail.util.Functions;
import java.util.Hashtable;
import mujmail.tasks.BackgroundTask;
import mujmail.tasks.ConditionalActionRunner.SampleTestTask;
import mujmail.tasks.TasksManagerUI;
//#ifdef MUJMAIL_SEARCH
import mujmail.search.SearchWindows;
//#endif
//#ifdef MUJMAIL_DEBUG_CONSOLE
import mujmail.debug.DebugConsoleUI;
//#endif
//#ifdef MUJMAIL_SYNC
import mujmail.account.Sync;
//#endif
//#ifdef MUJMAIL_TOUCH_SCR
import mujmail.pointer.MujMailPointerEventListener;
import mujmail.pointer.MujMailPointerEventProducer;
//#endif

class MenuItem {
    String name;
    String value;
    int actionKey; //a keyboard shortcut
    Image img; //an icon

    public MenuItem(String name, String value, int actionKey, Image img) {
        this.name = name;
        this.value = value;
        this.actionKey = actionKey;
        this.img = img;
    }
}

class MenuTab {

    String name;
    int outlineColor;
    int fillColor;
    int actionKey;
    Image img;
    Vector item;
}

/**
 * Represents menu of this application.
 * To add items to the menu of the application, edit this class - see
 * constructor.
 * 
 */
public class Menu extends Canvas implements CommandListener {
    private static final boolean DEBUG = false; // Debuggin output for this class

    public static final byte ACTION = 0;
    public static final byte FOLDERS = 1;
    public static final byte SETTINGS = 2;
    public static final byte ACCOUNTS = 3;
    public static final byte UTILS = 4;
    public static final byte MAX_TABS = 5;
    public static final int BACKGROUND_COLOR = 0x00CCCCCC;
    public static final int OUTLINE_COLOR = 0x007766FF;
    public static final int FILL_COLOR = 0x00FFFFFF;
    public static final int GREY_OUTLINE_COLOR = 0x007777EE;
    public static final int GREY_FILL_COLOR = 0x00BBBBFF;
    public static final int FONT_COLOR = 0x00000000;
    public static final int SCROLLBAR_COLOR = 0x00FF0000;
    public static final int SCROLLBAR_BGCOLOR = 0x00CCCCCC;

    //public static final byte USR_BOX_FIRST_POSITION = 6; /// First empty position after standart mail boxes and separator where user mail box can take place
 /* TODO: refactor: replace numbers of order of items in menu with constants
 *  for example: tabs[tabContext].item.elementAt(0)
 *  replace with tabs[tabContext].item.elementAt(INBOX_ORDER)
*/
    public static final byte MENU_ACT_INBOX                = 0;
    public static final byte MENU_ACT_RETRIEVE_MAILS       = MENU_ACT_INBOX + 1;
    public static final byte MENU_ACT_WRITE_MAIL           = MENU_ACT_RETRIEVE_MAILS + 1;
    public static final byte MENU_ACT_SENDALL              = MENU_ACT_WRITE_MAIL + 1;
    //#ifdef MUJMAIL_SEARCH
    public static final byte MENU_ACT_SEARCH_MAILS         = MENU_ACT_SENDALL + 1;
    //#else
//#     public static final byte MENU_ACT_SEARCH_MAILS         = MENU_ACT_SENDALL;
    //#endif
    public static final byte MENU_ACT_PUSH                 = MENU_ACT_SEARCH_MAILS + 1;
    public static final byte MENU_ACT_DISCONNECT           = MENU_ACT_PUSH + 1;
    public static final byte MENU_ACT_SERVERS_INBOX_SYNC   = MENU_ACT_DISCONNECT + 1;
    public static final byte MENU_ACT_SIZE                 = MENU_ACT_SERVERS_INBOX_SYNC + 1; // Lease this as last element

    public static final byte MENU_FOLDERS_INBOX            = 0;
    public static final byte MENU_FOLDERS_OUTBOX           = MENU_FOLDERS_INBOX + 1;
    public static final byte MENU_FOLDERS_SENTBOX          = MENU_FOLDERS_OUTBOX + 1;
    public static final byte MENU_FOLDERS_DRAFT            = MENU_FOLDERS_SENTBOX + 1;
    public static final byte MENU_FOLDERS_TRASH            = MENU_FOLDERS_DRAFT + 1;
    public static final byte MENU_FOLDERS_SEPARATOR        = MENU_FOLDERS_TRASH + 1;
    public static final byte MENU_FOLDERS_USERBOX_FIRST    = MENU_FOLDERS_SEPARATOR + 1; /// First empty position after standart mail boxes and separator where user mail box can take place

    public static final byte MENU_SETTINGS_SMTP = 0;
    public static final byte MENU_SETTINGS_RETRIEVING = MENU_SETTINGS_SMTP + 1;
    public static final byte MENU_SETTINGS_STORING_MAILS = MENU_SETTINGS_RETRIEVING + 1;
    public static final byte MENU_SETTINGS_APPEARANCE = MENU_SETTINGS_STORING_MAILS + 1;
    public static final byte MENU_SETTINGS_OTHER = MENU_SETTINGS_APPEARANCE + 1;
    public static final byte MENU_SETTINGS_POLLING = MENU_SETTINGS_OTHER + 1;
    public static final byte MENU_SETTINGS_MUJMAIL_SERVER = MENU_SETTINGS_POLLING + 1;
    public static final byte MENU_SETTINGS_SIZE            = MENU_SETTINGS_MUJMAIL_SERVER + 1; // Lease this as last element
    
    public static final byte MENU_UTILS_ADRESSBOOK         = 0;
    //#ifdef MUJMAIL_SYNC
    public static final byte MENU_UTILS_BACKUP_SETTINGS    = MENU_UTILS_ADRESSBOOK + 1;
    public static final byte MENU_UTILS_RESTORE_SETTINGS   = MENU_UTILS_BACKUP_SETTINGS + 1;
    //#else
//#     public static final byte MENU_UTILS_RESTORE_SETTINGS   = MENU_UTILS_ADRESSBOOK;
    //#endif
    public static final byte MENU_UTILS_CLEAR_DB           = MENU_UTILS_RESTORE_SETTINGS + 1;
    public static final byte MENU_UTILS_ABOUT              = MENU_UTILS_CLEAR_DB + 1;
    public static final byte MENU_UTILS_TASK_MANAGER       = MENU_UTILS_ABOUT + 1;
    public static final byte MENU_UTILS_RUN_SAMPLE_TASK    = MENU_UTILS_TASK_MANAGER + 1;
    //#ifdef MUJMAIL_DEBUG_CONSOLE
    public static final byte MENU_UTILS_DEB_MENU           = MENU_UTILS_RUN_SAMPLE_TASK + 1; // Debug
    //#else
//#     public static final byte MENU_UTILS_DEB_MENU           = MENU_UTILS_RUN_SAMPLE_TASK; // no meniu debug entry, no adding space
    //#endif
    public static final byte MENU_UTILS_SIZE               = MENU_UTILS_DEB_MENU + 1;  // Lease this as last element
    
    Command exit, cmdNew, change, delete, setPrimary, retrieve, select, clear;

    //#ifdef MUJMAIL_USR_FOLDERS
    /// User Folders commands
    Command fldAdd, fldEdit, fldDel;
    //#endif
   

    MenuTab[] tabs = new MenuTab[MAX_TABS];
    Image imAction, imInbox, imFolders, imSettings, imAccounts,
            imWriteAct, imRetrieveAct, imSendallAct, imPollAct, imClearDB, imDisc,
            imPrimaryAcc, imActiveAcc, imInActiveAcc,
            imUtilities, imBook, imAbout, imSync, imTaskManager;
    
    //#ifdef MUJMAIL_SYNC
    Image imBackup, imRestore;
    //#endif
    //#ifdef MUJMAIL_SEARCH
    Image imSearch;
    //#endif
    //#ifdef MUJMAIL_DEBUG_CONSOLE
    Image imDebug;
    //#endif
    
    MujMail mujMail;

    // tab positioning
    byte firstTab = 0;
    byte currTab = 0;
    byte maxTabs;
    byte selectedTab = 0;
    // item positioning
    short currItem = 0;
    short firstItem = 0;
    short selectedItem = 0;
    // action key variables	
    boolean starPressed = false, poundPressed = false;
    int fontHeight;
    int clientHeight;
    // TickerTask
    Timer timer;
    short sindex, aindex;
    boolean sStarted, aStarted;
    String tickerText1, tickerText2;

    //#ifdef MUJMAIL_TOUCH_SCR
    private final MujMailPointerEventProducer pointerEventTransformer;
    //#endif
    
    private Font getFirstLineFont() {
    	if (Settings.fontSize == Settings.FONT_NORMAL)
    		return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    	else 
    		return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    }

    private Font getSecondLineFont() {
    	if (Settings.fontSize == Settings.FONT_NORMAL)
    		return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    	else 
    		return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    }

    private class TickerTask extends TimerTask {

        public void run() {
            repaint();
        }
    }
    ActionKeyTask actionKeyTask;

    private class ActionKeyTask extends TimerTask {

        public void run() {
            if (starPressed) {
                starPressed = false;
            }
            if (poundPressed) {
                poundPressed = false;
            }
            repaint();
        }
    }
    Timer refreshTimer;

    private class RefreshTask extends TimerTask {

        byte tab;

        public RefreshTask(byte tab) {
            this.tab = tab;
        }

        public void run() {
            if (getSelectedTab() == tab) {
                refresh(tab, timer == null);
            } //repaint() only if it's not tickering already
            else {
                cancelRefreshTask();
            }
        }
    }

    /**
     * Creates a new instance of Menu
     * @param mujMail the main application
     */
    public Menu(MujMail mujMail) {
        this.mujMail = mujMail;

        exit = new Command(Lang.get(Lang.BTN_EXIT), Command.EXIT, 0);
        cmdNew = new Command(Lang.get(Lang.BTN_AS_NEW), Command.ITEM, 1);
        change = new Command(Lang.get(Lang.BTN_EDIT), Command.ITEM, 2);
        delete = new Command(Lang.get(Lang.BTN_DELETE), Command.ITEM, 4);
        setPrimary = new Command(Lang.get(Lang.BTN_AS_SET_PRIMARY), Command.ITEM, 5);
        retrieve = new Command(Lang.get(Lang.BTN_RTV_NEW_MAILS), Command.ITEM, 0);
        //button select is  here just to make it more convenient on the real phone, 
        //where pressing fire would not always trigger CommandListener() (but key '5' does ironically).
        select = new Command(Lang.get(Lang.BTN_SELECT), Command.OK, 0);
        clear = new Command(Lang.get(Lang.BTN_CLEAR), Command.ITEM, 6);
                
        //#ifdef MUJMAIL_USR_FOLDERS
        fldAdd = new Command(Lang.get(Lang.BTN_USR_FLD_ADD), Command.ITEM, 1);
        fldDel = new Command(Lang.get(Lang.BTN_DELETE), Command.ITEM, 2);
        fldEdit = new Command(Lang.get(Lang.BTN_EDIT), Command.ITEM, 3);
        //#endif
        
        imAction = Functions.getIcon("menu_action.png");
        imInbox = Functions.getIcon("act_inbox.png");
        imFolders = Functions.getIcon("menu_folders.png");
        imSettings = Functions.getIcon("menu_settings.png");
        imAccounts = Functions.getIcon("menu_accounts.png");
        imUtilities = Functions.getIcon("menu_utilities.png");
        imWriteAct = Functions.getIcon("act_write.png");
        imRetrieveAct = Functions.getIcon("act_retrieve.png");
        imSendallAct = Functions.getIcon("act_sendall.png");
        imPollAct = Functions.getIcon("act_poll.png");
        imPrimaryAcc = Functions.getIcon("acc_primary.png");
        imActiveAcc = Functions.getIcon("acc_active.png");
        imInActiveAcc = Functions.getIcon("acc_inactive.png");
        imBook = Functions.getIcon("addressbook.png");
        imClearDB = Functions.getIcon("act_clear.png");
        imDisc = Functions.getIcon("act_disc.png");
        imSync = Functions.getIcon("act_sync.png");
        imTaskManager = Functions.getIcon("task_manager.png");
        //#ifdef MUJMAIL_SEARCH
        imSearch = Functions.getIcon("search.png");
        //#endif
        //#ifdef MUJMAIL_SYNC
        imBackup = Functions.getIcon("act_backup.png");
        imRestore = Functions.getIcon("act_restore.png");
        //#endif
        imAbout = Functions.getIcon("help.png");
        //#ifdef MUJMAIL_DEBUG_CONSOLE
        imDebug = Functions.getIcon("menu_debug.png");
        //#endif
        
        
        
        addCommand(clear);
        addCommand(exit);
        addCommand(cmdNew);
        addCommand(change);
        addCommand(delete);
        addCommand(setPrimary);
        addCommand(retrieve);
        setCommandListener(this);
        init();
        
        //#ifdef MUJMAIL_TOUCH_SCR
        pointerEventTransformer = new MujMailPointerEventProducer(new MenuPointerEventListener(), getWidth(), getHeight());
        //#endif
    }

    private void init() {
        // ACTION menu
        byte i = 0;
        tabs[i] = new MenuTab();
        tabs[i].name = Lang.get(Lang.AC_ACTIONS);
        tabs[i].actionKey = KEY_NUM1; //a keyboard shortcut		
        tabs[i].img = imAction; //an icon
        tabs[i].item = new Vector(MENU_ACT_SIZE);
        tabs[i].item.setSize(MENU_ACT_SIZE);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_INBOX), null, KEY_NUM1, imInbox), MENU_ACT_INBOX);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_RETRIEVE_MAILS), null, KEY_NUM2, imRetrieveAct), MENU_ACT_RETRIEVE_MAILS);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_WRITE_MAIL), null, KEY_NUM3, imWriteAct), MENU_ACT_WRITE_MAIL);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_SENDALL), null, KEY_NUM4, imSendallAct), MENU_ACT_SENDALL);
        //#ifdef MUJMAIL_SEARCH
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_SEARCH_MAILS), null, KEY_NUM5, imSearch), MENU_ACT_SEARCH_MAILS);
        //#endif
        //tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_POLLING), "(" + Lang.get(Lang.INACTIVE) + ")", KEY_NUM6, imPollAct), ... NOT POSITION DEFINED NOW);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_PUSH) + " (" + Lang.get(Lang.INACTIVE) + ")", null, KEY_NUM6, imPollAct), MENU_ACT_PUSH);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_DISCONNECT), null, KEY_NUM7, imDisc), MENU_ACT_DISCONNECT);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_SERVERS_INBOX_SYNC), null, KEY_NUM9, imSync), MENU_ACT_SERVERS_INBOX_SYNC);

        ++i;
        // FOLDERS menu
        tabs[i] = new MenuTab();
        tabs[i].name = Lang.get(Lang.TB_FOLDERS);
        tabs[i].actionKey = KEY_NUM3;
        tabs[i].img = imFolders;
        tabs[i].item = new Vector(MENU_FOLDERS_USERBOX_FIRST);
        tabs[i].item.setSize(MENU_FOLDERS_SEPARATOR); // Currently we set only 5 elements
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_INBOX)   + " (0/0)", null, KEY_NUM1, null), MENU_FOLDERS_INBOX);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_OUTBOX)  + " (0)",   null, KEY_NUM2, null), MENU_FOLDERS_OUTBOX);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_SENTBOX) + " (0)",   null, KEY_NUM3, null), MENU_FOLDERS_SENTBOX);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_DRAFT)   + " (0)",   null, KEY_NUM4, null), MENU_FOLDERS_DRAFT);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.TB_TRASH)   + " (0)",   null, KEY_NUM5, null), MENU_FOLDERS_TRASH);
        //Initialy no user folders are present. After loading folders from DB, refresh method is called
        
        ++i;
        // SETTINGS menu
        tabs[i] = new MenuTab();
        tabs[i].name = Lang.get(Lang.ST_SETTINGS);
        tabs[i].actionKey = KEY_NUM4;
        tabs[i].img = imSettings;
        tabs[i].item = new Vector(MENU_SETTINGS_SIZE);
        tabs[i].item.setSize(MENU_SETTINGS_SIZE);
        tabs[i].item.setElementAt(new MenuItem("Appearance", null, -1, null), MENU_SETTINGS_APPEARANCE);
        tabs[i].item.setElementAt(new MenuItem("MujMail server", null, -1, null), MENU_SETTINGS_MUJMAIL_SERVER);
        tabs[i].item.setElementAt(new MenuItem("Other", null, -1, null), MENU_SETTINGS_OTHER);
        tabs[i].item.setElementAt(new MenuItem("Polling", null, -1, null), MENU_SETTINGS_POLLING);
        tabs[i].item.setElementAt(new MenuItem("Retrieving mails", null, -1, null), MENU_SETTINGS_RETRIEVING);
        tabs[i].item.setElementAt(new MenuItem("SMTP", null, -1, null), MENU_SETTINGS_SMTP);
        tabs[i].item.setElementAt(new MenuItem("Storing mails", null, -1, null), MENU_SETTINGS_STORING_MAILS);

        ++i;
        // ACCOUNTS menu
        tabs[i] = new MenuTab();
        tabs[i].name = Lang.get(Lang.AC_ACTIONS);
        tabs[i].actionKey = KEY_NUM5;
        tabs[i].img = imAccounts;

        ++i;
        // UTILITIES menu
        tabs[i] = new MenuTab();
        tabs[i].name = Lang.get(Lang.UT_UTILS);
        tabs[i].actionKey = KEY_NUM6;
        tabs[i].img = imUtilities;
        tabs[i].item = new Vector(MENU_UTILS_SIZE);
        tabs[i].item.setSize(MENU_UTILS_SIZE);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AD_ADDRESSBOOK), null, KEY_NUM1, imBook), MENU_UTILS_ADRESSBOOK);
        //#ifdef MUJMAIL_SYNC
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_BACKUP_SETTINGS), null, KEY_NUM0, imBackup), MENU_UTILS_BACKUP_SETTINGS);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_RESTORE_SETTINGS), null, KEY_NUM0, imRestore), MENU_UTILS_RESTORE_SETTINGS);        
        //#endif
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.AC_CLEAR_DB), null, KEY_NUM8, imClearDB), MENU_UTILS_CLEAR_DB);
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.ABT_ABOUT), null, KEY_NUM2, imAbout), MENU_UTILS_ABOUT);
        //#ifdef MUJMAIL_DEBUG_CONSOLE
        tabs[i].item.setElementAt(new MenuItem(Lang.get(Lang.DEB_MENU), null, KEY_NUM3, imDebug), MENU_UTILS_DEB_MENU);
        //#endif
        tabs[i].item.setElementAt(new MenuItem("Task manager", null, KEY_NUM4, imTaskManager), MENU_UTILS_TASK_MANAGER);
        tabs[i].item.setElementAt(new MenuItem("Start sample task", null, KEY_NUM5, null), MENU_UTILS_RUN_SAMPLE_TASK);
        setSelectedTab((byte) 0);

    }

    public void refreshAll(boolean displayMenu) {
        refresh(ACTION, false);
        refresh(FOLDERS, false);
        refresh(SETTINGS, false);
        refresh(ACCOUNTS, false);
        if (displayMenu) {
            repaint();
            mujMail.getDisplay().setCurrent(this);
        }
    }

    public void refresh(byte tabContext, boolean displayMenu) {
        String prefix = " (";
        byte refreshAgain = -1;
        try {
        if (tabContext == ACTION) {
            //if it's busy display the * signaling that numbers are just estimated			
            if (mujMail.getInBox().isBusy() && getSelectedTab() == ACTION) {
                prefix = " *(";
                refreshAgain = ACTION;
            }

            if (DEBUG) { System.out.println("DEBUG Menu.refresh() - inbox storage: " + mujMail.getInBox().getStorage()); }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_ACT_INBOX)).name = Lang.get(Lang.TB_INBOX) +
                    prefix + mujMail.getInBox().getUnreadMailsCount() + "/" +
                    mujMail.getInBox().getMessageCount() + ")";

             if (DEBUG) { System.out.println("DEBUG Menu.refresh() - outBox storage: " + mujMail.outBox.storage); }
            String saCounter = "";
            if (mujMail.outBox.isBusy() && getSelectedTab() == ACTION) {
                refreshAgain = ACTION;
                saCounter = " *(" + mujMail.outBox.getMessageCount() + ")";

            } else if (mujMail.outBox.getMessageCount() != 0) {
                saCounter = " (" + mujMail.outBox.getMessageCount() + ")";
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_ACT_SENDALL)).name = Lang.get(Lang.AC_SENDALL) + saCounter;

            // Updating state of pushing
            if (DEBUG) { System.out.println("DEBUG Menu.refresh() - mujMail: " + mujMail); }
            if ( mujMail.getInBox().isPushActive() ) {
                ((MenuItem) tabs[tabContext].item.elementAt(MENU_ACT_PUSH)).name = Lang.get(Lang.AC_PUSH) + " (" + Lang.get(Lang.ACTIVE) + ")";
            } else {
                ((MenuItem) tabs[tabContext].item.elementAt(MENU_ACT_PUSH)).name = Lang.get(Lang.AC_PUSH) + " (" + Lang.get(Lang.INACTIVE) + ")";
            }


        } else 
        if (tabContext == FOLDERS) {
            // Inbox - updating state
            if (mujMail.getInBox().isBusy() && getSelectedTab() == FOLDERS) {
                prefix = " *(";
                refreshAgain = ACTION;
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_INBOX)).name = Lang.get(Lang.TB_INBOX) + " " +
                    prefix + mujMail.getInBox().getUnreadMailsCount() + "/" +
                    mujMail.getInBox().getMessageCount() + ")";

            // Outbox - updating state
            if (mujMail.outBox.isBusy() && getSelectedTab() == FOLDERS) {
                prefix = " *(";
                refreshAgain = FOLDERS;
            } else {
                prefix = "(";
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_OUTBOX)).name = Lang.get(Lang.TB_OUTBOX)  + " " + prefix + mujMail.outBox.getMessageCount() + ")";

            // SentBox - updating state
            if (mujMail.getSentBox().isBusy() && getSelectedTab() == FOLDERS) {
                prefix = " *(";
                refreshAgain = FOLDERS;
            } else {
                prefix = "(";
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_SENTBOX)).name = Lang.get(Lang.TB_SENTBOX) + " " + prefix + mujMail.getSentBox().getMessageCount() + ")";

            // Drafts - updating state
            if (mujMail.draft.isBusy() && getSelectedTab() == FOLDERS) {
                prefix = " *(";
                refreshAgain = FOLDERS;
            } else {
                prefix = "(";
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_DRAFT)).name = Lang.get(Lang.TB_DRAFT) + " " + prefix + mujMail.draft.getMessageCount() + ")";

            // Trash - updating state
            if (mujMail.getTrash().isBusy() && getSelectedTab() == FOLDERS) {
                prefix = " *(";
                refreshAgain = FOLDERS;
            } else {
                prefix = "(";
            }
            ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_TRASH)).name = Lang.get(Lang.TB_TRASH) + " " + prefix + mujMail.getTrash().getMessageCount() + ")";
            
            //#ifdef MUJMAIL_USR_FOLDERS
            // User folders
            // Note user folder use only name, no other parameters from MenuItem (picture, shortcut key or value)
            Vector userMailBoxes = mujMail.getUserMailBoxes().getBoxList();
            int userMailBoxesSize = userMailBoxes.size();
            synchronized(this) {
                /* We update size of tabs folders array /
                    if we repaint in time before entrie setting is done
                    we can fall down on null dereference --> synchronisation is needed
                */ 
                // Prepare item array to have correct length
                if (userMailBoxesSize == 0) {
                    // 1) Last user mailbox removed - no separator
                    tabs[tabContext].item.setSize(MENU_FOLDERS_SEPARATOR);
                } else {
                    tabs[tabContext].item.setSize(MENU_FOLDERS_USERBOX_FIRST + userMailBoxesSize);
                    // Set separator
                    if (tabs[tabContext].item.elementAt(MENU_FOLDERS_SEPARATOR) == null) {
                        tabs[tabContext].item.setElementAt(new MenuItem(null, null, -1, null), MENU_FOLDERS_SEPARATOR);
                    }
                    ((MenuItem) tabs[tabContext].item.elementAt(MENU_FOLDERS_SEPARATOR)).name = "-----------------";
                }

                // Check if position is not out of range (otherwise set to last position)
                if ((tabContext == currTab) && (currItem >= tabs[tabContext].item.size()-1)) {
                    setSelectedItem((short)(tabs[tabContext].item.size()-1));
                }

                // Setting user folder names
                for( int i = 0; i < userMailBoxesSize; i++) {
                    InBox userBox = (InBox)userMailBoxes.elementAt(i);
                    if (userBox.isBusy() && getSelectedTab() == FOLDERS) {
                        prefix = " *(";
                        refreshAgain = FOLDERS;
                    } else {
                        prefix = "(";
                    }
                    if (tabs[tabContext].item.elementAt(MENU_FOLDERS_USERBOX_FIRST + i) == null) {
                        tabs[tabContext].item.setElementAt(new MenuItem(null, null, -1, null), MENU_FOLDERS_USERBOX_FIRST + i);
                    }
                    ((MenuItem)tabs[tabContext].item.elementAt(MENU_FOLDERS_USERBOX_FIRST + i)).name = userBox.getName() + " " + prefix + userBox.getUnreadMailsCount() + "/" + userBox.getMessageCount() + ")";
                }
            }
            //#endif       
        } else
        if (tabContext == ACCOUNTS) {
            // Display only primary mail accounts
            Hashtable /*<String, MailAccountPrimar>*/ accounts = mujMail.getMailAccounts();
            tabs[tabContext].item = new Vector(accounts.size());

            short i = 1;
            for (Enumeration e = accounts.elements(); e.hasMoreElements();) {
                MailAccount account = (MailAccount) e.nextElement();
                String info = account.isActive() ? Lang.get(Lang.ACTIVE) : Lang.get(Lang.INACTIVE);
                Image img = imInActiveAcc;
                //an account must be active in order to be primary, 
                //so if it has icon imPrimary, it means its active and also primary
                if (Settings.primaryEmail.equals(account.getEmail())) {
                    info += " | " + Lang.get(Lang.AS_PRIMARY);
                    img = imPrimaryAcc;
                } else if (account.isActive()) //its active only
                {
                    img = imActiveAcc;
                }
                tabs[tabContext].item.addElement(new MenuItem(account.getEmail(), info, i, img));
            }
        }

        if (displayMenu) {
            if (refreshTimer == null) { //this is not a refresh call
                setTabContext(getSelectedTab());
            }
            repaint();
            mujMail.getDisplay().setCurrent(this);
        }

        //if refresh is needed and it was not initiated yet
        if (refreshAgain != -1 && refreshTimer == null) {
            refreshTimer = new Timer();
            refreshTimer.schedule(new RefreshTask(refreshAgain), 0, 100);
        } //refresh is not needed and yet refreshTimer is still running
        else if ((refreshAgain == -1 || mujMail.getDisplay().getCurrent() != this) && 
        		refreshTimer != null) {	//NOTE: do not use Displayable.isShown method as it does not
        								//      work for Nokias.
            cancelRefreshTask();
        }
        } catch (Throwable e ) {
            System.err.println("ERROR Menu - Unexpected exception");
            e.printStackTrace();
        }
    }

    protected void hideNotify() {
        cancelTickerTask();
        cancelRefreshTask();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exit) {
            TasksManager.conditionallyExit();
//            if (TasksManager.isSomeTaskRunning()) {
//                this.mujMail.alert.setAlert(null, this, Lang.get(Lang.ALRT_SYS_SHUTDOWN_CONFIRM), MyAlert.EXIT_BUSY_CONFIRM, AlertType.CONFIRMATION);
//            } else {
//                this.mujMail.destroyApp(false);
//            }
        } else {
            switch (getSelectedTab()) {
                case Menu.ACTION:
                    switch (getSelectedIndex()) {
                        case MENU_ACT_INBOX:
                            mujMail.getInBox().showBox();
                            break;
                        case MENU_ACT_RETRIEVE_MAILS:
                            mujMail.getDisplay().setCurrent( mujMail.getInBox());
                            mujMail.getInBox().retrieve();
                            break;
                        case MENU_ACT_WRITE_MAIL:
                            mujMail.sendMail.writeMail(this);
                            break;
                        case MENU_ACT_SENDALL:
                            mujMail.getDisplay().setCurrent(mujMail.outBox);
                            mujMail.outBox.sendAll();
                            break;
                        //#ifdef MUJMAIL_SEARCH
                        case MENU_ACT_SEARCH_MAILS:
                            SearchWindows searchWindows = new SearchWindows(mujMail);
                            searchWindows.displaySearchWindow();
                            break;
                        //#endif
                        case MENU_ACT_PUSH:
                            mujMail.getInBox().poll();
                            //Testing IMAP Push
                            mujMail.getInBox().push();
                            refresh(Menu.ACTION, true);
                            break;
                        case MENU_ACT_DISCONNECT:
                            mujMail.discServers(true);
                            break;
                        case MENU_ACT_SERVERS_INBOX_SYNC:
                        	//No task (new thread) is needed here since
                        	//serversSync() invokes GET_NEW_MAILS task
                            mujMail.getInBox().serversSync(null);
                            refresh(Menu.ACTION, true);
                            break;
                    }
                    break;
                case Menu.FOLDERS:
                    //#ifdef MUJMAIL_USR_FOLDERS
                    if (c == fldAdd) {
                        mujMail.getUserMailBoxes().createPersistentBox();
                    } else if (c == fldDel) {
                        mujMail.getUserMailBoxes().removeUserMailBox(getSelectedIndex() - MENU_FOLDERS_USERBOX_FIRST);
                        repaint();
                    } else if (c == fldEdit) {
                        mujMail.getUserMailBoxes().editUserMailBoxSettings(getSelectedIndex() - MENU_FOLDERS_USERBOX_FIRST);
                        //repaint();
                    } else { // c == select or enter (null)
                    //#endif
                        switch (getSelectedIndex()) {
                            // receive mails
                            case MENU_FOLDERS_INBOX:
                                mujMail.getInBox().showBox();
                                break;
                            case MENU_FOLDERS_OUTBOX:
                                mujMail.getOutBox().showBox();
                                break;
                            case MENU_FOLDERS_SENTBOX:
                                mujMail.getSentBox().showBox();
                                break;
                            case MENU_FOLDERS_DRAFT:
                                mujMail.getDraft().showBox();
                                break;
                            case MENU_FOLDERS_TRASH:
                                mujMail.getTrash().showBox();
                                break;
                            //#ifdef MUJMAIL_USR_FOLDERS
                            default:
                               //case 5: // No action -- mailbox delimiter
                               int pos = getSelectedIndex() - MENU_FOLDERS_USERBOX_FIRST;
                               Vector/*<inbox>*/ usrMailBoxes = mujMail.getUserMailBoxes().getBoxList();
                               if ( pos >= 0 && pos < usrMailBoxes.size()) {
                                   ( (InBox)usrMailBoxes.elementAt(pos)).showBox();
                               }
                            //#endif
                        }
                    //#ifdef MUJMAIL_USR_FOLDERS                   
                    }
                    //#endif
                    break;
                case Menu.SETTINGS:
                    switch (getSelectedIndex()) {
                        case MENU_SETTINGS_APPEARANCE:
                            mujMail.getSettings().showAppearanceSettingsForm();
                            break;
                        case MENU_SETTINGS_MUJMAIL_SERVER:
                            mujMail.getSettings().showMujMailServerSettingsForm();
                            break;
                        case MENU_SETTINGS_OTHER:
                            mujMail.getSettings().showOtherSettingsForm();
                            break;
                        case MENU_SETTINGS_POLLING:
                            mujMail.getSettings().showPollingSettingsForm();
                            break;
                        case MENU_SETTINGS_RETRIEVING:
                            mujMail.getSettings().showRetrievingSettingsForm();
                            break;
                        case MENU_SETTINGS_SMTP:
                            mujMail.getSettings().showSMTPSettingsForm();
                            break;
                        case MENU_SETTINGS_STORING_MAILS:
                            mujMail.getSettings().showStoringMailsSettingsForm();
                            break;

                    }
                    
                    break;
                case Menu.ACCOUNTS:
                    if (c == cmdNew) {
                        mujMail.getAccountSettings().showAccount(null);
                    } else if (c == change) {
                        mujMail.getAccountSettings().showAccount(getSelectedAccount());
                    } else if (c == delete) {
                        mujMail.getAccountSettings().deleteAccount(getSelectedAccount(), false);
                    } else if (c == setPrimary) {
                        String accountID = getSelectedAccount();
                        if (accountID != null && ((MailAccount) mujMail.getMailAccounts().get(accountID)).isActive()) {
                            Settings.primaryEmail = accountID;
                            mujMail.getSettings().saveSettings(true);
                        } else {
                            mujMail.alert.setAlert(null, this, Lang.get(Lang.ALRT_AS_NONEXIST), MyAlert.DEFAULT, AlertType.INFO);
                        }
                    } else if (c == retrieve) {
                        mujMail.getDisplay().setCurrent(mujMail.getInBox());
                        mujMail.getInBox().retrieveOne(getSelectedAccount());
                    } else if (c == clear) {
                        mujMail.getAccountSettings().deleteAll(false);
                    } else {
                        mujMail.getAccountSettings().showAccount(getSelectedAccount());
                    }
                    break;
                case Menu.UTILS:
                    switch (getSelectedIndex()) {
                        case MENU_UTILS_ADRESSBOOK:
                            mujMail.getDisplay().setCurrent(mujMail.getAddressBook());
                            break;
                        //#ifdef MUJMAIL_SYNC
                        case MENU_UTILS_BACKUP_SETTINGS:
                            mujMail.getSync().setAction(Sync.BACKUP);
                            //#ifdef MUJMAIL_FS
                            mujMail.getSync().setSmDlg(mujMail.getSync().new SyncModeDialog(Lang.get(Lang.AC_BACKUP_SETTINGS)));
                            mujMail.getDisplay().setCurrent(mujMail.getSync().getSmDlg());
                            //#else
//#                             mujMail.getSync().commandAction(null, null);
                            //#endif
                            break;
                        case MENU_UTILS_RESTORE_SETTINGS:
                            mujMail.getSync().setAction(Sync.RESTORE);
                            //#ifdef MUJMAIL_FS
                            mujMail.getSync().setSmDlg(mujMail.getSync().new SyncModeDialog(Lang.get(Lang.AC_RESTORE_SETTINGS)));
                            mujMail.getDisplay().setCurrent( mujMail.getSync().getSmDlg());
                            //#else
//#                             mujMail.getSync().commandAction(null, null);
                            //#endif
                            break;
                        //#endif
                        case MENU_UTILS_CLEAR_DB:
                            mujMail.clearDBSelect.refresh();
                            mujMail.getDisplay().setCurrent( mujMail.clearDBSelect);
                            break;
                        case MENU_UTILS_ABOUT:
                            mujMail.getDisplay().setCurrent(mujMail.about);
                            break;
                        //#ifdef MUJMAIL_DEBUG_CONSOLE
                        case MENU_UTILS_DEB_MENU:
                            //BackgroundTaskTest threadTest = new BackgroundTaskTest();
                            //threadTest.testSleepingThread();
                            //threadTest.testStoppableThread();
                            DebugConsoleUI consoleUI = new DebugConsoleUI();
                            consoleUI.showScreen();
                            //mujMail.getDisplay().setCurrent(debug);
                            break;
                        //#endif
                        case MENU_UTILS_TASK_MANAGER:
                            TasksManagerUI tasksManager = new TasksManagerUI();
                            tasksManager.showScreen();
                            break;
                        case MENU_UTILS_RUN_SAMPLE_TASK:
                            BackgroundTask sampleTask = new SampleTestTask();
                            sampleTask.start();

                            break;
                    }
                    break;
            } // switch end
        } // else in c == exit condition 
    }
    /**
     * This method draws both menu tabs and lists items on Canvas . If a number of all tabs is bigger than
     * it is able to display, then it draws a vertical scrollbar at the bottom of the display and two black
     * arrows at the top part. Equally, if there is a need for drawing a horizontal scrollbar it is done at
     * the left side of the display.
     * @param g - a graphical object.
     */
    public synchronized void paint(Graphics g) {
        int menuPadding = 1;
        int width = getWidth(), height = getHeight();
        int arrowWidth;
        int tabLeft;
        int tabHeight = (width < 200) ? 20 : 30;
        int tabWidth = tabHeight + 2;
        int imgPadding = 4;
        clientHeight = height - tabHeight - 2 * menuPadding;
        int clientWidth = width - 2 * menuPadding;
        fontHeight = g.getFont().getHeight();

        // clear display
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);

        // draw arrows
        if ((maxTabs = (byte) ((width - 2 * menuPadding - 1) / (tabWidth + 1))) < MAX_TABS) {
            arrowWidth = 5;
            drawTriangle((short) menuPadding, (short) (menuPadding + (tabHeight - 2 * arrowWidth) / 2), (short) arrowWidth, true, g);
            drawTriangle((short) (width - menuPadding - arrowWidth), (short) (menuPadding + (tabHeight - 2 * arrowWidth) / 2), (short) arrowWidth, false, g);
            tabLeft = arrowWidth + menuPadding + 2;
            maxTabs = (byte) ((width - 2 * menuPadding - 2 * tabLeft) / (tabWidth + 1));
        } else {
            arrowWidth = 0;
            tabLeft = menuPadding;
            maxTabs = MAX_TABS;
        }

        // draw tabs
        g.setColor(OUTLINE_COLOR);
        g.drawLine(0, menuPadding + tabHeight + 1, width, menuPadding + tabHeight + 1);
        for (int i = 0; i < maxTabs; i++) {
            byte listID = (byte) ((firstTab + i) % MAX_TABS);
            // draw inner part of the tab
            if (i == selectedTab) {
                g.setColor(FILL_COLOR);
                g.fillRoundRect(tabLeft + 1, menuPadding + 1, tabWidth - 1, tabHeight + 2, imgPadding, imgPadding);
                g.setColor(OUTLINE_COLOR);
            } else {
                g.setColor(GREY_FILL_COLOR);
                g.fillRoundRect(tabLeft + 1, menuPadding + 1, tabWidth - 1, tabHeight + 2, imgPadding, imgPadding);
                g.setColor(OUTLINE_COLOR);
                g.drawLine(tabLeft, menuPadding + tabHeight + 1, tabLeft + tabWidth, menuPadding + tabHeight + 1);
                g.setColor(GREY_OUTLINE_COLOR);
            }

            // draw outlines of the tab
            g.drawLine(tabLeft, menuPadding + tabHeight, tabLeft, menuPadding + imgPadding);
            g.drawArc(tabLeft, menuPadding, 2 * imgPadding, 2 * imgPadding, 180, -90);
            g.drawLine(tabLeft + imgPadding, menuPadding, tabLeft + tabWidth - imgPadding, menuPadding);
            g.drawArc(tabLeft + tabWidth - 2 * imgPadding, menuPadding, 2 * imgPadding, 2 * imgPadding, 0, 90);
            g.drawLine(tabLeft + tabWidth, menuPadding + imgPadding, tabLeft + tabWidth, menuPadding + tabHeight);

            // draw content of the tab
            if (tabs[listID].img != null) {
                g.drawImage(tabs[listID].img, tabLeft + (tabWidth - tabs[listID].img.getWidth()) / 2, menuPadding + (tabHeight - tabs[listID].img.getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
            }
            if (starPressed) {
                g.setColor(FONT_COLOR);
                g.setFont(getFirstLineFont());
                g.drawString(String.valueOf(listID + 1), tabLeft + ((tabWidth - g.getFont().charWidth((char) (listID + '0' + 1))) / 2), menuPadding + tabHeight, Graphics.BOTTOM | Graphics.LEFT);
            }
            // move position to another tab
            tabLeft += tabWidth + 1;
        } /* for */

        // clear tab body
        g.setColor(FILL_COLOR);
        g.fillRect(0, menuPadding + tabHeight + 2, width, height - menuPadding - tabHeight - 2);

        // if it is necessary to display a vertical scrollbar
        g.translate(0, menuPadding + tabHeight + menuPadding + 2);
        short scrollStep, minStep/*, currScroll*/;

        if (maxTabs < MAX_TABS) {
            clientHeight -= 5;
            minStep = 10;
            scrollStep = (short) ((clientWidth) / (MAX_TABS - maxTabs + 1));

            g.setColor(SCROLLBAR_BGCOLOR);
            g.fillRect(menuPadding, clientHeight, clientWidth, 2);
            if (scrollStep < minStep) {
                scrollStep = (short) ((clientWidth - minStep) / (MAX_TABS - maxTabs));
                g.setColor(SCROLLBAR_COLOR);
                g.fillRect(menuPadding + firstTab * scrollStep, clientHeight, minStep, 2);
            } else {
                g.setColor(SCROLLBAR_COLOR);
                g.fillRect(menuPadding + firstTab * scrollStep, clientHeight, scrollStep, 2);
            }
        }

        // if it is necessary to display a horizontal scrollbar
        short itemCount = (short) tabs[currTab].item.size();
        short maxDsplItems = getMaxItems();
        if (maxDsplItems < itemCount) {
            scrollStep = (short) ((clientHeight) / (itemCount - maxDsplItems + 1));
            minStep = 10;
            clientWidth -= 3;

            g.setColor(SCROLLBAR_BGCOLOR);
            g.fillRect(2 * menuPadding + clientWidth, 0, 2, clientHeight);
            if (scrollStep < minStep) {
                scrollStep = (short) ((clientHeight - minStep) / (itemCount - maxDsplItems));
                g.setColor(SCROLLBAR_COLOR);
                g.fillRect(2 * menuPadding + clientWidth, firstItem * scrollStep, 2, minStep);
            } else {
                g.setColor(SCROLLBAR_COLOR);
                g.fillRect(2 * menuPadding + clientWidth, firstItem * scrollStep, 2, scrollStep);
            }
        }

        // draw menu items
        int x = 0;
        int y = 0;
        for (short j = 0; j < maxDsplItems; j++) {
            short itemID = (short) ((firstItem + j) % itemCount);
            MenuItem mi = (MenuItem) (tabs[currTab].item.elementAt(itemID));
            // if item is not selected
            if (itemID != currItem) {
                g.setColor(FONT_COLOR);
            } else {
                g.setColor(OUTLINE_COLOR);
                // try to draw a one-line rectangle if value of the item is null
                if (mi.value == null) {
                    g.fillRoundRect(menuPadding, y, clientWidth, fontHeight, 4, 4);
                } // else draw a two-line rectangle
                else {
                    g.fillRoundRect(menuPadding, y, clientWidth, 2 * fontHeight, 4, 4);
                }

                g.setColor(FILL_COLOR);
            }

            // item now fits to the display
            if (mi.img != null) {
                x += menuPadding + 1;
                g.drawImage(mi.img, x, y + (fontHeight - mi.img.getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
                x += mi.img.getWidth() + 2;
            }

            // draw name and value
            if (mi.name != null) {
                g.setFont(getFirstLineFont());
                byte offset = (byte) (x + 3);
                // scrollbar
                if (maxDsplItems < itemCount) {
                    offset += 3;
                }
                // actionkey numbers
                if (poundPressed) {
                    offset += g.getFont().charWidth('m');
                }
                if (g.getFont().stringWidth(mi.name) < clientWidth - offset) {
                    g.drawString(mi.name, x + 2, y + fontHeight, Graphics.BOTTOM | Graphics.LEFT);
                } // name does not fit to the screen 
                else {

                    if (itemID == currItem) {
                        //init TickerTask						
                        if (timer == null) {
                            timer = new Timer();
                            timer.schedule(new TickerTask(), 250, 100);
                        }
                        if (!sStarted) {
                            sStarted = true;
                            tickerText1 = mi.name;
                            //lets fill the tickerText1 with half of display of spaces
                            short space = (short) (width / (2 * g.getFont().charWidth(' ')));
                            for (short i = 0; i < space; i++) {
                                tickerText1 += ' ';
                            }
                            String nameSubstr = Functions.cutString(mi.name, sindex, clientWidth - offset - g.getFont().stringWidth(".."), g).trim() + "..";
                            g.drawString(nameSubstr, x + 2, y + fontHeight, Graphics.BOTTOM | Graphics.LEFT);
                        } else {
                            Functions.Ticker(tickerText1, sindex, x + 2, y + fontHeight, clientWidth - 3, g, Graphics.BOTTOM | Graphics.LEFT);
                            sindex = (short) ((sindex + 1) % tickerText1.length());
                        }
                    } else {// item is not selected so don't move												
                        String nameSubstr = Functions.cutString(mi.name, 0, clientWidth - offset - g.getFont().stringWidth(".."), g).trim() + "..";
                        g.drawString(nameSubstr, x + 2, y + fontHeight, Graphics.BOTTOM | Graphics.LEFT);
                    }
                }

                // draw actionkey number if a pound key is pressed
                if (mi.actionKey != -1 && poundPressed) {
                	if (Settings.fontSize == Settings.FONT_NORMAL) {
                		g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                	}
                	else {
                		g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
                	}
                	g.drawString(String.valueOf(itemID + 1), clientWidth, y + (fontHeight + g.getFont().getHeight()) / 2, Graphics.BOTTOM | Graphics.RIGHT);
                }

                // draw value on the second line
                y += fontHeight;
                if (poundPressed) {
                    offset -= g.getFont().charWidth('m');
                }
                if (mi.value != null) {
                    g.setFont(getSecondLineFont());
                    if (g.getFont().stringWidth(mi.value) < clientWidth - offset) {
                        g.drawString(mi.value, 2, y + fontHeight - 2, Graphics.BOTTOM | Graphics.LEFT);
                    } // value does not fit to the screen, so move
                    else {

                        if (itemID == currItem) {
                            // init TickerTask
                            if (timer == null) {
                                timer = new Timer();
                                timer.schedule(new TickerTask(), 250, 100);
                            }
                            if (!aStarted) {
                                aStarted = true;
                                tickerText2 = mi.value;
                                //lets fill the tickerText1 with half of display of spaces
                                short space = (short) (width / (2 * g.getFont().charWidth(' ')));
                                for (short i = 0; i < space; i++) {
                                    tickerText2 += ' ';
                                }
                                String valueSubstr = Functions.cutString(mi.value, aindex, clientWidth - offset - g.getFont().stringWidth(".."), g).trim() + "..";
                                g.drawString(valueSubstr, 2, y + fontHeight - 2, Graphics.BOTTOM | Graphics.LEFT);
                            } else { //doing the effect, moving to the left
                                Functions.Ticker(tickerText2, aindex, 2, y + fontHeight - 2, clientWidth - 3, g, Graphics.BOTTOM | Graphics.LEFT);
                                aindex = (short) ((aindex + 1) % tickerText2.length());
                            }

                        } else {
                            String valueSubstr = Functions.cutString(mi.value, 0, clientWidth - offset - g.getFont().stringWidth(".."), g).trim() + "..";
                            g.drawString(valueSubstr, 2, y + fontHeight, Graphics.BOTTOM | Graphics.LEFT);
                        }
                    }
                    y += fontHeight;
                }
            }
            x = 0;
        } /* while */


        // transform axis back to [0, 0] position
        g.translate(0, -menuPadding - tabHeight - menuPadding - 2);
    }

    //#ifdef MUJMAIL_TOUCH_SCR
    protected void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        
        if (DEBUG) { System.out.println("Pointer pressed: " + x + "; " + y); }
        pointerEventTransformer.pointerPressed(x, y);
    }
    //#endif
    
    

    /**
     * Reacts on keys pressed.
     * @param keyCode - a code of key pressed.
     */
    protected synchronized void keyPressed(int keyCode) {

        // process a NUM pad keys that are mostly used as shortcuts
//        boolean has = hasRepeatEvents();
        switch (keyCode) {
            case KEY_STAR:
                if (actionKeyTask != null) {
                    actionKeyTask.cancel();
                    actionKeyTask = null;
                }
                if (starPressed) {
                    starPressed = false;
                } else {
                    starPressed = true;
                    if (timer == null) {
                        timer = new Timer();
                    }

                    actionKeyTask = new ActionKeyTask();
                    timer.schedule(actionKeyTask, 3000);
                }

                poundPressed = false;
                repaint();
                break;

            case KEY_POUND:
                if (actionKeyTask != null) {
                    actionKeyTask.cancel();
                    actionKeyTask = null;
                }
                if (poundPressed) {
                    poundPressed = false;
                } else {
                    poundPressed = true;
                    if (timer == null) {
                        timer = new Timer();
                    }
                    actionKeyTask = new ActionKeyTask();
                    timer.schedule(actionKeyTask, 3000);
                }

                starPressed = false;
                repaint();
                break;

            case KEY_NUM1:
            case KEY_NUM2:
            case KEY_NUM3:
            case KEY_NUM4:
            case KEY_NUM5:
            case KEY_NUM6:
            case KEY_NUM7:
            case KEY_NUM8:
            case KEY_NUM9:
                // interval between pressing the STAR button and the following button is limited to 3 second
                if (starPressed) {
                    if (keyCode - 49 < MAX_TABS) {
                        starPressed = false;
                        poundPressed = false;
                        setSelectedTab((byte) (keyCode - 49));
                    }
                    return;
                }

                if (poundPressed) {
                    if (keyCode - 49 < tabs[currTab].item.size()) {
                        starPressed = false;
                        poundPressed = false;
                        setSelectedItem((short) (keyCode - 49));
                        commandAction(null, this);
                    }
                    return;
                }
                starPressed = false;
                poundPressed = false;
                repaint();

                break;
        }

        int gameAction = getGameAction(keyCode);
        if (gameAction == UP || gameAction == DOWN || gameAction == RIGHT || gameAction == LEFT) {
            starPressed = false;
            poundPressed = false;
        }

        switch (getGameAction(keyCode)) {
            case UP:
                upPressedAction();
                break;

            case DOWN:
                downPressedAction();
                break;

            case RIGHT:
                rightPressedAction();
                break;

            case LEFT:
                leftPressedAction();
                break;

            case FIRE:
                firePressedAction();
                break;
        }
    }
    
    /**
     * The action when left button was pressed.
     */
    private void leftPressedAction() {
        setSelectedTab((byte) (currTab - 1));
    }
    
    /**
     * The action when left button was pressed.
     */
    private void rightPressedAction() {
        setSelectedTab((byte) (currTab + 1));
    }
    
    /**
     * The action when up button was pressed.
     */
    private void upPressedAction() {
        setSelectedItem((short) (currItem - 1));
    }
    
    /**
     * The action when down button was pressed.
     */
    private void downPressedAction() {
        setSelectedItem((short) (currItem + 1));
    }
    
    /**
     * The action when fire button was pressed.
     */
    private void firePressedAction() {
        commandAction(null, this);
    }

    /**
     * Reacts on keys being held. It occurs only while pressing arrows in the menu.
     * @param keyCode - a code of key that is held.
     */
    protected void keyRepeated(int keyCode) {
        if (hasRepeatEvents()) {
            int gameAction = getGameAction(keyCode);
            if (gameAction == UP || gameAction == DOWN || gameAction == RIGHT || gameAction == LEFT) {
                starPressed = false;
                poundPressed = false;
                // stop TickerTask
                cancelTickerTask();
            }

            switch (gameAction) {
                case UP:
                    upPressedAction();
                    break;

                case DOWN:
                    downPressedAction();
                    break;

                case RIGHT:
                    rightPressedAction();
                    break;

                case LEFT:
                    leftPressedAction();
                    break;
            }
        }
    }

    private short getMaxItems() {
        short j = 0;
        short itemCount = (short) tabs[currTab].item.size();
        int y = 0;
        while (y + fontHeight < clientHeight && j < itemCount) {
            MenuItem mi = (MenuItem) (tabs[currTab].item.elementAt((firstItem + j) % itemCount));
            // if there is only one line
            if (mi.value == null) {
                y += fontHeight;
            } else if (y + 2 * fontHeight <= clientHeight) {
                y += 2 * fontHeight;
            } else {
                // to set maxItems correctly afterwards
                //++j;
                break;
            }
            ++j;
        }

        return j;
    }

    /**
     * Recounts three variables that describe current position of the item cursor.
     * These variables are:
     * <ul>
     *    <li><B>firstItem</B> - it is in range of <0 .. itemCount - maxDsplItems -1> and determines a
     * <CODE>Vector</CODE> ID of the first item on the current display.</li>
     *    <li><B>currItem</B> - it is in range of <0 .. itemCount - 1> and holds an ID of the <CODE>MenuItem Vector</CODE> of
     *    the item that is selected.</li>
     *    <li><B>selectedItem</B> - it is in range of <0 .. maxDsplItem - 1> and describes just relative
     *    position of the cursor from the firstItem.</li>
     * <ul>
     * @param newItem - ID of the new position.
     */
    public void setSelectedItem(short newItem) {
        cancelTickerTask();
        // count new positions		
        short itemCount = (short) tabs[currTab].item.size();
        if (itemCount == 0) {
            return;
        }

        short offset = (short) (newItem - currItem);
        short newCurrItem = (short) (currItem + offset);
        currItem = (short) ((newCurrItem + itemCount) % itemCount);
        short maxItems = getMaxItems();

        // cursor is at the begining
        if (newCurrItem < 0) {
            firstItem = (short) ((currItem - maxItems + itemCount + 1) % itemCount);
            selectedItem = (short) (maxItems - 1);
        } // cursor is at the end
        else if (newCurrItem >= itemCount) {
            firstItem = currItem;
            selectedItem = 0;
        } // cursor is in the middle
        else if (selectedItem + offset >= 0 && selectedItem + offset < maxItems) {
            selectedItem += offset;
        } // cursor is at the beginning of the display
        else if (selectedItem + offset < 0) {
            firstItem = currItem;
            selectedItem = 0;
        } // cursor is at the end of the display
        else if (selectedItem + offset >= maxItems) {
            firstItem = (short) ((currItem - maxItems + itemCount + 1) % itemCount);
            selectedItem = (short) (maxItems - 1);
        }
        
        //#ifdef MUJMAIL_USR_FOLDERS
        // Customize user folder actions
        if ( getSelectedTab() == FOLDERS) {
            addCommand(select);
            if (currItem == MENU_FOLDERS_SEPARATOR) removeCommand(select);
            
            if (currItem >= MENU_FOLDERS_USERBOX_FIRST) {
                addCommand(fldEdit);
                addCommand(fldDel);
            } else {
                removeCommand(fldEdit);
                removeCommand(fldDel);
            }
        }
        //#endif
        repaint();

    }

    /**
     *
     * @return current position of the cursor.
     */
    public short getSelectedIndex() {
        return currItem;
    }

    void cancelTickerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            sindex = 0;
            aindex = 0;
            sStarted = false;
            aStarted = false;
        }
    }

    void cancelRefreshTask() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * Recounts three variables that describe current position of the tab cursor.
     * These variables are:
     * <ul>
     *    <li><B>firstTab</B> - it is in range of <0 .. MAX_TABS - maxTabs -1> where maxTabs is a count
     * of tabs that fits to the display's width. It determines a
     * <CODE>Menu Array</CODE> ID of the first tab on the display.</li>
     *    <li><B>currTab</B> - it is in range of <0 .. MAX_TABS - 1> and holds an ID of the <CODE>Menu Array</CODE>
     * of the tab that is selected.</li>
     *    <li><B>selectedTab</B> - it is in range of <0 .. maxTabs - 1> and describes just relative
     *    position of the cursor from the firstTab which is displayed on selectTab = 0 position.</li>
     * <ul>
     * @param newTab - a new position of the tab cursor.
     */
    public void setSelectedTab(byte newTab) {
        if (DEBUG) { System.out.println("setSelectedTab(byte) - start"); }
        cancelRefreshTask();
        cancelTickerTask();
        // move a cursor of the list item at the begining
        firstItem = 0;
        currItem = 0;
        selectedItem = 0;

        // count new positions
        byte offset = (byte) (newTab - currTab);

        byte newCurrTab = (byte) (currTab + offset);
        currTab = (byte) ((newCurrTab + MAX_TABS) % MAX_TABS);

        // if paint method has not been called yet
        if (maxTabs == 0) {
            firstTab = currTab;
            selectedTab = 0;
        } // cursor is at the beginning
        else if (newCurrTab < 0) {
            firstTab = (byte) ((currTab - maxTabs + MAX_TABS + 1) % MAX_TABS);
            selectedTab = (byte) (maxTabs - 1);
        } // cursor is at the end
        else if (newCurrTab >= MAX_TABS) {
            firstTab = currTab;
            selectedTab = 0;
        } // cursor is in the middle
        else if (selectedTab + offset >= 0 && selectedTab + offset < maxTabs) {
            selectedTab += offset;
        } // cursor is at the beginning of the display
        else if (selectedTab + offset < 0) {
            firstTab = currTab;
            selectedTab = 0;
        } // cursor is at the end of the display
        else if (selectedTab + offset >= maxTabs) {
            firstTab = (byte) ((currTab - maxTabs + MAX_TABS + 1) % MAX_TABS);
            selectedTab = (byte) (maxTabs - 1);
        }

        setTabContext(currTab);

        // TODO: not necessary?
        repaint();

        mujMail.getDisplay().setCurrent(this);

        if (DEBUG) System.out.println("setSelectedTab(byte) - end");
    }

    /**
     * @return current position of the cursor.
     */
    public byte getSelectedTab() {
        return currTab;
    }

    public String getSelectedAccount() {
        if (tabs[getSelectedTab()].item.isEmpty()) {
            return null;
        }
        short index = getSelectedIndex();
        MenuItem itm = (MenuItem) tabs[getSelectedTab()].item.elementAt(index);
        return itm.name;
    }

    private void setTabContext(byte currTab) {
        removeCommand(cmdNew);
        removeCommand(change);
        removeCommand(delete);
        removeCommand(setPrimary);
        removeCommand(retrieve);
        removeCommand(clear);
        //#ifdef MUJMAIL_USR_FOLDERS
        removeCommand(fldAdd);
        removeCommand(fldEdit);
        removeCommand(fldDel);
        //#endif
        addCommand(select);
        switch (currTab) {
            case ACTION:
                break;
            //#ifdef MUJMAIL_USR_FOLDERS
            case FOLDERS:
                addCommand(fldAdd);
                if (getSelectedIndex() >= MENU_FOLDERS_USERBOX_FIRST) {
                    addCommand(fldEdit);
                    addCommand(fldDel);
                }
                break;
            //#endif
            case SETTINGS:
                addCommand(change);
                removeCommand(select);
                break;
            case ACCOUNTS:
                removeCommand(select);
                if (!mujMail.getAccountSettings().isBusy()) {
                    addCommand(cmdNew);
                    if (!mujMail.getMailAccounts().isEmpty()) {
                        addCommand(clear);
                        addCommand(change);
                        addCommand(delete);
                        addCommand(setPrimary);
                        addCommand(retrieve);
                    }
                }
                break;
            case UTILS:
                break;
        }
        //lets refresh the tab, so we don't have to see old info (like the * of TheBox counters)
        refresh(currTab, false);
    }

    private void drawTriangle(short left, short top, short width, boolean toLeft, Graphics g) {
        g.setColor(FONT_COLOR);
        if (toLeft) {
            for (short i = 0; i < width; i++) {
                g.drawLine(left + i, top + width - i, left + i, top + width + i);
            }
        } else {
            for (short i = 0; i < width; i++) {
                g.drawLine(left + i, top + i + 1, left + i, top + 2 * width - i - 1);
            }
        }
    }

    //#ifdef MUJMAIL_TOUCH_SCR
    /**
     * Listens to the mujMail pointer events.
     */
    private class MenuPointerEventListener extends MujMailPointerEventListener.MujMailPointerEventListenerAdapter {

        public void down() {
            downPressedAction();
        }

        public void fire() {
            firePressedAction();
        }

        public void left() {
            leftPressedAction();
        }

        public void right() {
            rightPressedAction();
        }

        public void up() {
            upPressedAction();
        }

        public void upQuartersSlash() {
            up();
        }

        public void downQuartersStar() {
            down();
        }
    }
    //#endif
}
