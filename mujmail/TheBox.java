/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
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


package mujmail;

import mujmail.util.Functions;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import mujmail.account.MailAccount;
import mujmail.ordering.ComparatorStrategy;
import mujmail.ordering.Criterion;
import mujmail.ordering.Ordering;
import mujmail.protocols.InProtocol;
import mujmail.threading.ThreadedEmails;
//#ifdef MUJMAIL_DEBUG_CONSOLE
import mujmail.debug.DebugConsole;
//#endif
//#ifdef MUJMAIL_TOUCH_SCR
import mujmail.pointer.MujMailPointerEventListener;
import mujmail.pointer.MujMailPointerEventProducer;
//#endif

/**
 * Represents boxes: see Inbox, Outbox, ...
 * 
 * Each message is stored in the container and RMS database of one persistent box.
 * Moreover, it can be stored in the container of more Nonpersistent boxes.
 * See documentation of PersistentBox and UnpersistentBox for more details.
 * 
 * It displays the box. That is, it displays headers of mails in the box (paint()) and
 * it displays the progress bar (paintProgress(), report())
 */
public abstract class TheBox extends Canvas implements CommandListener {
    
    /** Set to true if debug information should be displayed while reporting
     messages using methods report() */
    private static final boolean DEBUG = false;
    
    /** The name of this source file */
    private static final String SOURCE_FILE = "TheBox";
    
    private boolean tickerEnabled = true;

    /** The name of the box that is shown to user */
    private String name;
    
    protected final MujMail mujMail;
    /** Mails in the box. */
    protected IStorage storage;

    int deleted; //counter of mails that are going to be deleted
    /**
     * Index of currently selected message.
     * Even if threading is enabled this number is index to the storage vector
     * (the empty messages are skipped in this index).  
     */
    int cur; //the currently selected mail
    /**
     * This number indicates the number of empty message before {@link #cur}
     * index. It's used when showing index of the message in box.
     */
    int empties;
    byte pageJump; //offset of the next page from the current page of email list
    Image imNormal, imDeleted, imRead, imAnswered, imSent, imFailedSent, imAttch, imFlagged, imRoot;
    public Command stop, exit, delete, deleteNow, viewMessage, empty, sort, seen,
            flagged, showHeader;
    boolean btnsHidden = false; //are some buttons hidden?	
    String activity = "";

    //represents sort mode of the box. 
    //the most right bit represents sort order, the other 3bits represents sort criteria
    //the meaning of criterion bits are defined in Functions.SRT_HDR_*
    //private byte sortMode;

    private Ordering ordering;
    private Criterion criterion;

    /** Item used for text rotating (shifting if too long) */
    Timer tickerTimer;
    short tIndex; //aindex is substring index of the tText, from where the ticker should begin			
    boolean tStarted; //indicates whether the ticker has been initiated
    String tText;//a text of the ticker
    int tY; //y-position of the ticker

    private final EventListener eventListener = new EventListener();
    //#ifdef MUJMAIL_TOUCH_SCR
    private final MujMailPointerEventProducer pointerEventTransformer;
    //#endif

    /** Used to paint something below header details. */
    protected MessagePartPainter belowHeaderDetailsPainter;
    /** Used to paint header details. */
    protected MessagePartPainter headerDetailsPainter;

    protected MujMail getMujMail() {
        if (mujMail == null) {
            System.out.println("mujmail is null");
        }
        return MujMail.mujmail;
    }

    /**
     * Increments the number of deleted messages in this box.
     */
    public void incDeleted() {
        deleted++;
    }

    /**
     * @return the ordering
     */
    public Ordering getOrdering() {
        return ordering;
    }

    public void setOrdering(Ordering ordering) {
        this.ordering = ordering;
    }

    /**
     * @return the criterion
     */
    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }

    public IStorage getStorage() {
        return storage;
    }

    public void setStorage(ThreadedEmails storage) {
        //#ifdef MUJMAIL_DEBUG_CONSOLE
        DebugConsole.println("Setting storage " + storage);
        if (storage == null) {
            DebugConsole.println("Setting storage is null");
            return;
        }
        //#endif
        if ( DEBUG && storage != null ) {
              System.out.println("DEBUG InBox.setStorage(ThreadedEmails) - new storage size: " + (storage == null?"":Integer.toString( storage.getSize()) ) );
              System.out.println("DEBUG InBox.setStorage(ThreadedEmails) - new storage: " );
              //#ifdef MUJMAIL_DEVELOPMENT
//#               ((ThreadedEmails)storage).printToConsole();
              //#endif
          }
          
        this.storage = storage;
    }

    private class Ticker extends TimerTask {

        public void run() {
            tStarted = true;
            if (isBusy()) {
                return;
            }
            repaint(); //we just repaint the needed part not whole screen			
        }
    }

    /**
     * Creates the box.
     * 
     * @param mMail 		the main object in the application
     * @param name 			the name of the box
     * @param searchable 	true if the box should be searchable
     */
    public TheBox(MujMail mMail, String name) {
        
        setBelowHeaderDetailsPainter();
        setHeaderDetailsPainter();
        
        this.name = name;
        mujMail = mMail;
        //storage = new Vector();
        storage = new ThreadedEmails();
        imNormal = Functions.getIcon("m_normal.png");
        imDeleted = Functions.getIcon("m_deleted.png");
        imRead = Functions.getIcon("m_opened.png");
        imAnswered = Functions.getIcon("m_answered.png");
        imSent = Functions.getIcon("m_sent.png");
        imFailedSent = Functions.getIcon("m_failed_send.png");
        imAttch = Functions.getIcon("m_attachment.png");
        imFlagged = Functions.getIcon("m_flagged.png");
        imRoot = Functions.getIcon( "m_root.png" );

        exit = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        viewMessage = new Command(Lang.get(Lang.BTN_TB_VIEW_MESS), Command.OK, 1);
        stop = new Command(Lang.get(Lang.BTN_TB_STOP), Command.STOP, 2);
        delete = new Command(Lang.get(Lang.BTN_DEL_UNDEL), Command.ITEM, 4);
        deleteNow = new Command(Lang.get(Lang.BTN_TB_DEL_NOW), Command.ITEM, 5);
        empty = new Command(Lang.get(Lang.BTN_CLEAR), Command.ITEM, 7);
        sort = new Command(Lang.get(Lang.BTN_TB_SORT), Command.ITEM, 9);
        seen = new Command(Lang.get(Lang.BTN_TB_MARK_SEEN), Command.ITEM, 6);
        flagged = new Command(Lang.get(Lang.BTN_TB_MARK_FLAGGED), Command.ITEM, 10);
        showHeader = new Command(Lang.get(Lang.BTN_MF_HEADERS_DETAILS), Command.ITEM, 11);
        addCommand(sort);
        addCommand(deleteNow);
        addCommand(viewMessage);
        addCommand(exit);
        addCommand(delete);
        addCommand(empty);
        addCommand(seen);
        addCommand(flagged);
        addCommand(showHeader);
        setCommandListener(this);
        
        //#ifdef MUJMAIL_TOUCH_SCR
        pointerEventTransformer = new MujMailPointerEventProducer(eventListener, getWidth(), getHeight());
        //#endif

          // TODO (Betlista): this shouldn't be here (my opinion, I think it should be loaded or something)
        this.ordering = Ordering.NATURAL;
        this.criterion = Criterion.TIME;
    }

    public void commandAction(Command c, Displayable d) {
          if (DEBUG) System.out.println( "DEBUG TheBox.commandAction(Command, Displayable)  - displayable: " + d.getClass().toString() );
        standardButtons(c);
    }

    /// Manages standard command actions of the boxes
    private void standardButtons(Command c) {
        if (c == viewMessage) {
            MujMail.mujmail.mailForm.viewMessage(getSelectedHeader(), this);
        } else if (c == exit) {
            exit();
        } else if (c == delete) {
            markAsDeleted(getSelectedHeader());
        } else if (c == deleteNow) {
            deleteMarkedFromBoxAndDB();
        } else if (c == seen) {
        	markSeen(getSelectedHeader());
        } else if (c == flagged) {
        	markFlagged(getSelectedHeader());
        } else if (c == empty) {
            deleteAllMailsFromBoxAndDB(false);
        } else if (c == sort) {
            MujMail.mujmail.getSettings().showSortFrm(this);
        } else if (c == showHeader) {
              if (DEBUG) System.out.println( "DEBUG TheBox.standardButtons() - c == showHeader" );
            MujMail.mujmail.mailForm.showHeader(getSelectedHeader(), this);
        }

    }

    

    public String toString() {
        return name;
    }
    
    /**
     * Gets the enumeration of all messages in this box.
     * @return the enumeration of all messages in this box
     */
    public Enumeration getMessages() {
        return storage.getEnumeration();
    }
    
    /**
     * @return gets box name
     */
    public final String getName() {
        return name;
    }
    
    /** 
     * Changes name of the box.
     * @param newName Name to set.
     */
    public void setName(String newName) {
        if (newName != null) {
            name = newName;
        }
    }
    
    /**
     * Gets number of messages in this box.
     * If threading is enabled it returns number of messages without empty root
     * messages.
     * 
     * @return the number of messages.
     */
    public int getMessageCount() {
    	final int storageSize = storage.getSize();
    	int emptyRootsNumber = 0;
    	if ( storage instanceof ThreadedEmails ) {
    		emptyRootsNumber = ((ThreadedEmails)storage).getEmptyRootsNumber();
    	}
        return storageSize - emptyRootsNumber;
    }

    // TODO (Betlista): why there are 2 methods for retrieving messages? (storageAt(int), getMessage(int) ) 
    /*
     * Return i-th message in storage.
     * 
     * @param index of the message to be retrieved
     * @return message for requested index or null if there is not message with such index
     */
//    public MessageHeader storageAt(int index) {
//        if (index >= storage.size() || index < 0) {
//            return null;
//        }
//        return (MessageHeader) storage.elementAt(index);
//    }

    /**
     * Return i-th message in storage.
     * 
     * @param index of the message to be retrieved
     * @return message for requested index or null if there is not message with such index
     */
    public MessageHeader getMessageHeaderAt(int index) {
        return storage.getMessageAt( index );
    }

    public MessageHeader getSelectedHeader() {
//        return storageAt(getSelectedIndex());
        return getMessageHeaderAt( getSelectedIndex() );
    }

    /**
     * Indicates whether there proceeds some action beyond the mails in this
     * box.
     * @return true if there proceeds some action beyond the mails int this
     * box
     * 
     * TODO: this is now used only for canceling ticker. Refactor!
     */
    protected boolean isBusy() {
        return false;
    }
    
    /**
     * Displays report message which has originated because of some exception.
     * 
     * @param report message which to display. If it ends with '+' it will
     *  be just printed to the standard output, if it ends with '*', the focus
     *  will be returned back after displaying the message.
     * @param sourceFile the source file where the message which is reported
     *  was originated
     * @param ex the exception because of the message is reported
     */
    public void report(String report, String sourceFile, Exception ex) {
        if (DEBUG) {
            ex.printStackTrace();
        }
        
        report(report, sourceFile);
    }
    
    /**
     * Stores mail to this box. 
     * If this box is persistent, makes the copy of given mail and stores it to
     * the RMS database and container of this box.
     * If this box is not persistent, stores the mail only to the container of
     * this box and does not make a copy.
     * @param header the mail to be stored
     * @return the mail that was stored (in case of persistent box, this is the
     *  copy of mail in parameter)
     */
    public abstract MessageHeader storeMail(MessageHeader header);

    /**
     * Displays report message.
     * 
     * @param report message which to display. If it ends with '+' it will
     *  be just printed to the standard output, if it ends with '*', the focus
     *  will be returned back after displaying the message.
     * @param sourceFile the source file where the message which is reported
     *  was originated
     */
    public synchronized void report(String report, String sourceFile) {
          if (DEBUG) System.out.println("DEBUG " + sourceFile + " - " + report);
        //return;
        if (report.startsWith("+")) //not important messages
        {
            return;
        }
        Displayable display;
        activity = report;

        if (report.endsWith("*")) //then we need to get back the focus afted displaying an alert
        {
            display = this;
        } else {
            display = null;
        }

        if (report.startsWith("100:")) {
            getMujMail().alert.setAlert(this, display, report.substring(4), MyAlert.DEFAULT, AlertType.ERROR);
        } else if (report.startsWith("200:")) {
            getMujMail().alert.setAlert(this, display, report.substring(4), MyAlert.DEFAULT, AlertType.WARNING);
        } else if (report.startsWith("300:")) {
            getMujMail().alert.setAlert(this, display, report.substring(4), MyAlert.DEFAULT, AlertType.INFO);
        }

        repaint();

        if (report.startsWith("*")) { //important message
            Functions.sleep(1500);
        }
    }

    /**
     * Set the information that will be displayed on the progress bar.
     * Note, that progress bar will be displayed only if isBusy() returns true.
     *
     * TODO: replace by tasks
     * 
     * @param activity the message displayed on the progress bar
     *  if it starts with *, this thread will be 500 milliseconds sleep
     * @param max the number of time units when the activity will be finished
     * @param actual actual number of time units
     */
    private synchronized void setProgress(String activity, int max, int actual) {
        this.activity = activity;
        repaint();

        if (activity.startsWith("*")) {
            Functions.sleep(500);
        }
    }

    public void markAsDeleted(MessageHeader message) {
        if (message != null) {
            if (message.deleted) //was it marked as deleted?
            {
                --deleted;
            } //decrease the counter for marked mails
            else {
                ++deleted;
            }
            message.deleted = !message.deleted; //change its state

            /*
            MailAccount msgAcct = (MailAccount)this.getMujMail().mailAccounts.get(message.accountID);
            
            //Set '\Deleted' flag on server if it's an IMAP account
            if (msgAcct.type == MailAccount.IMAP)
            {
            	msgAcct.protocol.setFlags(message, "(\\Deleted)");
            }
            */

            shiftSelectedIndex(true);

            repaint();
        }
    }

    /**
     * Marks a given message as Seen/Unseen depending on the current flag.
     * In case the message's account is of type IMAP4, command to update
     * IMAP server is send too.
     * @param message		Message to mark as Seen/Unseen
     */
    public void markSeen(MessageHeader message) {
        MailAccount msgAcct = (MailAccount)this.getMujMail().getMailAccounts().get(message.getAccountID());

    	if (message.readStatus == MessageHeader.READ)
    	{
    		message.readStatus = MessageHeader.NOT_READ;
            //Unset '\Seen' flag on server if it's an IMAP account
            if (msgAcct != null && msgAcct.isIMAP())
            {
            	msgAcct.getProtocol().setFlags(message, "(\\Seen)", InProtocol.REMOVE_FLAGS, this);
            }    	
    	}
    	else
    	{
    		message.readStatus = MessageHeader.READ;
            //Set '\Seen' flag on server if it's an IMAP account
            if (msgAcct != null && msgAcct.isIMAP())
            {
            	msgAcct.getProtocol().setFlags(message, "(\\Seen)", InProtocol.SET_FLAGS, this);
            }    	
    	}
    	
    	try {
            // msgAcct.getProtocol().getBox().mailDB.saveHeader(message); // Alf: Why so complicated??
            message.getBox().getMailDB().saveHeader(message);
        } catch (MyException e) {
            MujMail.mujmail.alert.setAlert("Error saving message header", AlertType.ERROR);
        }
        
        repaint();
    }

    /**
     * Marks a given message as Seen/Unseen depending on the current flag.
     * In case the message's account is of type IMAP4, command to update
     * IMAP server is send too.
     * @param message		Message to mark as Seen/Unseen
     */
    public void markFlagged(MessageHeader message) {
        MailAccount msgAcct = (MailAccount)this.getMujMail().getMailAccounts().get(message.getAccountID());
    	if (message.flagged)
    	{
    		message.flagged = false;
            //Unset '\Flagged' flag on server if it's an IMAP account
            if (msgAcct != null && msgAcct.isIMAP())
            {
            	msgAcct.getProtocol().setFlags(message, "(\\Flagged)", InProtocol.REMOVE_FLAGS, this);
            }
    	}
    	else
    	{
    		message.flagged = true;
            //Set '\Flagged' flag on server if it's an IMAP account
            if (msgAcct != null && msgAcct.isIMAP())
            {
            	msgAcct.getProtocol().setFlags(message, "(\\Flagged)", InProtocol.SET_FLAGS, this);
            }
    	}

    	try {
            //  msgAcct.getProtocol().getBox().mailDB.saveHeader(message);
            message.getBox().getMailDB().saveHeader(message);
    	} catch (MyException e) {
            MujMail.mujmail.alert.setAlert("Error saving message header", AlertType.ERROR);
    	}
    	
    	repaint();
    }

    /**
     * Do the work of deleting all mails from database. Called by 
     * deleteAllMailsFromBoxAndDB(boolean).
     * If the box is Persistent, should delete all mails from database of this
     * persistent box.
     * If the box is Unpersistent, should delete also messages from containers
     * of boxes to which stored mails belong.
     */
    protected abstract void deleteAllMailsFromDB();
    
    public void exit() {
        getMujMail().mainMenu();
    }
    
    /**
     * Returns true if this box is empty.
     * @return true if this box is empty.
     */
    public boolean isEmpty() {
        return storage.isEmpty();
    }
    
    /**
     * Deletes all mails in this storage from this storage and from database.
     * Note that this storage does not store any mails so the mails will be
     * deleted from databases of other boxes.
     * 
     * @param sure if it is true, deletes mails in spite of the box is busy and
     *  don't ask user.
     */
    public void deleteAllMailsFromBoxAndDB(boolean sure) {
        if (!sure) {
            if (isBusy()) {
                getMujMail().alert.setAlert(this, this, Lang.get(Lang.ALRT_SYS_BUSY), MyAlert.DEFAULT, AlertType.INFO);
            } else {
                getMujMail().alert.setAlert(this, this, Lang.get(Lang.ALRT_SYS_DEL_ALL_CONFIRM), MyAlert.DB_CLEAR_CONFIRM, AlertType.CONFIRMATION);
            }
            return;
        }
        
        deleteAllMailsFromDB();
        
        synchronized (storage) {
            storage.removeAllMessages();
        }
        
        deleted = 0;
        cur = 0;
        repaint();
    }
    
    /**
     * Immediately deletes given message.
     * @param message the message to be deleted
     * @param  trashMode describes the storing of deleted mail to trash
     */
    public void deleteNowFromBoxAndDB(MessageHeader message, 
            Trash.TrashModes trashMode) {
        message.deleteFromDBAndBox(this, trashMode);
        cur = 0;
        repaint();
    }
    
    /**
     * Do the physical work of deleting marked mails from box and database.
     * Called by deleteMarkedFromBoxAndDB().
     */
    protected abstract void doDeleteMarkedFromBoxAndDB();

    /**
     * Do batch deleteFromDBAndBox of all messages marked as deleted.
     */
    public void deleteMarkedFromBoxAndDB() {
        if (deleted > 0) {
            cancelTicker();
            doDeleteMarkedFromBoxAndDB();
            cur = 0;
            
            repaint();
        }
    }

    protected void cancelTicker() {
        if (tickerTimer != null) {
            tickerTimer.cancel();
            tickerTimer = null;
            tIndex = 0;
            tStarted = false;
        }
    }
    
    /** Shows Box on foreground of display */
    void showBox() {
        mujMail.getDisplay().setCurrent(this);
    }

    /**
     * Cancels ticker and disables running of ticker.
     */
    public void disableTicker() {
        cancelTicker();
        tickerEnabled = false;
    }
    
    /**
     * Enables running of ticker.
     */
    public void enableTicker() {
        tickerEnabled = true;
    }

    public int getSelectedIndex() {
        cancelTicker();
        return cur;
    }

    /**
     * Moves the current index (depends on direction) and returns new current
     * index. It works as cyclic list (from last item it's moved to the first).
     * 
     * @param cur current position
     * @param direction where to move, <code>true</code> moves down (+1)
     * @return new index to current item
     */
    private int moveCurrent( final int cur, final boolean direction ) {
          if (DEBUG) System.out.println( "DEBUG TheBox.moveCurrent(cur=" + cur + ", direction=" + direction + ")" );
    	int result = cur;
        final int shift = direction ? 1 : -1 ;
        final int size = storage.getSize();

        result += shift;
        if ( result == size ) { // if we were at the last message
        	result = 0; // move to the first one
        	  // when we are at the first item there are no empty items
        	  //   at the previous items
    		empties = 0;
        } else if ( result == -1 ) { // if we were at the first message
        	result = size - 1; // move to the last one (it cannot be empty message)
    		if ( storage instanceof ThreadedEmails ) {
    			  // when we are at the last item (which cannot be empty), there
    			  //   are all empty items at the previous positions
    			empties = ((ThreadedEmails)storage).getEmptyRootsNumber();
    		}
        }

    	return result;
    }
    /**
     * Moves cursor with requested shift.
     * It's needed to compute {@link #cur} and {@link #empties} too.
     * 
     * @param direction defines the direction, there are just 2 possibilities
     *        down/up, so I chose boolean, <code>true</code> means move down
     */
    public void shiftSelectedIndex( final boolean direction ) { //i is the offset from actual position
          if (DEBUG) System.out.println( "DEBUG TheBox.shiftSelectedIndex(direction=" + direction + ")" );
        final int size = storage.getSize();
        final int shift = direction ? 1 : -1;
        if ( size != 0 ) {
            cur = moveCurrent(cur, direction);
              // additional check is if the message we are at just now is empty
            MessageHeader mh = storage.getMessageAt( cur );
            while ( mh.isEmpty() ) {
        		empties += shift;
            	cur = moveCurrent(cur, direction);
            	mh = storage.getMessageAt( cur );
            }
              if (DEBUG) System.out.println( "DEBUG TheBox.shiftSelectedIndex(direction=" + direction + ") - cur=" + cur + ", empties=" + empties );
            repaint();
        }
    }

    protected void keyPressed(int keyCode) {
          if (DEBUG) System.out.println( "DEBUG TheBox.keyPressed(keyCode=" + keyCode + ") - key name: " + getKeyName(keyCode) );
        cancelTicker();
        switch (keyCode) {
            case '1':
                markAsDeleted(getSelectedHeader());
                return;
            case '*':
                eventListener.down();
                return;
            case '#':
                eventListener.up();
                return;

        }
        switch (getGameAction(keyCode)) {
            case UP:
                eventListener.upQuartersSlash();
                break;
            case DOWN:
                eventListener.downQuartersStar();
                break;
            case LEFT:
                eventListener.left();
                break;
            case FIRE:
                eventListener.fire();
                break;
        }
        repaint();
    }

    protected void keyRepeated(int keyCode) {
    	  if (DEBUG) System.out.println( "DEBUG TheBox.keyRepeated(keyCode=" + keyCode + ") - key name: " + getKeyName(keyCode) );
    	/*
    	 * This condition below is little hack :-/
    	 * There was situation in which user pressed for example DOWN key, but
    	 * from somewhere there was SELECT action generated (retrieved from
    	 * DefaultEventHandler).
    	 * 
    	 * If there will be problem in future with this method remove it, but
    	 * I really like it O:-)
    	 * 
    	 *                                                     Betlista
    	 */
    	final int gameAction = getGameAction(keyCode);
    	if ( gameAction == UP || gameAction == DOWN ) {
    	    keyPressed(keyCode);
    	}
    }

    //#ifdef MUJMAIL_TOUCH_SCR
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);

        cancelTicker();
        System.out.println("Pointer pressed: " + x + "; " + y);
        pointerEventTransformer.pointerPressed(x, y);
    }
    //#endif

    protected void hideButtons() {
        if (!btnsHidden) {
            removeCommand(deleteNow);
            removeCommand(viewMessage);
            removeCommand(delete);
            removeCommand(empty);
            removeCommand(sort);
            removeCommand(seen);
            removeCommand(flagged);
            removeCommand(showHeader);
            btnsHidden = true;
        }
    }

    protected void showButtons() {
        if (btnsHidden) {
            addCommand(deleteNow);
            addCommand(viewMessage);
            addCommand(delete);
            addCommand(empty);
            addCommand(sort);
            addCommand(seen);
            addCommand(flagged);
            addCommand(showHeader);
            btnsHidden = false;
        }
    }

    public void resort() {
        cancelTicker();
        if ( storage != null ) {
            synchronized (storage) {
                report(Lang.get(Lang.ALRT_SORTING) + Lang.get(Lang.ALRT_WAIT), SOURCE_FILE);
                storage.sort( ComparatorStrategy.getStrategy().getComparator( ordering, criterion ) );
                //Functions.sort(storage, (byte) (getSortMode() % 2), (byte) (getSortMode() >> 1));
            }
        }
        cur = 0;
    }

    protected void hideNotify() {
        cancelTicker();	//every time we lose focus, cancel the ticker	
    }

    protected void drawIcons(MessageHeader mail, Graphics g, int x, int y) {
        if ( mail.isEmpty() ) {
        	g.drawImage( imRoot, x, y + 3, Graphics.TOP | Graphics.LEFT );
        } else if (mail.sendStatus == MessageHeader.REPLIED) {
            g.drawImage(imAnswered, x, y + 3, Graphics.TOP | Graphics.LEFT);
        } else if (mail.sendStatus == MessageHeader.FAILED_TO_SEND) {
            g.drawImage(imFailedSent, x, y + 3, Graphics.TOP | Graphics.LEFT);
        } else if (mail.sendStatus == MessageHeader.SENT) {
            g.drawImage(imSent, x, y + 3, Graphics.TOP | Graphics.LEFT);
        } else if (mail.readStatus == MessageHeader.READ) {
            g.drawImage(imRead, x, y + 3, Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawImage(imNormal, x, y + 3, Graphics.TOP | Graphics.LEFT);
        }
        if (mail.deleted) {
        	g.drawImage(imDeleted, x, y + 3, Graphics.TOP | Graphics.LEFT);
        }
    }


    /**
     * Redefine to set another object that paints item below header details to
     * the variable MessagePartPainter belowHeaderDetailsPainter.
     * 
     * This method sets MessagePartPainter.DEFFAULT_PAINTER that does not paint 
     * anything.
     */
    protected void setBelowHeaderDetailsPainter() {
        belowHeaderDetailsPainter = MessagePartPainter.DEFAULT_PAINTER;
    }
    
    /**
     * Redefine to set another object that paints header details to the variable 
     * MessagePartPainter headerDetailsPainter.
     * 
     * This method sets the object of instance HeaderDetailsPainter.
     */
    protected void setHeaderDetailsPainter() {
        headerDetailsPainter = new HeaderDetailsPainter();
    }
    
    /**
     * Gets the number of actually painted item.
     * 
     * @param g the graphics object used to paint
     * @param screenHeight the height of the screen
     * @param headlineHeight the height of the headline
     * @return the number of actually painted item
     */
    private int getActuallyPaintedItem(Graphics g, int headlineHeight, int subjectHeight, int screenHeight) {
          //System.out.println("DEBUG TheBox.getActuallyPaintedItem(Graphics, int, int, int) - start");
        int selectedMailHeight = subjectHeight + 
            headerDetailsPainter.getHeight(g, true) +
            belowHeaderDetailsPainter.getHeight(g, true) + 
            1;  // the line separating mails
        int notSelectedMailHeight = subjectHeight + 
            headerDetailsPainter.getHeight(g, false) +
            belowHeaderDetailsPainter.getHeight(g, false) + 
            1;  // the line separating mails
        // the number of not selected mails in the page
        int maxItemsPerPage = (screenHeight - headlineHeight - selectedMailHeight) / notSelectedMailHeight;
        maxItemsPerPage++;  // one mail is selected
        //System.out.println("Max items : " + maxItemsPerPage);
        
        
        final int size = storage.getSize();
        if (pageJump != 0 || cur >= maxItemsPerPage) { //if the user pressed * or # or cursor is doesn't fit to the first page of display
            int currentPage = cur / maxItemsPerPage; //change current page
            //change the current page if the user wanted to jump to another page. 
            currentPage = (currentPage + pageJump + size / maxItemsPerPage + 1) % (size / maxItemsPerPage + 1); //+1 to prevent modulo by 0 
            int actItem = currentPage * maxItemsPerPage; //set the first item to be drawn on the page			
            
            //System.out.println("DEBUG TheBox.getActuallyPaintedItem(Graphics, int, int, int) - end (" + actItem + ")");
          return actItem;
            
        } else {
              //System.out.println("DEBUG TheBox.getActuallyPaintedItem(Graphics, int, int, int) - end (0)");
            return 0;
        }
    }

//    private void printDot(final Graphics g, final int x, final int y) {
//        g.drawLine(x, y, x, y);
//    }

    /* *
     * This method paints before message the '-' sign if the message is root
     * message in threaded emails.
     *   
     * @param g
     * @param x
     * @param y
     */
//    private void printThreadSign(final Graphics g, final int x, final int y) {
//        int color = g.getColor();
//        g.setColor( 50, 50, 50 );
//        g.drawRect( x + 1, y + 3, 8, 8 );
//        g.drawLine( x + 3, y + 7, x + 7, y + 7 );
//        printDot(g, x + 11, y + 7);
//        printDot(g, x + 13, y + 7);
//        g.setColor( color );
//    }

    /* *
     * 
     * @param g
     * @param x
     * @param y
     */
//    private void printDots(final Graphics g, final int x, final int y) {
//        final int STEP = 2;
//        final int CORNER = 16;
//        for (int i = 2; i < CORNER; i += STEP) {
//            printDot(g, x, y + i);
//        }
//        for (int i = 0; i < 11; i += STEP) {
//            printDot(g, x + i, y + CORNER);
//        }
//    }

    /**
     * Paints the box that is empty or busy.
     */
    protected void paintIsBusy() {

    }

    /**
     * Returns title for the box.
     * @return
     */
    private String getBoxTitle() {
    	final StringBuffer buff = new StringBuffer( getName() );

        // String boxName = size > 0 ? getName() + " (" + (cur + 1) + "/" + (size) + ")" : getName() + " (" + Lang.get(Lang.EMPTY) + ")";
    	final int storageSize = storage.getSize();

    	if ( storageSize > 0 ) {
    		buff.append(" (").append( cur + 1 - empties ) // number of current message
    			.append('/')
    			.append( getMessageCount() ) // count of all non empty messages
    			.append(')');
    	} else {
    		buff.append(" (").append( Lang.get(Lang.EMPTY) ).append(')');
    	}
    	return buff.toString();
    }

    private void printTitle( Graphics g ) {
        if (Settings.fontSize == Settings.FONT_NORMAL) {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)); //set font size for box's headline
        } else {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE)); //set font size for box's headline             
        }

        int fontHeight = g.getFont().getHeight();
        g.setColor(184, 179, 255);
        g.fillRect(0, 0, getWidth(), fontHeight + 3);
        String boxTitle = getBoxTitle();
        g.setColor(0x00000000); // black color
        g.drawString(boxTitle, getWidth() / 2 - g.getFont().stringWidth(boxTitle) / 2, 1, Graphics.TOP | Graphics.LEFT);

        if (Settings.fontSize == Settings.FONT_NORMAL) {
            g.setFont( Font.getDefaultFont() );
        } else {
            g.setFont( Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE) );
        }
    }

    boolean direction = true;

    /**
     * Paints the list of headers of mails in this box.
     * If there is proceeding some operation beyond the mails in the box,
     * displays progress bar.
     * @param g
     */
    protected void paint(Graphics g) {
        try {
            if (storage.isEmpty() || isBusy()) {
                cancelTicker();
                hideButtons();
            } else {
                showButtons();
                if (tStarted) {  //if this paint call is from ticker			 
                    g.setColor(121, 111, 255);
                    int cw = getWidth();
                    int ch = Font.getDefaultFont().getHeight();
                    g.fillRect(0, tY, cw, ch);
                    g.setColor(255, 255, 255);
                    if (Settings.fontSize == Settings.FONT_NORMAL)
                    	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                    else
                    	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));                	
    
                    //do the ticker stuff
                    Functions.Ticker(tText, tIndex, 1, tY, cw - 1, g, Graphics.TOP | Graphics.LEFT);
                    tIndex = (short) ((tIndex + 1) % tText.length());
                    return;
                }
            }
    
            // screen parameters
            int screenHeight = getHeight();
            int screenWidth = getWidth();
            if (Settings.fontSize == Settings.FONT_NORMAL)
            	g.setFont(Font.getDefaultFont());
            else
            	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));

            int fontHeight = g.getFont().getHeight();
            
            //fill the screen with white color
            g.setColor(0x00ffffff); // white color
            g.fillRect(0, 0, screenWidth, screenHeight);
    
    
            if (isBusy()) {
                paintIsBusy();
                return;
            }
    
            //synchronization saves us from crashing, when the storage is being modified in another thread(ie deleting)
            //so the size of storage is changed and we may take an element that is out of array index
            synchronized (storage) {
                final int size = storage.getSize();
                  //System.out.println( "DEBUG TheBox.paint() - storage size: " + storage.getSize() );
                  // vertical coordinate where to start painting
                int actItem = 0;    // actually painted item
                if (Settings.fontSize == Settings.FONT_NORMAL) {
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)); //set font size for box's headline
                } else {
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE)); //set font size for box's headline            	
                }
    
                // -3 because of headline's spacing
                actItem = getActuallyPaintedItem(g, g.getFont().getHeight() - 3, fontHeight, screenHeight);
                if (pageJump != 0) {
                    cur = actItem;
                    pageJump = 0;
                }
    
                //headline: for example the folder name and number of currently 
                // selected message
                int y = g.getFont().getHeight() + 3;
                printTitle(g);
    
                // paint messages
                while (y < screenHeight && actItem < size) {
                      //System.out.println("DEBUG TheBox.paint() - actItem: " + actItem);
                    //MessageHeader mail = (MessageHeader) storage.getMessageAt( size - actItem - 1 );
                    MessageHeader mail = storage.getMessageAt( actItem );
                    if ( mail.isEmpty() ) {
                        if ( actItem == cur) {
                            empties += (direction)?1:-1;
                            cur = moveCurrent(cur, direction);
                            printTitle(g);
                        }
                    	++actItem;
                    	continue;
                    }

                    if (actItem == cur) { //fill the selected item's background
                        g.setColor(121, 111, 255);
                        g.fillRect(0, y, screenWidth, 2 * fontHeight);
                        g.setColor(255, 255, 255);
                    } else {
                        g.setColor(0x00000000);
                    }

                      // horizontal coordinate where to start painting
                    int x = 1;

                    if (storage instanceof ThreadedEmails) {
                        ThreadedEmails tm = (ThreadedEmails)storage;
                        int level = tm.getLevel( mail.getThreadingMessageID() );
                          // we want to indent only first 10 levels because 
                          //   there is not enought space for message left
                        level = Math.min(level, 10);
                        x += ( 5 * level );
                    }

                    //icons
                    drawIcons(mail, g, x, y);
                    x += 12;
                    //has attachments
                    if (mail.messageFormat == MessageHeader.FRT_MULTI) {
                        g.drawImage(imAttch, x - 3, y + 3, Graphics.TOP | Graphics.LEFT);
                        x += 4;
                    }
                    //is flagged
                    if (mail.flagged) {
                    	g.drawImage(imFlagged, x - 3, y, Graphics.TOP | Graphics.LEFT);
                    	x += 12;
                    }
    
                    // subject
                    String newSbj = mail.getSubject().length() == 0 ? Lang.get(Lang.TB_NO_SUBJECT) : mail.getSubject();
                    if (g.getFont().stringWidth(newSbj) > screenWidth - x - 1) {	//does it fit to the display?				
                        newSbj = Functions.cutString(newSbj, 0, screenWidth - x - 1 - g.getFont().stringWidth(".."), g).trim() + "..";
                    }
                    g.drawString(newSbj, x + 1, y, Graphics.TOP | Graphics.LEFT);
    
    
                    y += fontHeight;
                    
                    // paint header details
                    y = headerDetailsPainter.paint(g, mail, actItem == cur, 
                        new ScreenParameters(fontHeight, screenWidth, y));
                    
                    // paint items below header details
                    y = belowHeaderDetailsPainter.paint(g, mail, actItem == cur, 
                        new ScreenParameters(fontHeight, screenWidth, y));
                    
                    // paint the line separating each mail
                    g.setColor(228, 228, 228);
                    g.drawLine(0, y, screenWidth, y);
                    ++y;
                    ++actItem;
                } // while
            } // synchronized
        } catch (Throwable t) {
            System.err.println("ERROR - TheBox.paint()");
            t.printStackTrace();
        }
    }
    
    protected class HeaderDetailsPainter implements MessagePartPainter {

        public int getHeight(Graphics g, boolean isMailSelected) {
            if (!isMailSelected) {
                return 0;
            } else {
                Font font = g.getFont();
                if (Settings.fontSize == Settings.FONT_NORMAL)
                	g.setFont(Font.getDefaultFont());
                else
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
                int messageFontHeight = g.getFont().getHeight();
                g.setFont(font);

                return (isMailSelected) ? messageFontHeight + 1 : messageFontHeight;
            }
        }

        /**
         * Paints header details. Called by method paint.
         * Paints all header details on single line. If the line is too long, uses
         * ticker to scroll through the line.
         * This implementation paints header details only if the mail is currently
         * selected. See parameter isMailSelected.
         * 
         * @param g the graphics object used for painting.
         * @param mail the mail which header details will be painted
         * @param isMailSelected true if the mail which header details are painted is
         *  currently selected.
         * @param screenParameters the parameters of the screen
         * @return vertical (y) position on the screen where next item can be painted
         */
        public int paint(Graphics g, MessageHeader mail, boolean isMailSelected, ScreenParameters screenParameters) {
            if (!isMailSelected) {
                return screenParameters.actYPosition;
            } else {

                int screenWidth = screenParameters.screenWidth;
                int y = screenParameters.actYPosition;
                int fontHeight = screenParameters.defaultFontHeight;

                if (Settings.fontSize == Settings.FONT_NORMAL)
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                else
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));                	
                
                //Build the string which will be shown below
                //message header
                String info = (TheBox.this == getMujMail().getInBox()) ? 
                		Lang.get(Lang.ML_FROM) + " " + mail.getFrom() + " " + 
                		mail.getRecipients() + " " +
                		Lang.get(Lang.ML_MAIL_ACCOUNT) + " " + mail.getAccountID() 
                		: mail.getRecipients();
                		
                //add size to the info string
                info = mail.getSize() < 1024 ? info + "  " + Lang.get(Lang.ML_SIZE) + " " + mail.getSize() + "B" : info + "  " + Lang.get(Lang.ML_SIZE) + " " + mail.getSize() / 1024 + "kB";
                //display time, depending if its today or not, 86400000 = 24*60*60*1000
                if (System.currentTimeMillis() / 86400000 == mail.getTime() / 86400000) {
                    info = info + "  " + Lang.get(Lang.ML_TIME) + " " + mail.getShortTimeStr();
                } else {
                    info = info + "  " + Lang.get(Lang.ML_TIME) + " " + mail.getTimeStr().substring(5, 11);
                }

                //does it fit in?	
                if (tickerEnabled && g.getFont().stringWidth(info) > screenWidth - 1) {
                    if (tickerTimer == null) { //lets init the ticker																			
                        StringBuffer bf = new StringBuffer(info);
                        short space = (short) (screenWidth / (2 * g.getFont().charWidth(' ')));
                        for (short i = 0; i < space; i++) //lets fill it with half of screen of spaces
                        {
                            bf.append(' ');
                        }
                        tText = bf.toString();
                        info = Functions.cutString(info, 0, screenWidth - 1, g);
                        g.drawString(info, 1, y, Graphics.TOP | Graphics.LEFT);
                        tickerTimer = new Timer();
                        tY = y;
                        tickerTimer.schedule(new Ticker(), 100, 100);
                    }
                } else {
                    g.drawString(info, 1, y, Graphics.TOP | Graphics.LEFT);
                }

                y += fontHeight;
                if (Settings.fontSize == Settings.FONT_NORMAL)
                	g.setFont(Font.getDefaultFont());
                else
                	g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));

                return y;
            }
        }
        
        
    }

    /**
     * Interface for objects used to paint parts of messages while painting
     * the list of messages from this box.
     * Such objects are used in method paint of the class TheBox.
     * To change the way of painting of given part, redefine method setXXXPainter()
     * in the descendant of the class TheBox and set to variable used to paint
     * given message part another object implementing this interface.
     */
    protected interface MessagePartPainter {
        /** Dummy instance - does not paint anything. */
        public static final MessagePartPainter DEFAULT_PAINTER = new DefaultPainter();
        
        /**
         * Gets the height of this message part.
         * @param g the graphics object used for painting.
         * @param isMailSelected true if this mail is currently selected.
         * @return the height of this message part
         */
        public int getHeight(Graphics g, boolean isMailSelected);
        
        /**
         * Paints given message part.
         * 
         * @param g the graphics object used for painting.
         * @param mail the mail which part is painted
         * @param isMailSelected true if this mail is currently selected.
         * @param screenParameters the parameters of the screen
         * @return vertical (y) position on the screen where next item can be painted
         */
        public int paint(Graphics g, MessageHeader mail, boolean isMailSelected, ScreenParameters screenParameters);
        
        /**
         * Default implementation that does not paint anything.
         */
        public static class DefaultPainter implements MessagePartPainter {

            public int getHeight(Graphics g, boolean isMailSelected) {
                return 0;
            }

            public int paint(Graphics g, MessageHeader mail, boolean isMailSelected, ScreenParameters screenParameters) {
                return screenParameters.actYPosition;
            }
            
        }
    }
        
    /**
     * Represents screen parameters.
     */
    protected static class ScreenParameters {
        private final int defaultFontHeight;
        private final int screenWidth;
        private final int actYPosition;

        /**
         * 
         * @param defaultFontHeight the height of default font
         * @param screenWidth the width of the screen
         * @param actYPosition actual position when painting in the screen
         */
        public ScreenParameters(int defaultFontHeight, int screenWidth, int actYPosition) {
            this.defaultFontHeight = defaultFontHeight;
            this.screenWidth = screenWidth;
            this.actYPosition = actYPosition;
        }

    }
    
    
    //#ifdef MUJMAIL_TOUCH_SCR
    protected class EventListener extends  MujMailPointerEventListener.MujMailPointerEventListenerAdapter {
    //#else
//#     protected class EventListener {
    //#endif
        public void down() {
            pageJump = 1;
            repaint();
        }

        public void up() {
            pageJump = -1;
            repaint();
        }

        public void left() {
            commandAction(exit, TheBox.this);
        }

        public void right() {
        }

        public void fire() {
            if ( cur >= 0 && cur < storage.getSize() ) {
                commandAction(viewMessage, TheBox.this);
            }
        }

        public void downQuartersStar() {
            direction = true;
            shiftSelectedIndex(true);
        }

        public void upQuartersSlash() {
            direction = false;
            shiftSelectedIndex(false);
        }
    }
}
