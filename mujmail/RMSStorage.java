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

import mujmail.util.Functions;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

import javax.microedition.rms.RecordStore;
import mujmail.protocols.MailSender;
import mujmail.connections.ConnectionInterface;
import mujmail.ui.OKCancelDialog;
import mujmail.util.Callback;
import mujmail.util.StartupModes;

/**
 * Implementation of {@link ContentStorage} that stores the content
 * in internal java rms database.
 * 
 * Uses class {@link MailDB} to store and load fragments of content
 * to the RMS database. 
 * 
 * Stores the content in more fragments - database records - basically 
 * each calling of method {@link RMSStorage#addToContent(String, boolean)} 
 * or {@link #addToContentRaw(byte[], boolean)} stores added content to new
 * database record. Each database record identified by recordID and is 
 * represented by class {@link RMSStoragePart} that contains this ID.
 * Note, that to reduce the number of records per one body part content,
 * it should be used method {@link ContentStorage#addToContentBuffered(String)}
 * instead of method {@link ContentStorage#addToContent(String)}.
 * 
 * Getting of maximum content from this storage loads the content from
 * maximum number of database records that can fits into memory.
 * 
 * Contains a field for file name of database where the content
 * is stored. The name of the database is determined when the content is
 * stored.
 * Note that for the sake of optimalization, the name of the database is
 * common for all database records storing the content. That means that
 * it is not possible to store one content in multiple databases.
 * 
 * Provides memory management functions. If the space in database is left
 * and user has enabled automatic memory management of body part databases,
 * tries to delete the content of old body parts in order to free the
 * space in the database and than it try to save the content again. 
 * It deletes only such such number of body parts that should be 
 * sufficient to save actual content. 
 * If automatic memory management of body part databases is not enabled,
 * discards saving the content and it shows the dialog to user in that 
 * the user can choose deleting of body parts.
 * 
 * @see MailDB
 * 
 * @author David Hauzar
 */
public class RMSStorage extends ContentStorage {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    private RMSStorageParts rmsStorageParts;
    private String DBFileName = null; /** The database in that is stored the content. */
    private int numPartToGet = 0;
    /** The storage part that will be used for storing next data. It is 
     * pre-allocated in order to the OutOfMemory exception would not be thrown
     * while saving next content. */
    private RMSStoragePart nextRMSStoragePart;
    
    /**
     * Represents all parts of the content.
     */
    private static class RMSStorageParts {
        private final Vector storageParts = new Vector(1);
        private final RMSStorage contentStorage;

        /**
         * Creates the instance that represents all parts of content.
         * @param contentStorage the content storage that parts this object
         *  represents.
         */
        public RMSStorageParts(RMSStorage contentStorage) {
            this.contentStorage = contentStorage;
        }
        
        
        /**
         * Loads this storage parts from given input stream (rms database).
         * @param inputStream the input stream that contains this storage parts.
         * @throws java.lang.Exception
         */
        public void load(DataInputStream inputStream) throws Exception {
            // load content part
            int numberOfParts = inputStream.readInt();
            for (int i = 0; i < numberOfParts; i++) {
                RMSStoragePart storagePart = new RMSStoragePart(contentStorage);
                storagePart.load(inputStream);
                storageParts.addElement(storagePart);
            }
            
        }
        
        /**
         * Saves this storage parts to given output stream (typically rms database).
         * @param outputStream
         * @throws java.lang.Exception
         */
        public void save(DataOutputStream outputStream) throws Exception {
            outputStream.writeInt(storageParts.size());
            for (int i = 0; i < storageParts.size(); i++) {
                ((RMSStoragePart) storageParts.elementAt(i)).save(outputStream);
            }
        }
        
        public void addStoragePart(RMSStoragePart storagePart) {
            storageParts.addElement(storagePart);
        }
        
        /**
         * Delete the content of all storage parts and deletes also this storage
         * parts.
         */
        public void deleteContent() {
            for (int i = 0; i < storageParts.size(); i++) {
                ((RMSStoragePart) storageParts.elementAt(i)).deleteContent();
            }
            storageParts.removeAllElements();
        }
        
        /**
         * Gets the number of storage parts.
         * @return the number of storage parts.
         */
        public int getNumParts() {
            return storageParts.size();
        }
        
        /**
         * Gets given storage path.
         * @param i the number of storage part to get.
         * @return given storage path.
         */
        public RMSStoragePart getStoragePart(int i) {
            return (RMSStoragePart) storageParts.elementAt(i);
        }
        
    }
    
    /**
     * Represents one part of content stored in one record in RMS.
     */
    private static class RMSStoragePart {
        private int recordID = -1;
        private final RMSStorage contentStorage;

        public RMSStoragePart(RMSStorage contentStorage) {
            this.contentStorage = contentStorage;
        }
        
          // TODO (Betlista): is it used somewhere ?
        public RMSStoragePart(RMSStorage contentStorage, RMSStoragePart copy) {
            this.contentStorage = contentStorage;
            this.recordID = copy.recordID;
        }
        
        public int getRecordID() {
            return recordID;
        }
        
        private BodyPart getBodyPart() {
            return contentStorage.getBodyPart();
        }
        
        public String getContent() throws MyException {
            return MailDB.loadFragmentOfBodypartContent(contentStorage.DBFileName, getRecordID());
        }
        
        public byte[] getContentRaw() throws MyException {
           return MailDB.loadFragmentBodypartContentRaw(contentStorage.DBFileName, getRecordID());
        }

        /**
         * Counts the size of bodypart that is saveable to the database.
         * If there is not enough space in database, try to delete existing bodyparts
         * from database.
         * @param size
         * @param safeMode
         * @return
         */
        private int countSaveableSizeReleaseMemory(int size, boolean safeMode) throws Exception {
            int spaceLeft = safeMode ? Functions.spaceLeft(MailDB.safeModeDBFile) : Functions.spaceLeft(getBodyPart().getBox().getDBFileName());
            if (spaceLeft < size) { // we need more space
            //if (getBodyPart().getMessageHeader().getBox().getStorage().getSize() >= 3) {
                if (!safeMode) {
                        // try to delete bodyparts of existing mails to free space in db
                        handleProblemWithSavingBodyPart(size - spaceLeft);
                }

                if (spaceLeft < size) {

                    //but the bodypart's format doesnt support shortening - can not be viewed if trimmed
                    if (!MessageHeader.canBePartiallySaved(getBodyPart()) || spaceLeft <= 0) {
                        return -1;
                    }

                    return spaceLeft;
                }
            }

            return size;

        }

        /**
         * Displays dialog askingto delete oldest bodyparts from database.
         */
        private static class DeleteOldestBodyparts implements Callback {
            private final MessageHeader savingMessage;
            private final long sizeToRelease;

            public DeleteOldestBodyparts(MessageHeader savingMessage, long sizeToRelease) {
                this.savingMessage = savingMessage;
                this.sizeToRelease = sizeToRelease;
            }

            public void callback(Object called, Object message) {
                savingMessage.getBox().deleteOldestBodyParts(sizeToRelease, savingMessage);
            }
            
        }

        /**
         * Handles the problem with saving bodypart.
         *
         * This means that if Settings.deleteMailsBodyWhenBodyDBIsFull is true
         * it deletes the content of oldest bodyparts to release memory and than
         * tryes to save the bodypart again.
         * If Settings.deleteMailsBodyWhenBodyDBIsFull is false, it displays the
         * dialog to user and asks him if he want to delete headers. Than it
         * cancels storing bodypart. That means that in this case it will be
         * empty. TODO: it only marks bodypart as empty. Should it also delete it?
         *
         * @param sizeToRelease
         * @return the amount of released space in database.
         * @throws java.lang.Exception if it is not possible to save this
         *  bodypart now. That means it makes no sence to repeate the action now.
         */
        private long handleProblemWithSavingBodyPart(long sizeToRelease) throws Exception {
            if (Settings.deleteMailsBodyWhenBodyDBIsFull) {
                return getBodyPart().getMessageHeader().getBox().deleteOldestBodyParts(sizeToRelease, getBodyPart().getMessageHeader());
            } else {
                OKCancelDialog dialog = new OKCancelDialog("Database is full",
                        "The database is full. Do you want to delete body of oldest mails to release memory in database to be able to fit this bodypart?",
                        new DeleteOldestBodyparts(getBodyPart().getMessageHeader(), sizeToRelease));
                dialog.showScreen(StartupModes.IN_NEW_THREAD);
                throw new MyException(MyException.DB_CANNOT_SAVE_BODY);
            }
        }
        
        /**
         * Saves given content to this storage part.
         * If there is not enough space in database, shortens the content.
         * @param content the content to be saved.
         * @param safeMode
         * @return the size of content that was really saved.
         */
        protected long saveContent(String content, boolean safeMode) throws Exception {
            int saveable = countSaveableSizeReleaseMemory(Functions.getStringByteSize(content), safeMode);

            if (saveable < Functions.getStringByteSize(content)) {
                // TODO: there can occur OutOfMemoryException: handle better
                content = content.substring(0, saveable);
            }

            try {
                //throw new MyException(0);
                saveContentToDB(content, safeMode);
            } catch (MyException e) {
                handleProblemWithSavingBodyPart(saveable);
                saveContentToDB(content, safeMode);
            }

            return saveable;
        }
        
        /**
         * Saves given content to this storage part.
         * If there is not enough space in database, shortens the content.
         * TODO: shortening content in case of binary data probably makes no sense!!
         * @param content the content to be saved.
         * @param safeMode
         * @return the size of content that was really saved.
         */
        protected long saveContentRaw(byte[] content, boolean safeMode) throws Exception {
            int saveable = countSaveableSizeReleaseMemory(content.length, safeMode);

            byte [] decodedBodyBytes = null;
            if (saveable < content.length) {
                // TODO: there can occur OutOfMemoryException!!!! Handle it better.
                decodedBodyBytes = new byte[saveable];
                System.arraycopy(content, 0, decodedBodyBytes, 0, saveable);
            }

            try {
                saveContentToDB(decodedBodyBytes, content, safeMode);
            } catch (MyException e) {
                handleProblemWithSavingBodyPart(saveable);
                saveContentToDB(decodedBodyBytes, content, safeMode);
            }

            return saveable;
        }
        
        /**
         * Deletes the conten of this storage.
         */
        public void deleteContent() {
            try {
                MailDB.deleteStorageContent(contentStorage.DBFileName, recordID);
            } catch (MyException ex) {
                ex.printStackTrace();
            }
        }
        
        /**
         * Load this object from given input stream (typically rms database).
         * @param inputStream input stream that contains data of this object.
         * @throws java.lang.Exception
         */
        public void load(DataInputStream inputStream) throws Exception {
            // load content part
            recordID = inputStream.readInt();            
            
        }
        
        /**
         * Saves this object to given output stream (typycally rms database).
         * @param outputStream output stream to that save the data of this object.
         * @throws java.lang.Exception
         */
        public void save(DataOutputStream outputStream) throws Exception {
            outputStream.writeInt(getRecordID());
        }

        private void saveContentToDB(String content, boolean safeMode) throws MyException {
            recordID = getBodyPart().getBox().getMailDB().saveFragmentBodypartContent(content, safeMode);
            updateDBFileName(safeMode);
            
        }

        /**
         * Updates the database file name. The name of database file name 
         * must be the same for all bodyparts.
         * @param safeMode
         */
        private void updateDBFileName(boolean safeMode) {
            String newDBFileName = safeMode ? MailDB.safeModeDBFile : getBodyPart().getBox().getDBFileName();
            if (contentStorage.DBFileName != null) {
                // check if the name of the database is correct: the same
                if (!contentStorage.DBFileName.equals(newDBFileName)) {
                    throw new RuntimeException("The content of one bodypart must be saved in the same database.");
                }
            } else {
                // set the database name
                contentStorage.DBFileName = newDBFileName;
            }
        }

        private void saveContentToDB(byte[] decodedBodyBytes, byte[] content, boolean safeMode) throws MyException {
            recordID = getBodyPart().getBox().getMailDB().saveFragmentOfBodypartContent(decodedBodyBytes != null ? decodedBodyBytes : content, safeMode);
            
            updateDBFileName(safeMode);
        }
        
        
        
    }

    public RMSStorage(BodyPart bodyPart) {
        super(bodyPart); 
        rmsStorageParts = new RMSStorageParts(this);
    }

    /**
     * Initialize class by copying another instance.
     * Note that if the mode is SHALLOW_COPY it the body part which content
     * will is stored in this ContentStorage should be in the same box as original
     * storage.
     * @param bp the body part which content is stored in this storage
     * @param copy AttachmentPart instance to copy
     * @param copyMode defines copying mode
     */
    private RMSStorage(BodyPart bp, RMSStorage content, CopyingModes copyMode) {
        super(bp);

        if (copyMode == CopyingModes.NO_COPY) {
            // the instance is yet created
            rmsStorageParts = new RMSStorageParts(this);
            return;
        }

        if (copyMode == CopyingModes.SHALLOW_COPY) {
            // TODO: copy rms storage parts?
            this.DBFileName = content.DBFileName;
            this.rmsStorageParts = content.rmsStorageParts;
            this.setSize(content.getSize());

            return;
        }

        if (copyMode == CopyingModes.DEEP_COPY) {
            rmsStorageParts = new RMSStorageParts(this);
            addContent(content, false);

            return;
        }
    }

    public boolean willReturnFirstContent() {
        return (numPartToGet == 0);
    }

    /**
     * Tryes to get maximum content parts beginning with content part with number
     * numPartToGet.
     * Gets content parts until it is reached the end of this content or it is
     * not enough memory.
     * @return the stirng with content.
     */
    private String getMaximumContent() throws Throwable {
        StringBuffer sb = new StringBuffer();
        String lastAddedContent = null;
        for (int i = numPartToGet; i < rmsStorageParts.getNumParts(); i++) {
            try {
                lastAddedContent = rmsStorageParts.getStoragePart(i).getContent();
                sb.append(lastAddedContent);
                incNumPartToGet();
            } catch (Throwable exception) {
                // TODO: show message to user: Load body part failed or something
                // like that?
                rewindStringbuffer(sb, lastAddedContent);
                throw exception;
            }
        }

        return sb.toString();
    }
    
    /**
     * Counterpart of getMaximumContent for binary data.
     * @return the stirng with content
     * @see getMaximumContent
     */
    private byte[] getMaximumContentRaw() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        System.out.println("DEBUG - RMSStorage.getMaximumContentRaw: rmsStorageParts.getNumParts() = " + rmsStorageParts.getNumParts());
        for (int i = numPartToGet; i < rmsStorageParts.getNumParts(); i++) {
            try {
                byte[] lastAddedContent = rmsStorageParts.getStoragePart(i).getContentRaw();
                bos.write(lastAddedContent);
                incNumPartToGet();
            } catch (Throwable exception) {
                exception.printStackTrace();
                break;
            }
        }

        return bos.toByteArray();
    }
    
    /**
     * Increases number of part to get. Called when part of the content is readed.
     * If it is readed the last part of the content, next part of the content
     * to get will be the first one.
     */
    private void incNumPartToGet() {
        if (numPartToGet + 1 < rmsStorageParts.getNumParts()) {
            numPartToGet++;
        } else {
            numPartToGet = 0;
        }
    }
    
    private void decNumPartToGet() {
        if (numPartToGet > 0) numPartToGet--;
    }

    public void resetToFirstContent() {
        numPartToGet = 0;
    }
    
    
    

    public StorageTypes getStorageType() {
        return StorageTypes.RMS_STORAGE;
    }

    public ContentStorage copy(BodyPart bp, CopyingModes copyMode) {
        if (!checkCopy(bp, copyMode)) {
            return null;
        }

        return new RMSStorage(bp, this, copyMode);
    }                                                                            

    public void saveStorageHeader(DataOutputStream outputStream) throws Exception {
        super.saveStorageHeader(outputStream);
        
        outputStream.writeUTF(DBFileName);
        rmsStorageParts.save(outputStream);
    }

    public void loadStorage(DataInputStream inputStream) throws Exception {
        super.loadStorage(inputStream);

        DBFileName = inputStream.readUTF();
        rmsStorageParts.load(inputStream);
    }

    public String sendContentToConnection(ConnectionInterface connection, 
            MailSender.SendingModes sendingMode,
            boolean returnSendedData) throws Throwable {
      
        String senderData = "";
        do {
            senderData = conditionallyAppend(senderData, 
                    sendRMSBodyPartData(connection, getContent(), sendingMode), 
                    returnSendedData);
        } while (!willReturnFirstContent());
        
        return senderData;
    }
    
    /**
     * Sends data from RMS storage to connection.
     * 
     * @param rmsData the data from rms storage (from method getContent() of RMSStorage)
     * @param capturedMailText the string that was already sent
     * @param sendingMode used to encode the string before sending
     * @return the string that was sent
     */
    private String sendRMSBodyPartData(ConnectionInterface connection, String rmsData, 
            MailSender.SendingModes sendingMode) throws Exception {
        String body = Functions.toCRLF(
                rmsData +
                (Settings.signature.length() != 0 ? "\r\n--\r\n" + Settings.signature : ""));
        // Sending body
        String tmpMailLine = sendingMode.toEncoding(body, false);
        connection.sendCRLF(tmpMailLine);
        
        return MailSender.captureStrCRLF("", tmpMailLine);
    }

    protected void preallocateToNextSaving() {
        // TODO: warning: addToContent / addToContentRaw still allocates something
        // see this methods
        nextRMSStoragePart = new RMSStoragePart(this);
    }

    protected boolean preallocatedToNextSaving() {
        return nextRMSStoragePart != null;
    }

    protected long addToContent(String content, boolean safeMode) throws Exception {
        RMSStoragePart rmsStoragePart = nextRMSStoragePart;
        nextRMSStoragePart = null;
        long wasSaved = rmsStoragePart.saveContent(content, safeMode);
        rmsStorageParts.addStoragePart(rmsStoragePart);
        return wasSaved;
    }

    protected long addToContentRaw(byte[] content, boolean safeMode) throws Exception {
        RMSStoragePart rmsStoragePart = nextRMSStoragePart;
        nextRMSStoragePart = null;
        long wasSaved = rmsStoragePart.saveContentRaw(content, safeMode);
        rmsStorageParts.addStoragePart(rmsStoragePart);

        return wasSaved;
    }

    public synchronized String getNotRawContent() throws Throwable {
        //if (getSize() == 0) {
            //throw new Exception("There is no content in this storage.");
        //}

        String returnString = null;
        try {
            if (DEBUG) System.out.println("DEBUG RMSStorage.getContent - geting the content.");
            returnString = getMaximumContent();
            if (DEBUG) System.out.println("DEBUG RMSStorage.getContent - the content getted.");
        } catch (Throwable ex) {
            if (DEBUG) System.out.println("DEBUG RMSStorage.getContent - the exception while getting content.");
            ex.printStackTrace();
            numPartToGet = 0;
            throw ex;
        }
        
        return returnString;
    }
    
    
    

    public synchronized byte[] getContentRaw() throws MyException {
        byte[] returnData = null;
        try {
            returnData = getMaximumContentRaw();
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.out.println("getContentRaw: exception!!!");
            numPartToGet = 0;
        }
        return returnData;
    }

    protected void deleteContentFromStorage() {
        rmsStorageParts.deleteContent();
    }

    private void rewindStringbuffer(StringBuffer sb, String lastAddedContent) {
        if (lastAddedContent != null) {
            sb.delete(sb.length() - lastAddedContent.length(), sb.length());
        }
        decNumPartToGet();
    }
}
