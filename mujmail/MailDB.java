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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;
import mujmail.tasks.StoppableBackgroundTask;
import mujmail.threading.Algorithm;
import mujmail.ui.OKCancelDialog;
import mujmail.util.Callback;
import mujmail.util.Functions;
import mujmail.util.StartupModes;

/**
 * Provides functions for storing mail headers and low level functions
 * for storing fragments of body parts of mails used in class {@link RMSStorage}.
 */
public class MailDB {

    /** True if mail header are actually saved. */
    //private boolean savingHeader = false;
    /** The name of this source file */
    private static final String SOURCE_FILE = "MailDB";
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false; /// Debugging output for this file

    /** The number of headers to delete from headers database if database is full. */
    private static final int NUM_HEADERS_TO_DELETE_IF_DB_FULL = 5;
    /** The size of free space in header database when start to delete headers or notice user that the space is left. */
    private static final int FREE_SPACE_IN_HEADER_DB_WHEN_DELETE_HEADERS = 3000;

    private final static byte RUNMODE_LOAD = 1;
    private final static byte RUNMODE_DELETE_ALL_MAILS = 2;
    private final static byte RUNMODE_DELETE_MAIL = 3;

    public final static String safeModeDBFile = "safemodeStore";
    
    //Vector hdrRefer; // TODO (Betlista): I not sure if it is safe to store reference to storage
    //   for example: when there is new e-mail all mails have to be threaded
    //   and this operation recreates new storage (creates new instance), so this
    //   is the reason why it should NOT to store reference
    
    /** Database file name where mail are stored */
    private String dbName;

    /** The dbLoadingTask that loads the database. */
    private StoppableBackgroundTask dbLoadingTask = null;

    private boolean busy; /// Marks if running any dbLoadingTask (loading or deleteing, adding) or all dbLoadingTask had been finnished
    private Object notifier = new Object(); /// Object on which wait fo beeing notify
    
    public MailDB(String dbName) {
        busy = true;
        this.dbName = dbName;
    }

    /**
     * Loads headers in this database in new thread.
     * @param reportBox box which db become to
     */
    public void loadDB(PersistentBox reportBox) {
        if (DEBUG) { System.out.println("MailDB.loadDB - " + dbName); }
        // TODO: set max priority to this dbLoadingTask
        busy = true;
        dbLoadingTask = new MailDBTask(reportBox, RUNMODE_LOAD);
        dbLoadingTask.disableDisplayingProgress();
        dbLoadingTask.disableDisplayingUserActionRunnerUI();
        dbLoadingTask.start(reportBox);
    }

    public StoppableBackgroundTask getDBLoadingTask() {
        return dbLoadingTask;
    }

    /**
     * Delete all mails marked as to markAsDeleted.
     * @param reportBox box which db become to
     */
    public void deleteMails(PersistentBox reportBox) {
        // TODO: set max priority to this dbLoadingTask
        busy = true;
        StoppableBackgroundTask task = new MailDBTask(reportBox, RUNMODE_DELETE_ALL_MAILS);
        task.disableDisplayingProgress();
        task.start(reportBox);
    }

    /**
     * Deletes mail from database. Does not update the vector where the mail
     * is stored in TheBox.
     * @param header
     */
    public void deleteMail(MessageHeader header, PersistentBox reportBox) {
        if (this != header.getMailDB()) {
            throw new RuntimeException("Called on bad database");
        }

        // TODO: set max priority to this dbLoadingTask
        MailDBTask task = new MailDBTask(reportBox, RUNMODE_DELETE_MAIL);
        task.setMessageHeaderToDelete(header);
        task.disableDisplayingProgress();
        task.start(reportBox);
        //task.doWork();
    }

    public String getDBName() {
        return dbName;
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    /** @return Block and wait until any of running tasks end. 
     *    Returns immediatelly if no dbLoadingTask running. */
    public void waitForTaskEnd() {
        if (DEBUG) { System.out.println("DEBUG AccountSettings.waitForAccountsLoading .. in"); }
        try {
            synchronized(notifier) {
                if (!busy) return;
                notifier.wait();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        if (DEBUG) { System.out.println("DEBUG AccountSettings.waitForAccountsLoading .. out"); }
    }

    /**
     * This method clears all records in the database. 
     * By default removes all message bodies.
     * @param headers If set remove message headers too.
     */
    public void clearDb(boolean headers) throws MyException {
        boolean exception = false; // Mark if exception raise
        
        // Body db
        {
            try {
                RecordStore.deleteRecordStore(dbName);
            } catch (Exception ex) {
                exception = true;
                if (DEBUG) { 
                    System.out.println("DEBUG MailDB.clearDB - removing mail body problem from DB: " + dbName);
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }
        }
        // Headers db
        if (headers) {
            try {
                RecordStore.deleteRecordStore(dbName + "_H");
            } catch (Exception ex) {
                exception = true;
                if (DEBUG) { 
                    System.out.println("DEBUG MailDB.clearDB - removing mail headers db problem DB: " + dbName + "_H");
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }
        }
        // throw exception if problem
        if (exception) {
            throw new MyException(MyException.DB_CANNOT_CLEAR);
        }
    }
  
    /**
     * This method saves a bodypart of a message as a new record in a different RecordStore which name is determined TheBox.name
     * @param body - a String that is supposed to be save. Whole bodypart is stored as one String. The other information
     * about the bodypart are hold in a <code>Vector</code> and stored separatly.
     * Then we return an index which will be stored in bodyPart.recordID.
     * Synchronization is ensured by the rms system.
     * 
     * @see RMSStorage
     */
    int saveFragmentBodypartContent(String body, boolean safeMode) throws MyException {
        if (DEBUG) {
            System.out.println("MailDB.saveBodypartContent: " + body);
        }
        int index = -1;
        if (body.length() == 0) {
            body = "<no content>";
        } //we must do this, because sending a mail, without a body is supported		  
        //we will try to minimize using DB by recycling common record store safeModeDBFile for mails
        if (DEBUG) {
            System.out.println("Saving body part content");
        }
        RecordStore bodyRS = Functions.openRecordStore(safeMode?safeModeDBFile:dbName, true);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(byteStream);
            outputStream.writeUTF(body);
            outputStream.flush();
            index = bodyRS.addRecord(byteStream.toByteArray(), 0, byteStream.size());
            outputStream.close();
            byteStream.close();
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_SAVE_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }

        if (DEBUG) {
            System.out.println("Body part content saved");
        }
        return index;
    }

    /**
     * this method is for saving binary data
     * @param body
     * @param safeMode
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    int saveFragmentOfBodypartContent(byte[] body, boolean safeMode) throws MyException {
        int index = -1;
        if (DEBUG) {
            System.out.println("Saving body part content raw");
        }
        RecordStore bodyRS = Functions.openRecordStore( safeMode?safeModeDBFile:dbName, true);
        try {
            index = bodyRS.addRecord(body, 0, body.length);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_SAVE_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }

        if (DEBUG) {
            System.out.println("Body part content raw saved");
        }

        return index;
    }

    /**
     * By this method we get the real content of a body part in byte[]. Can be used by a class that displays mails
     * @param dbFileName
     * @param recordID
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    static byte[] loadFragmentBodypartContentRaw(String dbFileName, int recordID) throws MyException {
        byte[] body = null;

        if (DEBUG) {
            System.out.println("Loading body part content");
        }
        RecordStore bodyRS = Functions.openRecordStore(dbFileName, true);
        try {
            body = bodyRS.getRecord(recordID);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }

        if (DEBUG) {
            System.out.println("body part content loaded");
        }
        return body;
    }

    /**
     * By this method we get the real content a body part. Can be used by a class that displays mails
     * @param dbFileName
     * @param recordID
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    static String loadFragmentOfBodypartContent(String dbFileName, int recordID) throws MyException {
        String body = null;

        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - Loading body part content from database " + dbFileName);
        }
        RecordStore bodyRS = Functions.openRecordStore(dbFileName, true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - Database opened");
        }
        // getRecord returns null if the bodypart is empty
        try {
            byte[] data = new byte[bodyRS.getRecordSize(recordID)];
            bodyRS.getRecord(recordID, data, 0);
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));



            //body = new String(data, 0, data.length); // TODO: this does not read unicode (i.e. czech diacritics) correctly!!
            body = inputStream.readUTF(); //TODO: here is an EOF while reading HTML attachment
            if (DEBUG) {
                System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - loadBodypartContent body='" + body + "'");
            }
            inputStream.close();
            data = null;
        } catch (NullPointerException npex) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - null pointer exception");
            body = "";
        } catch (Exception ex) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - exception ");
            ex.printStackTrace();
            throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }

        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - body part content loaded");
        }
        return body;
    }

    private static final String NULL_STRING = "\\";

    /**
     * This method serves as translator for string values.
     * If value of string is null, it is replaced with "\\" (one backslash).
     * If value is not null and first character is backslash this backslash
     * is doubled otherwise original string is returned.
     * 
     * @param str string to be translated 
     * @return escaped string
     */
    public static String saveNullable( final String str ) {
        if ( str == null ) {
            return NULL_STRING;
        }
        final int length = str.length();
        if ( length == 0 ) {
            return "";
        } else {
              // if first char is backslash 
            if ( str.charAt( 0 ) == '\\' ) {
                  // add one backslash at the beginning 
                return "\\" + str;
            } else {
                  // return original String
                return str;
            }
        }
    }

    /**
     * Oposite for {@link #saveNullable(String)} method.
     * 
     */
    public static String loadNullable( final String str ) {
        if ( NULL_STRING.equals( str ) ) {
            return null;
        }
        final int length = str.length();
        if ( length == 0 ) {
            return "";
        } else {
            final char c1 = str.charAt( 0 );
            if ( c1 == '\\' ) {
                return str.substring( 1 );
            } else {
                return str;
            }
        }
    }

    /**
     * Handles the situation when message header cannot be saved.
     *
     * @throws MyException if the header was not saved and cannot be saved now
     * @throws Exception if there was exception while saving the header
     */
    private RecordStore handleProblemWithSavingHeader(final MessageHeader header, RecordStore headerRS) throws Exception {
        if (Settings.deleteMailsWhenHeaderDBIsFull) {
            // delete some and than try to save the header again
            headerRS.closeRecordStore();
            header.getBox().deleteOldestMails(NUM_HEADERS_TO_DELETE_IF_DB_FULL);
//            headerRS = Functions.openRecordStore(dbName + "_H", true);
//            saveHeader(headerRS, header);
//           
            throw new MyException(MyException.DB_CANNOT_SAVE_HEADER);
        } else {
            // ask user whether delete some mails.
            DeleteOldMails deleteOldMailsAndSaveHeader = new DeleteOldMails(headerRS, header);
            OKCancelDialog dialog = new OKCancelDialog("Not enough space in database", "There is not enough space to store header of this mail. Do you want to delete " + NUM_HEADERS_TO_DELETE_IF_DB_FULL + " oldest mails?", deleteOldMailsAndSaveHeader);
            dialog.showScreen(StartupModes.IN_NEW_THREAD);
            throw new MyException(MyException.DB_CANNOT_SAVE_HEADER);
        }

        //return headerRS;
    }

    private class DeleteOldMails implements Callback {
//        private final RecordStore headerRS;
        private final MessageHeader messageHeader;

        public DeleteOldMails(RecordStore headerRS, MessageHeader messageHeader) {
//            this.headerRS = headerRS;
            this.messageHeader = messageHeader;
        }

        public void callback(Object called, Object message) {
            messageHeader.getBox().deleteOldestMails(NUM_HEADERS_TO_DELETE_IF_DB_FULL);
//            try {
//                saveHeader(headerRS, messageHeader);
//            } catch (Exception ex2) {
//                throw new RuntimeException();
//            }
        }
        
    }
  
    /**
     * Saves the header of the message and header of all bodyparts to the RMS database.
     * Does not save the content of the message.
     * If the status of the message is header.DBStatus == MessageHeader.STORED
     * saves the header to existing record in the database (just updates it)
     * @param header the header of the message which will be saved
     * @return the record ID under which the header is saved
     * @throws mujmail.MyException
     */
    public int saveHeader(final MessageHeader header) throws MyException {
        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - saving header: " + header);
        }
        RecordStore headerRS = Functions.openRecordStore(dbName + "_H", true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - to database: " + this.dbName);
        }
        
        try {
            if (headerRS.getSizeAvailable() <= FREE_SPACE_IN_HEADER_DB_WHEN_DELETE_HEADERS) {
            //if ( header.getBox().storage.getSize() >= 3 ) {
                headerRS = handleProblemWithSavingHeader(header, headerRS);
            } else {
                saveHeader(headerRS, header);
            }
        } catch (MyException myex) {
            // cannot recover from this
            myex.printStackTrace();
            throw myex;
        } catch (Exception ex) {
            // try to recover
            ex.printStackTrace();
            try {
                headerRS = handleProblemWithSavingHeader(header, headerRS);
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        } finally {
            try {
              if (DEBUG) System.out.println( "DEBUG MailDB.saveHeader(MessageHeader) - Record store size = " + headerRS.getRecordSize(header.getRecordID()) );
            } catch (RecordStoreNotOpenException ex) {
                ex.printStackTrace();
            } catch (InvalidRecordIDException ex) {
                ex.printStackTrace();
            } catch (RecordStoreException ex) {
                ex.printStackTrace();
            }
            Functions.closeRecordStore(headerRS);
        }

        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - header saved");
        }
        return header.getRecordID();
    }
 
    //its static called by a class that displays mails, therefor its static
    //to let user markAsDeleted a attachment from a mail
    public static void deleteStorageContent(String dbFileName, int recordID) throws MyException {
        if (DEBUG) {
            System.out.println("Deleting body part");
        }
        RecordStore bodyRecordStore = Functions.openRecordStore(dbFileName, true);
        try {
            bodyRecordStore.deleteRecord(recordID);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_DEL_BODY);
        } finally {
            Functions.closeRecordStore(bodyRecordStore);
        }

        if (DEBUG) {
            System.out.println("Body part deleted");
        }
    }

    public static int bodyRecordSize(String dbFileName, int recordID) {
        RecordStore store = null;
        int size = -1;
        try {
            store = RecordStore.openRecordStore(dbFileName, true);
            size = store.getRecordSize(recordID);
        } catch (Exception ex) {
        }
        Functions.closeRecordStore(store);
        return size;
    }
    
    /**
     * Get space in bytes that database take place in persistent storage.
     * @return Size of database.
     */
    public int getOccupiedSpace() {
        RecordStore db = null;
        int size = 0;
        try {
            // Headers
            db = RecordStore.openRecordStore(dbName + "_H", true);
            size += db.getSize();
            db.closeRecordStore();

            // Bodies
            db = RecordStore.openRecordStore(dbName, true);
            size += db.getSize();
            db.closeRecordStore();
        } catch (Exception ex) {} // Non existent database 
        return size;
    }

    /**
     * This method loads a Vector of headers from the <code>RecordStore</code> with name nameRs.
     */
    private void loadHeaders(MailDBTask progress) throws MyException {
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadHeaders() - start - " + dbName);
        }
        RecordStore headerRS = Functions.openRecordStore(dbName + "_H", true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadHeaders() - Record box opened");
        }

        try {
            if (DEBUG) {
                System.out.println("DEBUG MailDB.loadHeaders() - number of records: " + headerRS.getNumRecords());
            }
            if (headerRS.getNumRecords() > 0) {
                RecordEnumeration enumeration = headerRS.enumerateRecords(null, null, false);
                byte[] data = new byte[250];
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
                int recordsNumber = enumeration.numRecords();
                byte bodyPartsCount;
                int id, sizeOfRecord;
                progress.setTitle(Lang.get(Lang.ALRT_LOADING) + progress.actionInvoker.getName());
                progress.updateProgress(recordsNumber, 0);
                //hdrRefer.ensureCapacity(numRcds);
                final Vector storedMessages = new Vector(recordsNumber);
                while (enumeration.hasNextElement()) {
                    try {
                        id = enumeration.nextRecordId();
                        sizeOfRecord = headerRS.getRecordSize(id);
                        if (sizeOfRecord > data.length) {
                            data = new byte[sizeOfRecord + 100];
                            inputStream = new DataInputStream(new ByteArrayInputStream(data));
                        }
                        headerRS.getRecord(id, data, 0);
                        inputStream.reset(); //read from the beginning
                        MessageHeader header = new MessageHeader(progress.actionInvoker); //create a new header // TODO ALF
                        //read the important information from the stream	
                        header.setRecordID(id);
                        header.setOrgLocation(inputStream.readChar());

                        header.setFrom(inputStream.readUTF());
                        header.setRecipients(inputStream.readUTF());
                        header.setSubject(inputStream.readUTF());
                        header.setBoundary(inputStream.readUTF());
                        header.setMessageID(inputStream.readUTF());
                        header.setIMAPFolder(inputStream.readUTF());
                        header.setAccountID(inputStream.readUTF());
                        header.messageFormat = inputStream.readByte();
                        header.readStatus = inputStream.readByte();
                        header.flagged = inputStream.readBoolean();
                        header.DBStatus = inputStream.readByte();
                        header.sendStatus = inputStream.readByte();
                        header.setSize(inputStream.readInt());
                        header.setTime(inputStream.readLong());
                        // fields for threading
                        header.setThreadingMessageID( loadNullable( inputStream.readUTF() ) );
                        header.setParentID( loadNullable( inputStream.readUTF() ) );
                        int parents = inputStream.readInt();
                        Vector parentIDs = new Vector();
                        for (int i = 0; i < parents; ++i) {
                            parentIDs.addElement(inputStream.readUTF());
                        }
                        header.setParentIDs(parentIDs);

                        bodyPartsCount = inputStream.readByte();
                        //now create a new bodypart and read its information
                        for (byte k = 0; k < bodyPartsCount; k++) {
                            BodyPart bp = new BodyPart(header);
                            bp.loadBodyPart(inputStream);
                            header.addBodyPart(bp);
                        }
                        //change the counter of unread mails of the Inbox and use boxes 
                        if (header.readStatus == MessageHeader.NOT_READ) {
                            MujMail.mujmail.getMailDBManager().changeUnreadMails(this, 1);
                        }

                        storedMessages.addElement(header); //add the header to the appropriate box
                        progress.incActual(1);
                        if (progress.stopped()) {
                            break;
                        }
                    } catch (Exception exp) { //usually its EOFException 
                        exp.printStackTrace();
                        //try another header
                    }
                }
                  if (Algorithm.DEBUG) System.out.println( "DEBUG MailDB.loadHeaders(MailDBTask) -  box name: " + progress.actionInvoker.getName() );
                progress.actionInvoker.setStorage(Algorithm.getAlgorithm().invoke(storedMessages));
                if (inputStream != null) {
                    inputStream.close();
                }
                data = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MyException(MyException.DB_CANNOT_LOAD_HEADERS);
        } catch (Error er) {
            er.printStackTrace();
            throw new MyException(MyException.SYS_OUT_OF_MEMORY);
        } finally {
            Functions.closeRecordStore(headerRS);
        }
        if (DEBUG) { System.out.println("DEBUG MailDB.loadHeaders() - end - " + dbName);
        }
    }

    private void _deleteMail(MailDBTask progress, MessageHeader header) throws MyException {
        for (byte j = (byte) (header.getBodyPartCount() - 1); j >= 0; --j) {
            BodyPart bp = (BodyPart) header.getBodyPart(j);
            bp.getStorage().deleteContent();
        }

        RecordStore headerRS = null;
        try {
            headerRS = RecordStore.openRecordStore(dbName + "_H", true);
        } catch (Exception ex) {
            throw new MyException(0);
        }

        if (headerRS != null) {
            try {
                headerRS.deleteRecord(header.getRecordID());
            } catch (Exception ex) {
                progress.actionInvoker.report("+" + Lang.get(Lang.ALRT_DELETING) + header.getSubject() + Lang.get(Lang.FAILED) + ": " + ex, SOURCE_FILE);
                throw new MyException(0);
            }
        }

        Functions.closeRecordStore(headerRS);
    }

    //batch mails deleting. 	
    private void _deleteMails(MailDBTask progress) throws MyException {
        progress.actionInvoker.report(Lang.get(Lang.ALRT_DELETING) + progress.actionInvoker.getName(), SOURCE_FILE);
        //lock the container as were gonna to modify it, so owningBox.repaint() and other things will not crash
        final IStorage storage = progress.actionInvoker.storage;
        synchronized (storage) {
            //this silly sorting prevents DB corruption as we markAsDeleted headers with higher record id first
            //it seems like emulator's bad implementation of RMS, not sure if it happens to normal phones as well
            // TODO (Betlista) - uncomment: the code bellow
            //Functions.sort(owningBox.storage, Functions.SRT_ORDER_INC, Functions.SRT_HDR_RECORD_ID);
            final int storageSize = storage.getSize();
            Vector/*<MessageHeader>*/ toDel = new Vector/*<MessageHeader>*/();

            // Collect emails marked as delete
            for (int i = 0; i < storageSize; ++i) {
                MessageHeader header = (MessageHeader) storage.getMessageAt(i);
                if (header.deleted) {
                    toDel.addElement(header);
                }
            }
            
            // Remove emails marked for deletion
            for(Enumeration e = toDel.elements(); e.hasMoreElements();) {
                MessageHeader mh = (MessageHeader)e.nextElement();
                // move to trash
                MujMail.mujmail.getTrash().storeToTrash(mh, Trash.TrashModes.CONDITIONALLY_MOVE_TO_TRASH);                
                // remove mail from database
                _deleteMail(progress, mh);
                  // remove mail from storage
                storage.removeMessage(mh);
                --progress.actionInvoker.deleted;
            }
            
            progress.actionInvoker.resort();// we sorted by recordID before the iteration			
            MujMail.mujmail.getTrash().resort();
        }
    }
    
    private void saveHeader(RecordStore headerRS, final MessageHeader header) throws RecordStoreNotOpenException, RecordStoreException, IOException, Exception {
        ByteArrayOutputStream byteStream;
        DataOutputStream outputStream;
        byteStream = new ByteArrayOutputStream();
        outputStream = new DataOutputStream(byteStream);
        //if it was stored then this is update procedure
        boolean update = header.DBStatus == MessageHeader.STORED;
        if (header.getOrgLocation() == 'X') {
            header.setOrgLocation(dbName.charAt(0));
        }
        outputStream.writeChar(header.getOrgLocation());
        outputStream.writeUTF(header.getFrom());
        outputStream.writeUTF(header.getRecipients());
        outputStream.writeUTF(header.getSubject());
        if (header.getBoundary() == null) {
            header.setBoundary(header.getMessageID());
        }
        outputStream.writeUTF(header.getBoundary());
        outputStream.writeUTF(header.getMessageID());
        outputStream.writeUTF(header.getIMAPFolder());
        outputStream.writeUTF(header.getAccountID());
        outputStream.writeByte(header.messageFormat);
        outputStream.writeByte(header.readStatus);
        outputStream.writeBoolean(header.flagged);
        header.DBStatus = MessageHeader.STORED;
        outputStream.writeByte(header.DBStatus);
        outputStream.writeByte(header.sendStatus);
        outputStream.writeInt(header.getSize());
        outputStream.writeLong(header.getTime());
        // fields for threading
        outputStream.writeUTF(saveNullable(header.getThreadingMessageID()));
        outputStream.writeUTF(saveNullable(header.getParentID()));
        Vector parentIDs = header.getParentIDs(); /*<String>*/
        int parentsCount = parentIDs.size();
        outputStream.writeInt(parentsCount);
        for (int i = 0; i < parentsCount; ++i) {
            // this should not be null
            outputStream.writeUTF(parentIDs.elementAt(i).toString());
        }
        // save also all bodypart headers
        byte size = header.getBodyPartCount();
        if (DEBUG) System.out.println("DEBUG - MailDB.saveHeader() - number of bodyparts " + size);
        outputStream.writeByte(size);
        for (byte j = 0; j < size; j++) {
            header.getBodyPart(j).saveBodyPart(outputStream);
        }
        outputStream.flush();
        /*this code doesnt always work, seems like bad implementation of RMS
        if (update)
        headerRS.setRecord(header.recordID, byteStream.toByteArray(), 0, byteStream.size() );
        else
        header.recordID = headerRS.addRecord( byteStream.toByteArray(), 0, byteStream.size() );
         */
        /*this seems working most of time:**/
        int oldID = header.getRecordID();
        header.setRecordID(headerRS.addRecord(byteStream.toByteArray(), 0, byteStream.size()));
        if (update) {
            try {
                headerRS.deleteRecord(oldID);
            } catch (Exception ignored) {
            }
        }
        outputStream.close();
        byteStream.close();
    }
    
    /**
     * Performs stoppable actions.
     */
    private class MailDBTask extends StoppableBackgroundTask {

        /** The box which invoke action. 
         * Note: Used for error reporting in background tasks.
         */
        private PersistentBox actionInvoker;

        /** Specific action to do */
        private byte runMode;

        /** Select mail to delete (in case single mail delete) */
        private MessageHeader messageHeaderToDelete;
        
        public MailDBTask(PersistentBox actionInvoker, byte runMode) {
            super("Database task " + actionInvoker.getDBFileName() + " MailDBTask " + runMode);
            this.actionInvoker = actionInvoker;
            this.runMode = runMode;
        }
        
        void setMessageHeaderToDelete(MessageHeader messageHeaderToDelete) {
            this.messageHeaderToDelete = messageHeaderToDelete;
        }
        
        
        public void doWork() {
            if (MailDB.DEBUG) System.out.println("Starting MailDBTask");
            busy = true;
            switch (runMode) {
                case RUNMODE_LOAD:
                    try {
                        loadHeaders(this);
                        if (MailDB.DEBUG) System.out.println("MailDBTask.doWork - loadHeaders finnished");
                        actionInvoker.report(Lang.get(Lang.ALRT_LOADING) + actionInvoker.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                    } catch (MyException ex) {
                        ex.printStackTrace();
                        actionInvoker.report(Lang.get(Lang.ALRT_LOADING) + actionInvoker.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    MujMail.mujmail.getMailDBManager().loadedDB(MailDB.this);
                    if (MailDB.DEBUG) System.out.println("MailDBTask.doWork - loadHeaders after loadedDB");
                    break;


                case RUNMODE_DELETE_ALL_MAILS:
                    if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - starting deleting all mails");
                    try {
                        _deleteMails(this);
                        if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - end deleting all mails");
                        actionInvoker.report(Lang.get(Lang.ALRT_DELETING) + actionInvoker.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                        if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - success reported");
                    } catch (MyException ex) {
                        actionInvoker.report(Lang.get(Lang.ALRT_DELETING) + actionInvoker.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    break;

                case RUNMODE_DELETE_MAIL:
                    try {
                        System.out.println("MailDB.doWork - starting to delete mail");
                        _deleteMail(this, messageHeaderToDelete);
                        System.out.println("MailDB.doWork - mail succesfully deleted");
                        actionInvoker.report(Lang.get(Lang.ALRT_DELETING) + actionInvoker.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                    } catch (MyException ex) {
                        actionInvoker.report(Lang.get(Lang.ALRT_DELETING) + actionInvoker.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    break;
            }
            synchronized(notifier) {
                busy = false;
                notifier.notifyAll();
            }
            //dbLoadingTask = null; // Loading task ended
            actionInvoker.repaint();
        }
    }
}
