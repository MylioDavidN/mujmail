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

import mujmail.threading.ThreadedEmails;
import mujmail.util.Functions;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import mujmail.ordering.Ordering;
import mujmail.ordering.comparator.OriginatorDateTimeComparator;
//#ifdef MUJMAIL_SEARCH
import mujmail.util.SaveableBooleanValue;
import mujmail.util.PersistentValueReminder;
//#endif
/**
 * TheBox thats messages are persistent - stored in RMS.
 * It contains pointer to the database where are stored the headers of mails from
 * this box. So, there is one database for each Box.
 * 
 * Each persistent box is connected to two databases (DBFile, mailDB). The first one 
 * contains the headers of mails from given box, the second one bodyparts of 
 * the mail.
 * 
 * Each mail is stored in one persistent box.
 * 
 * @author David Hauzar
 */
//#ifdef MUJMAIL_SEARCH
public class PersistentBox extends TheBox implements SaveableBooleanValue {
//#else
//# public class PersistentBox extends TheBox {
//#endif   
    /** The name of this source file */
    private static final String SOURCE_FILE = "PersistentBox";
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;
    /** Vector of all searchable boxes. */
    private final static Vector persistentBoxes = new Vector();

    //#ifdef MUJMAIL_SEARCH
    private final PersistentValueReminder.PersistentBooleanValueReminder wasSelectedReminder;
    //#endif

    /**
     * The name of database file in which are stored headers of mails from this box
     */
    protected final String DBFile;
    /**
     * Represents database file in which are stored headers of mails from this box
     */
    protected MailDB mailDB;
    
    /**
     * Creates persistent box.
     * 
     * @param DBFile the identifier of RMS database where the mails of this box
     *  will be stored.
     * @param mMail the main object in the application
     * @param name the name of the box
     */
    public PersistentBox(String DBFile, MujMail mMail, String name) {
        super(mMail, name);

        this.DBFile = DBFile;
        
        persistentBoxes.addElement(this);
        mailDB = mujMail.getMailDBManager().getMailDB(this, DBFile);
        
        // mailDB.loadDB(this); // Race condition this have to be called from MujMail class
        //#ifdef MUJMAIL_SEARCH
        wasSelectedReminder = new PersistentValueReminder.PersistentBooleanValueReminder(PersistentValueReminder.DB_PREFIX + DBFile);
        //#endif

    }
    
    /**
     * Gets a copy of all boxes that can be used in the search.
     * @return all boxes that can be used in the search.
     */
    public static Vector getPersistentBoxes() {
        return Functions.copyVector( persistentBoxes );
    }

    /**
     * Do the physical work of deleting marked mails from box and database.
     * Called by deleteMarkedFromBoxAndDB().
     */
    protected void doDeleteMarkedFromBoxAndDB() {
        mailDB.deleteMails(this);
    }

    protected void paintIsBusy() {
        mailDB.getDBLoadingTask().showProgressIfRunning();
    }


    
    /**
     * Indicates whether there proceeds some action beyond the mails in this
     * box.
     * @return true if there proceeds some action beyond the mails int this
     * box
     */
    protected boolean isBusy() {
        return mailDB.isBusy();
    }

    /**
     * Copyes bodyparts of oldHeader to newHeader and stores it.
     * @param oldHeader the header which bodyparts will be copyed
     * @param newHeader the header to that bodyparts will be copyed
     */
    private void copyAndStoreBodyParts(MessageHeader oldHeader, MessageHeader newHeader) {
        for (byte i = 0; i <= oldHeader.getBodyPartCount() - 1; ++i) {
            BodyPart bp = new BodyPart(newHeader, oldHeader.getBodyPart(i), ContentStorage.CopyingModes.DEEP_COPY);
            //make a copy
            bp.setBodyState(oldHeader.getBodyPart(i).getBodyState());
            //maybe it is also partial as it's copy
            //maybe bodypart's content was not saved,
            //at least we'll save the bodypart's header
            newHeader.addBodyPart(bp);
        }
    }

    /**
     * 
     * @param messageHeader
     * @return
     */
    private MessageHeader copyMessageHeader(final MessageHeader messageHeader) {
        final MessageHeader message = new MessageHeader(this, messageHeader);
          // make a copy
        message.DBStatus = MessageHeader.NOT_STORED;
          // because our new header is really not stored in DB yet
        if (message.deleted) {
            message.deleted = false;
        }
          // to prevent being deleted
        if (this != getMujMail().getTrash()) {
            message.setOrgLocation( DBFile.charAt(0) );
        }
          // change the original location of the header to this box DBFile
          if (DEBUG) System.out.println("DEBUG PersistentBox.copyMessageHeader(MessageHeader) - adding message to the storage");
        if ( this instanceof InBox ) { // threading is active only in InBox
            storage.addMessage(message);
        } else {
            if ( storage instanceof ThreadedEmails ) {
                ThreadedEmails te = (ThreadedEmails)storage;
                te.addRoot( message );
            }
        }
          if (DEBUG) System.out.println("DEBUG PersistentBox.copyMessageHeader(MessageHeader) - message added");
        return message;
    }

    /**
     * Stores the copy of given mail with bodyparts mail to the DB of this box 
     * and to the container of this box.
     * 
     * @param header the header of mail to be copied and stored
     * @return the header of the mail that was stored - the copy of the mail
     *  given in parameter
     */
    public MessageHeader storeMail(MessageHeader header) {
        MessageHeader h = null;
        try {
              // TODO (Betlista): why there have to be copy returned ?
              if (DEBUG) System.out.println("DEBUG PersistentBox.storeMail(MessageHeader) - storing mail");
            h = copyMessageHeader(header);
            copyAndStoreBodyParts(header, h);
            h.saveHeader();
              if (DEBUG) System.out.println("DEBUG PersistentBox.storeMail(MessageHeader) - mail stored");
        } catch (MyException ex) {
            //something went wrong, markAsDeleted all saved data
            report(ex.getDetails() + header.getSubject(), SOURCE_FILE);
            for (int i = header.getBodyPartCount() - 1; i >= 0; --i) {
                ((BodyPart) header.getBodyPart(i)).getStorage().deleteContent();
            }
            storage.removeMessage(h);
            //remove the added header by copyMessageHeader()
            return null;
        }
        return h;
    }
    
    /**
     * Delete all mails from database of this persistent box.
     */
    protected void deleteAllMailsFromDB() {
        try {
            mailDB.clearDb(true);
        } catch (MyException ex) {
            if (DEBUG) {
                System.out.println("Clearing DB failed");
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Deletes oldest mails from this box and from database. Stops deleting if
     * the number of deleted mails is equal to numHeadersToDelete or if it was
     * deleted all mails in this box.
     *
     * @param numHeadersToDelete the number of headers to be deleted.
     * @return the number of headers that were deleted
     */
    public int deleteOldestMails(int numMailsToDelete) {
        if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldest mails - start.");
        synchronized (storage) {
            storage.sort(new OriginatorDateTimeComparator(Ordering.NATURAL));
        }
        
        numMailsToDelete = Math.min(numMailsToDelete, storage.getSize());
        if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldest mails - there should be " + numMailsToDelete + " mails deleted.");
        for (int i = 0; i < numMailsToDelete; i++) {
            if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldest mails - deleting mail.");
            storage.getMessageAt(i).deleteFromDBAndBox(this, Trash.TrashModes.NOT_MOVE_TO_TRASH);
        }
        
        resort();

        return numMailsToDelete;
    }

    /**
     * Deletes bodyparts of oldest mails in this box. Stops deleting if the size
     * of deleted bodyparts is bigger or equal than <code>memoryToBeReleased</code>
     * or if all bodyparts of all mails were deleted.
     *
     * @param memoryToBeReleased the amount of memory to be released in database
     *  by deleting of bodyparts.
     * @param thisMail the mail that's bodyparts should not be deleted.
     * @return the size of all deleted bodyparts.
     */
    public long deleteOldestBodyParts(long memoryToBeReleased, MessageHeader thisMail) {
        synchronized (storage) {
            storage.sort(new OriginatorDateTimeComparator(Ordering.NATURAL));
        }

        if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldestBodyParts - starting to delete bodyparts of max size " + memoryToBeReleased);
        long toDeleteAct = memoryToBeReleased;
        for (int i = 0; i < storage.getSize(); i++) {
            MessageHeader message = storage.getMessageAt(i);
            if (message == thisMail) continue;
            if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldestBodyParts - deleting bodyparts of message " + message);
            toDeleteAct -= deleteBodyPartsOfMessage(message);
            if (toDeleteAct <= 0) break;
        }

        resort();

        long sizeOfAllDeleted = memoryToBeReleased - toDeleteAct;
        if (DEBUG) System.out.println("DEBUG PersistentBox.deleteOldestBodyParts - the size of deleted bodyparts " + sizeOfAllDeleted);
        return sizeOfAllDeleted;
    }
    
    //#ifdef MUJMAIL_SEARCH
    public boolean loadBoolean() {
        return wasSelectedReminder.loadBoolean();
    }
    
    public void saveBoolean(boolean isSelected) {
        wasSelectedReminder.saveBoolean(isSelected);
    }
    //#endif

    public void showBox() {
        // Show us only if paint can show progress with loading DB or inbox content
        if (getMailDB().getDBLoadingTask() != null) {
            super.showBox();
        }
    }
    
    /**
     * Called from MailDB after loading db (loadDB) is done
     * Note: Specialized in childs
     */
    public void loadedDB() {
        resort(); //its needed to resort the box according to the settings
    }

    /**
     * Represents database file in which are stored headers of mails from this box
     */
    public MailDB getMailDB() {
        return MujMail.mujmail.getMailDBManager().getMailDB(this, mailDB.getDBName());
    }
    
    /**
     * @return Name of database where box data are stored
     */
    public String getDBFileName() {
        return DBFile;
    }

    public void commandAction(Command c, Displayable d) {
        super.commandAction(c, d);
    }
    
    /**
     * @return Size of databases that stores data of folder
     */
    public int getOccupiedSpace() {
        return mailDB.getOccupiedSpace();
    }

    /**
     * Deletes body parts of given message.
     *
     * @param message the message that's bodyparts will be deleted.
     * @return the size of bodyparts that were deleted.
     */
    private long deleteBodyPartsOfMessage(MessageHeader message) {
        if (message.getBodyPartCount() > 0) {
            for (int j = 0; j < message.getBodyPartCount(); j++) {
                BodyPart bodyPart = message.getBodyPart(j);
                deleted += bodyPart.getSize();
                bodyPart.getStorage().deleteContent();
            }
            // TODO: this would remove bodyparts only non-persistently!!!
            // message.deleteAllBodyParts();
        }

        return deleted;
    }

}
