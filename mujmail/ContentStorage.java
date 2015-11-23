/*
MujMail - Simple mail client for J2ME
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
along with this program; if not, bufferContent to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package mujmail;

import mujmail.util.Decode;
import mujmail.util.OutputBuffer;
import mujmail.protocols.MailSender;
import mujmail.connections.ConnectionInterface;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import mujmail.util.Functions;

/**
 * Persistently stores the content of body part. Part of 
 * <code>BodyPart</code>.
 *  
 * It is possible to incrementally add the content to existing
 * content without keeping existent content in heap memory. This is 
 * important for example when reading the content from SMTP connection.
 * 
 * Provides methods for adding content to the storage, for
 * buffered adding content to the storage - this case the content
 * is stored when more content for storing is collected - as well
 * as methods for reading or deleting the content.
 * Than provides information about the content contained in this
 * storage.   
 * 
 * Object of this type is contained in object {@link BodyPart}.
 * 
 * This object is stored persistently in RMS database. It's loading
 * and saving does object of instance {@link BodyPart} that
 * contains this storage.
 * 
 * @author David Hauzar
 */
public abstract class ContentStorage {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public static final String SOURCE_FILE = "ContentStorage";
    //private static final long MAX_SIZE_OF_BODYPART = Long.MAX_VALUE;

    private long size = 0;
    private BodyPart bodyPart;
    private final BufferedContentAdder buffer = new BufferedContentAdder();
    

    /**
     * 
     * @param bodyPart the body part which content is stored in this storage
     */
    protected ContentStorage(BodyPart bodyPart){
        this.bodyPart = bodyPart;
    }
    
    /**
     * Gets the name of the bodypart which content this storage holds.
     * @return the name of the bodypart that content this storage holds.
     */
    protected String getName() {
        return bodyPart.getHeader().getName();
    }

    /**
     * 
     * @param bodyPart the body part which content is stored in this storage
     * @param size the size of the content stored in the storage
     */
    protected ContentStorage(BodyPart bodyPart, long size) {
        this.bodyPart = bodyPart;
        this.size = size;
    }
    
    /**
     * It is important to lower the likelihood of throwing OutOfMemory exception
     * while calling method addToContent or addToContentRaw. This means that
     * allocating objects in these methods is undesirable. This method should
     * preallocate objects needed in addToContent or addToContentRaw.
     * 
     * It is ensured that this method will be called after saving the content 
     * to the storage and releasing the data kept in the buffer so it is
     * unlikely to OutOfMemoryException to be thrown there.
     */
    protected abstract void preallocateToNextSaving();
    
    /**
     * 
     * @return true if the content is yet preallocated to next saving.
     */
    protected abstract boolean preallocatedToNextSaving();
    
    /**
     * Ensures that te objects are really preallocated before calling addToContent
     * or addToContentRaw.
     */
    private void ensurePrealocating() {
        if (!preallocatedToNextSaving()) {
            preallocateToNextSaving();
        }
    }

    /**
     * Initialize class by copying another instance.
     * If copyMode is DEEP_COPY copies the content.
     * Note that if the mode is SHALLOW_COPY it the body part which content
     * will is stored in this ContentStorage should be in the same box as original
     * storage.
     * @param bp the body part which content is stored in this storage
     * @param copy AttachmentPart instance to copy
     * @param copyMode defines copying mode
     */
    protected ContentStorage(BodyPart bodyPart, ContentStorage copy, CopyingModes copyMode) {
        this.bodyPart = bodyPart;

        if (copyMode == CopyingModes.NO_COPY) {
            // the instance is yet created
            return;
        }

        if (copyMode == CopyingModes.DEEP_COPY) {
            addContent(copy, false);

            return;
        }
    }
    
    /**
     * Creates new instance of storage of type identified with given
     * number.
     * @param storageTypeNumber the number which identify the storage type
     *  of which we want to create an instance
     * @return the storage of given type
     */
    public static ContentStorage createStorageInstance(BodyPart bodyPart, byte storageTypeNumber) {
        //#ifdef MUJMAIL_FS
        if (storageTypeNumber == StorageTypes.FS_STORAGE.getStorageTypeNumber()) {
            return new FSStorage(bodyPart);
        }
        //#endif

        if (storageTypeNumber == StorageTypes.RMS_STORAGE.getStorageTypeNumber()) {
            return new RMSStorage(bodyPart);
        }

        throw new RuntimeException("not implemented storage type");
    }

    /**
     * Gets the body part which content is stored in the storage.
     * @return
     */
    protected BodyPart getBodyPart() { return bodyPart; }

    /**
     * Returns the storage type of given storage
     * @return
     */
    public abstract StorageTypes getStorageType();

    /**
     * Returns new instance of storage which is copy of this instance
     * of the storage.
     * @return new instance of storage which is copy of this instance
     */
    public abstract ContentStorage copy(BodyPart bp, CopyingModes copyMode);

    /**
     * Checks whether it is allowed to make a copy of this instance with given
     * copy mode.
     * @param bp
     * @param copyMode
     */
    protected boolean checkCopy(BodyPart bp, CopyingModes copyMode) {
        if (copyMode == CopyingModes.SHALLOW_COPY && bp.getBox() != getBodyPart().getBox()) {
            return false;
        }

        return true;
    }

    /**
     * Loads information about this storage from input stream (of RMS database).
     * Does not load the content of storage, loads only information about
     * the storage.
     * @param inputStream the input stream in which are stored information
     *  about this storage.
     * @throws java.lang.Exception can occur while reading inputStream
     */
    public void loadStorage(DataInputStream inputStream) throws Exception {
        setSize( inputStream.readLong() );
    }

    /**
     * Saves information about this storage to output stream (RMS database)
     * Does not save the content of the storage, saves only information about
     * this storage.
     * @param outputStream the output stream to which the information about
     *  this body part will be saved
     * @throws java.lang.Exception can occur while writing to the outputStream
     */
    public void saveStorageHeader(DataOutputStream outputStream) throws Exception {
        outputStream.writeLong(getSize());
    }

    /**
     * Deletes the content of this bodypart.
     */
    public void deleteContent() {
        setSize(0);
        buffer.clearCache();
        if (!bodyPart.convertedContentMode()) { 
            bodyPart.setBodyState(BodyPart.BS_EMPTY);
        }
        
        deleteContentFromStorage();
    }
    
    /**
     * Deletes the content from storage. Called from method 
     * {@link #deleteContent()}}.
     */
    protected abstract void deleteContentFromStorage();

    public void addContent(ContentStorage copy, boolean safeMode) {
        // TODO: redefine this method in class RMS storage
        // in case of RMS store it can be make too big records because 
        // getContent() or getContentRaw() returns not content from one record 
        // but from the maximum number of records that can be in RAM memory
        try {
            if (copy.isContentRaw()) {
               addToContentEnsurePreallocating(copy.getContentRaw(), safeMode);
               while (!copy.willReturnFirstContent()) {
                   addToContentEnsurePreallocating(copy.getContentRaw(), safeMode);
               }
            } else {
                addToContentEnsurePreallocating(copy.getContent(), safeMode);
                while (!copy.willReturnFirstContent()) {
                   addToContentEnsurePreallocating(copy.getContent(), safeMode);
                }
            }
        } catch (Throwable e) {
            getBodyPart().setBodyState(BodyPart.BS_EMPTY);
            size = 0;
            getBodyPart().getBox().report("+" + e.getMessage() + getBodyPart().getMessageHeader().getSubject(), SOURCE_FILE); //dont notice the user about the error
        }
    }
    
    protected String conditionallyAppend(String appendTo, String append, boolean shouldAppend) {
        if (shouldAppend) {
            return appendTo + append;
        } else {
            return appendTo;
        }
    }
    
    /**
     * According to the encoding and charset of the bodypart that content this
     * storage stores, decode given data.
     * @param data the data to be decoded
     * @return data decoded according to encoding and charset of the bodypart
     *  that stores this storage.
     * @throws mujmail.MyException
     */
    private String decodeNotRawBodypartData(String data) throws MyException {
        if (bodyPart.getHeader().getEncoding() == BodyPart.ENC_BASE64) {
            return Decode.decodeBase64(data, bodyPart.getHeader().getCharSet());
        } else if (bodyPart.getHeader().getEncoding() == BodyPart.ENC_QUOTEDPRINTABLE) {
            return Decode.decodeQuotedPrintable(data, bodyPart.getHeader().getCharSet());
        } else if (bodyPart.getHeader().getEncoding() == BodyPart.ENC_8BIT || bodyPart.getHeader().getCharSet() != BodyPart.CH_NORMAL) { //8bit or not usascii
            return Decode.decode8bitCharset(data, bodyPart.getHeader().getCharSet());
        } else { // 7- bit charset 	
            return data;
        }
    }
    
    /**
     * <p>
     * Adds given string to the storage.
     * Writes data not necessary immediately but only if bigger content for writing
     * is collected.
     * Note that it is necessary to call method {@link #flushBuffer()} to store data 
     * persistently before reading it.
     * </p>
     * <p>
     * Stores it according to encoding of the body part which content this storage
     * stores either as a raw data or as a text data.
     * </p>
     * @param bf the string which content store.
     */
    public void addToContentBuffered(String bf) throws Exception {
        buffer.bufferContent(bf);
    }
    
    /**
     * Adds given string to the storage.
     * Writes immediately.
     * 
     * Stores it according to encoding of the body part which content this storage
     * stores either as a raw data or as a text data.
     * @param bf the string which content store.
     */
    public void addToContent(String bf) throws Exception {
        addToContentBuffered(bf);
        flushBuffer();
        
    }
    
    /**
     * Stores data written in a buffer persistently. The data can be stored in
     * buffer when method addToContentBuffered is called.
     * @throws java.lang.Exception
     */
    public void flushBuffer() throws Exception {
        buffer.flush();
    }
    /**
     * Returns true if the content stored in this storage is raw data.
     * @return true if the content stored in this storage is raw data
     */
    public boolean isContentRaw() {
        if (getBodyPart().convertedContentMode()) {
            return true;
        } else {
            return (bodyPart.getHeader().getEncoding() == BodyPart.ENC_BASE64 && 
                    bodyPart.getHeader().getCharSet() == BodyPart.CH_NORMAL) ? 
                        true : 
                        false;
        }
    }

    /**
     * Adds given string to the content of the bodypart.
     * If passed content cannot be saved tries to shorten the content and
     * save it partially.
     *
     * This method neither sets the size of the bodypart nor the state of the
     * bodypart. This is done by methods inside class <code>ContentStorage<code/>
     * that calls this method.
     * 
     * This is a low-level method, saves bodypart always as a text data regardless
     * of the body part header. More high level method is addToContent(String).
     *
     * Before calling this method, method {@link #ensurePrealocating} is always
     * called.
     * 
     * @param content the content of the bodypart
     * @param safeMode if save mode is true, the content will be not saved
     *  to new persistent storage, but to temporary one
     * @return the size of content that was added to the content.
     *  content.length if all the content was written.
     */
    protected abstract long addToContent(String content, boolean safeMode) throws Exception;

    /**
     * Checks whether the size of bydypart with added content would be less or
     * equal than given limit.
     * If the size of bodypart with added content and the bodypart is not 
     * partially savebale. If it is partially saveable, sets its state to 
     * <code>BodyPart.BS_PARTIAL</code>. Than, it throws and exception.
     *
     * @param sizeOfAdded the size of added content to bodypart
     * @throws java.lang.Exception if the size of bodypart with added content
     *  is bigger than the limit.
     */
    /*
     * TODO: (David) this method is no more needed since the size of bodypart
     * is now checked while downloading the mail.
    private void checkSizeOfBodyPart(long sizeOfAdded) throws Exception {
        if (getSize() + sizeOfAdded > MAX_SIZE_OF_BODYPART) { 
            if (!MessageHeader.canBePartiallySaved(getBodyPart())) {
                deleteContent();
            } else {
                getBodyPart().setBodyState(BodyPart.BS_PARTIAL);
            }

            throw new Exception("The size of this bodypart is larger than given limit.");
        }
    }
     */

    /**
     * Do the work of adding the content to the storage. Calls the method
     * {@link #ensurePrealocating} before adding the content.
     *
     * @param content
     * @param safeMode
     *
     * @throws Exception if it was not stored whole content.
     */
    private void addToContentEnsurePreallocating(String content, boolean safeMode) throws Exception {
        //checkSizeOfBodyPart(content.length());

        ensurePrealocating();
        long sizeAdded = 0;
        try {
            sizeAdded = addToContent(content, safeMode);
        } catch (Exception exception) {
            throw exception;
        } finally {
            handleAfterAdding(sizeAdded, Functions.getStringByteSize(content));
        }

    }
    
    private void addToContentEnsurePreallocating(byte[] content, boolean safeMode) throws Exception {
        //checkSizeOfBodyPart(content.length);
        
        ensurePrealocating();
        long sizeAdded = addToContentRaw(content, safeMode);

        handleAfterAdding(sizeAdded, content.length);
    }

    /**
     * Adds given byte array to the content of the bodypart.
     * If passed content cannot be saved tries to shorten the content and
     * save it partially.
     *
     * This method neither sets the size of the bodypart nor the state of the
     * bodypart. This is done by methods inside class <code>ContentStorage<code/>
     * that calls this method.
     * 
     * This is a low-level method, saves bodypart always as a binary data regardless
     * of the body part header. More high level method is addToContent(String)
     *
     * Before calling this method, method {@link #ensurePrealocating} is always
     * called.
     * 
     * @param content the content of the bodypart
     * @param safeMode if save mode is true, the content will be not saved
     *  to new persistent storage, but to temporary one
     * @return the size of content that was added to the content.
     *  content.length if all the content was written.
     */
    protected abstract long addToContentRaw(byte[] content, boolean safeMode) throws Exception;


    /**
     * Gets given part of the content stored in this storage. First calling of
     * this method gets first part of the content. If there is another part, next
     * calling gets another part. If the returned part is the last, next returned
     * part will be first.
     * It can be determined whether next calling of this method will return first
     * part of the content by calling method willReturnFirstContent()

     * This method tries to get the part of maximum possible size. It loads the
     * content of this storage until it is reached the end of the storage or
     * it is not enough memory. Thats why the number of calling of getContent to 
     * get whole content of this storage is typically less than number of calling 
     * of method addToContent used to bufferContent the same content.
     * 
     * @return given part of the content stored in this storage
     * 
     * @throws MyException if the loading of the content was not successful
     */
    public abstract String getNotRawContent() throws Throwable;
    
    /**
     * TODO: comment
     * @return
     * @throws Throwable
     */
    public String getContent() throws Throwable {
        if (!isContentRaw()) {
            return getNotRawContent();
        } else {
            return getContentRawAsString();
        }
    }
    
    /**
     * Gets raw body part content as string that contains raw (not
     * encoded) content of body part content.
     */
    private String getContentRawAsString() {
        if (DEBUG) System.out.println("DEBUG - ContentStorage.getContentRawAsString - is converted mode = " + bodyPart.convertedContentMode());
        byte[] rawByteArray;
        try {
            rawByteArray = getContentRaw();
            if (DEBUG) System.out.println("DEBUG - ContentStorage.getContentRawAsString - length of rawByteArray = " + rawByteArray.length);
        } catch (MyException ex) {
            ex.printStackTrace();
            rawByteArray = new byte[0];
        }
        return getStringWithRawContentFromByte(rawByteArray);
    }
    
    /**
     * Gets string from byte array with raw (not encoded) content of
     * this array.
     */
    private String getStringWithRawContentFromByte(byte[] rawByteArray) {
        StringBuffer rawSB = new StringBuffer(rawByteArray.length);
        rawSB.setLength(rawByteArray.length);
        for (int i = 0; i < rawByteArray.length; i++) {
            rawSB.setCharAt(i, (char)rawByteArray[i]);
        }

        return rawSB.toString();
    }
    
    /**
     * Sends content to given output connection.
     * @param connection the connection to that send the content.
     * @throws mujmail.MyException
     */
    public void getContent(DataOutputStream outputStream) throws Throwable {
        do {
            if (isContentRaw()) {
                outputStream.write(getContentRaw());
            } else {
                outputStream.write(getContent().getBytes("utf-8"));
            }
        } while (!willReturnFirstContent());
        outputStream.flush();
    }
    
    /**
     * Sends the content of this storage to given connection.
     * 
     * @param connection the connection to that send the data.
     * @param sendingMode sending mode.
     * @param returnSendedData true if sent data should be returned.
     * @return the data sent to the connection if returnSendedData is true
     *  "" if returnSendedData is false
     * @throws java.lang.Exception
     */
    public abstract String sendContentToConnection(ConnectionInterface connection, 
            MailSender.SendingModes sendingMode, 
            boolean returnSendedData) throws Throwable;
    
    /**
     * 
     * @return true if next calling of getContent or getContentRaw will return
     *  first part of the content.
     */
    public abstract boolean willReturnFirstContent();
    
    /**
     * After calling this method, getContent or getContentRaw will return
     *  first part of the content.
     */
    public abstract void resetToFirstContent();

    /**
     * Gets given part of the content stored in this storage. First calling of
     * this method gets first part of the content. If there is another part, next
     * calling gets another part. If the returned part is the last, next returned
     * part will be first.
     * It can be determined whether next calling of this method will return first
     * part of the content by calling method willReturnFirstContent()

     * This method tries to get the part of maximum possible size. It loads the
     * content of this storage until it is reached the end of the storage or
     * it is not enough memory. Thats why the number of calling of getContent to 
     * get whole content of this storage is typically less than number of calling 
     * of method addToContent used to bufferContent the same content.
     * 
     * @return given part of the content stored in this storage represented as
     *  binary data.
     * 
     * @throws MyException if the loading of the content was not successful
     */
    public abstract byte[] getContentRaw() throws MyException;

    /**
     * Gets the size of the content stored in this storage.
     * @return the size of content stored in this storage
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of the content stored in this storage
     * @param size the new size of content stored in this storage
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Enumeration class which defines copying modes of the body part storage.
     * Used in copy constructor of body part storage.
     */
    public static class CopyingModes {
        private CopyingModes() {};

        /** Make deep copy. Original and new storages will store identical
         but independent content. */
        public static final CopyingModes DEEP_COPY = new CopyingModes();
        /** Makes shallow copy. Original and new storages will share this
         one content.
         Note, that this is allowed only if the body part which content is
         in the same box. */
        public static final CopyingModes SHALLOW_COPY = new CopyingModes();
        /**
         * Creates empty storage of the same type as original storage.
         */
        public static final CopyingModes NO_COPY = new CopyingModes();

    }

    /**
     * The enumeration of all possible storage types of body part.
     */
    public static class StorageTypes {
        private static byte counter = 0;
        private final byte number;
        private final String name;

        private StorageTypes(String name) {
            number = counter++;
            this.name = name;
        }

        /**
         * Gets unique number identifying this storage type.
         * @return the number identifying this storage type
         */
        public byte getStorageTypeNumber() {
            return number;
        }

        public String toString() {
            return name;
        }
        //#ifdef MUJMAIL_FS
        /** This storage uses filesystem to store the content of body part */
        public static final StorageTypes FS_STORAGE = new StorageTypes("filesystem storage");
        //#endif
        /** This storage uses rms database to store the content of body part */
        public static final StorageTypes RMS_STORAGE = new StorageTypes("rms storage");
    }
    
    /**
     * Sets correct state of bodypart after adding. If it was added less than it
     * should be added, deletes whole bodypart in the case that the bodypart is
     * not partially saveable. If it partially saveable, sets the state to partiall.
     * @param sizeWasAdded
     * @param sizeShouldBeAdded
     * @throws java.lang.Exception if sizeWasAdded < sizeShouldBeAdded
     */
    private void handleAfterAdding(long sizeWasAdded, long sizeShouldBeAdded) throws Exception {
        if (sizeWasAdded == sizeShouldBeAdded) {
            getBodyPart().setBodyState(BodyPart.BS_COMPLETE);
            setSize(getSize() + sizeWasAdded);
            return;
        }
        if (!MessageHeader.canBePartiallySaved(getBodyPart())) {
            deleteContent();
            throw new Exception("It was not possible to save whole content of bodypart. Bodypart deleted.");
        }
        getBodyPart().setBodyState(BodyPart.BS_PARTIAL);
        setSize(getSize() + sizeWasAdded);
        throw new Exception("It was not possible to save whole content of bodypart. Bodypart saved only partially.");
    }
    
    /**
     * Adds the data to the content. This means decodes it, saves it to appropriate
     * buffer and writes it when the memory is out or when flush method was called.
     * 
     * The data can be decoded either to String or to byte[] so object of this
     * class contains one buffer that stores String data and flushes the content
     * to this ContentStorage using method addToContent(String, boolean) and one
     * buffer that stores byte[] data and flushes the content to this 
     * ContentStorage using method addToContentRaw(byte[], boolean).
     */
    private class BufferedContentAdder {
        /** Contains String data and flushes the output using addToContent. */
        private AddToContentBuffer contentBuffer = new AddToContentBuffer();
        /** Contains byte[] data and flushes the output using addToContentRaw. */
        private AddToContentRawBuffer contentRawBuffer = new AddToContentRawBuffer();
        
        /**
         * Decodes given string according to the encoding of the body part that
         * content this storage stores and stores it to appropriate buffer.
         * 
         * @param bf the string which content store.
         */
        public void bufferContent(String bf) throws Exception {
            try {
                if (isContentRaw()) {
                    decodeAndBufferRawContent(bf);
                } else {
                    decodeAndBufferContent(bf);
                }
            } catch (Exception e) {
                e.printStackTrace();
                //throw new Exception(); //TODO: why to throw new _empty_ exception ?
                throw e; // rethrow
            } finally {
                
            }

            ensurePrealocating();
        }
        
        /**
         * Clears the buffers cache.
         */
        public void clearCache() {
            contentBuffer.clearCache();
            contentRawBuffer.clearCache();
        }
        
        /**
         * Forces the data to be written from buffers to this ContentStorage.
         * 
         * @throws java.lang.Exception
         */
        public void flush() throws Exception {
            System.out.println("Flushing buffers: ");
            if (contentBuffer.bufferSize() != 0) {
                contentBuffer.flush();
            } if (contentRawBuffer.bufferSize() != 0) {
                contentRawBuffer.flush();
            }
        }

        private void decodeAndBufferContent(String bf) throws Exception {
            String decoded = null;
            try {
                decoded = decodeNotRawBodypartData(bf);
            } catch (Exception exception) {
                flush();
                System.gc();
                decoded = decodeNotRawBodypartData(bf);
            }

            contentBuffer.write(decoded);
        }

        private void decodeAndBufferRawContent(String bf) throws Exception, Exception {
            byte[] decoded = null;
            try {
                if (bodyPart.convertedContentMode()) {
                    decoded = bf.getBytes();
                } else {
                    decoded = Decode.decodeBase64(bf).toByteArray();
                }
            } catch (Exception exception) {
                flush();
                System.gc();
                System.out.println("*** decoding bf = "+bf);
                decoded = Decode.decodeBase64(bf).toByteArray();
            }
            
            contentRawBuffer.write(decoded);
        }
    }
    
    /**
     * StringOutpuBuffer that writes the data out from the buffer using method
     * addToContent.
     */
    private class AddToContentBuffer extends OutputBuffer.StringOutputBuffer {
        protected void writeDataFromBuffer(String bufferData) throws Exception {
            addToContentEnsurePreallocating(bufferData, Settings.safeMode);
        }
        
    }
    
    /**
     * ByteOutpuBuffer that writes the data out from the buffer using method
     * addToContentRaw.
     */
    private class AddToContentRawBuffer extends OutputBuffer.ByteOutputBuffer {
        protected void writeDataFromBuffer(byte[] bufferData) throws Exception {
            addToContentEnsurePreallocating(bufferData, Settings.safeMode);
        }
        
    }

}
