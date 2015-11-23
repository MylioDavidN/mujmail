//#condition MUJMAIL_FS
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
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package mujmail;

import mujmail.util.Callback;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import javax.microedition.io.Connector;

import mujmail.protocols.MailSender.SendingModes;
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;
import mujmail.connections.ConnectionInterface;
import mujmail.protocols.MailSender;

/**
 * Implementation of {@link ContentStorage} that stores the content
 * in file in file system of the device.
 * The file is identified by the field <code>fileURL</code>
 * 
 * It is used to represent body parts that user attached to the mail
 * when writing an email.
 * Does not support writing new content to the storage yet.  
 * 
 * @author David Hauzar
 */
public class FSStorage extends ContentStorage {

    private String fileURL = "";

    public FSStorage(BodyPart bodyPart) { super(bodyPart); }

    /**
     * Initialize class by copying another instance.
     * Note that if the mode is SHALLOW_COPY it the body part which content
     * will is stored in this ContentStorage should be in the same box as original
     * storage.
     * @param bp the body part which content is stored in this storage
     * @param copy AttachmentPart instance to copy
     * @param copyMode defines copying mode
     */
    private FSStorage(BodyPart bp, FSStorage copy, CopyingModes copyMode) {
        super(bp);

        if (copyMode == CopyingModes.NO_COPY) {
            // the instance is yet created
            return;
        }

        // SHALLOW_COPY and DEEP_COPY is in FSStorage the same
        fileURL = copy.fileURL;
        setSize(copy.getSize());
    }

    protected boolean preallocatedToNextSaving() {
        // no preallocating now
        return true;
    }

    protected void preallocateToNextSaving() {
        // no preallocating now
    }

    
    

    /**
     * Initialize <code>ContentInFS</code> with file path and name
     * of the file in which the content is stored.
     * @param file_url	Path to file with trailing /
     * @param fileName	Filename 
     */
    public FSStorage(BodyPart bodyPart, long size, String file_url) {
        super(bodyPart, size);

        fileURL = file_url;
    }
    
    

    protected void deleteContentFromStorage() {
//        throw new UnsupportedOperationException();
    }
    
    
    /**
     * Sets file size according to the size of the file identified by this storage.
     * Starts the operations in new thread.
     * After updating the size calls callback method.
     */
    public void updateSizeInNewThread(Callback callback) {
        GetFileSizeRunnable gfs = 
                    new GetFileSizeRunnable(callback);
            Thread t = new Thread(gfs);
            t.start();
    }
    
    
    
    /**
     * Class to run get file size function in a separate
     * thread.  This is necessary since the FileConnection API
     * is used which cannot be run in the system thread.
     */
    public class GetFileSizeRunnable implements Runnable {

        private final Callback callback;

        /**
         * Create instance of object which will get the file size.
         * @param adder the object of attachment adder
         * @param updatedAttachment the attachment which size will be updated.
         *  The storage must be of type StorageTypes.FS_STORAGE
         */
        public GetFileSizeRunnable(Callback callback) {
            this.callback = callback;
        }

        public void run() {
            
            try {
                setSize(getFileSize());
            } catch (Exception e) {
            }
            
            callback.callback(this, null);
        }
    }
    
    /**
     * Get size of the file stored by this storage.
     * @return the size of the file identified by calling getURL.
     * 
     * @throws java.io.IOException since FileConnection API is used
     */
    private long getFileSize() throws Exception {
        MyFileConnection fc = FilesystemFactory.getFileConnection(getFileURL(), Connector.READ);
        return fc.fileSize();
    }



    public StorageTypes getStorageType() {
        return StorageTypes.FS_STORAGE;
    }

    public ContentStorage copy(BodyPart bp, CopyingModes copyMode) {
        if (!checkCopy(bp, copyMode)) {
            return null;
        }

        return new FSStorage(bp, this, copyMode);
    }

    public String sendContentToConnection(ConnectionInterface connection, SendingModes sendingMode, boolean returnSendedData) throws Exception {
        String capturedAttachmentText = "";

        // send data
        MyFileConnection fc = FilesystemFactory.getFileConnection(fileURL);
        StringBuffer sb = new StringBuffer(171);
        // encode file to base64 171 bytes at a time which is exactly
        // 3 rows at a time.  This is necessary to not take up a lot
        // of phone ram as 171 bytes come right from the file and are
        // sent off to isp in stream.  Also, if exact rows are not sent,
        // the base64 encoding will be corrupted
        try {
            InputStream in = fc.openInputStream();
            try {
                int c;
                int cnt = 0;
                while ((c = in.read()) != -1) {
                    sb.append((char) c);
                    cnt++;
                    if (cnt >= 171) {
                        String tmpAttachmentLine = sendingMode.toEncoding(sb.toString(), true);
                        connection.send(tmpAttachmentLine);
                        capturedAttachmentText = conditionallyAppend(capturedAttachmentText, tmpAttachmentLine, 
                                returnSendedData);
                        //System.out.println("Sent");
                        sb.delete(0, sb.length());
                        cnt = 0;
                    }
                }
            } finally {
                in.close();
            }
        } finally {
            fc.close();
        }
        // send last part of file which is < 171 bytes
        String tmpAttachmentLine = sendingMode.toEncoding(sb.toString(), true);
        connection.sendCRLF(tmpAttachmentLine);
        if (returnSendedData) {
            capturedAttachmentText = MailSender.captureStrCRLF(capturedAttachmentText, "");
        }
        
        
        return capturedAttachmentText;
    // boundary to end email ( -- + boundary + -- )
    }
    
    

    public void saveStorageHeader(DataOutputStream outputStream) throws Exception {
        super.saveStorageHeader(outputStream);

        outputStream.writeUTF(getFileURL());
    }

    public void loadStorage(DataInputStream inputStream) throws Exception {
        super.loadStorage(inputStream);

        fileURL = inputStream.readUTF();
    }



    protected long addToContent(String content, boolean safeMode) {
        // TODO: implement
        throw new RuntimeException("This operation is not yet implemented");
    }

    protected long addToContentRaw(byte[] content, boolean safeMode) {
        // TODO: implement
        throw new RuntimeException("This operation is not yet implemented");
    }

    public byte[] getContentRaw() throws MyException {
        // TODO: implement
        throw new MyException(0, "This operation is not yet implemented");
    }

    public boolean willReturnFirstContent() {
        return true;
    }

    public void resetToFirstContent() {
    }
    
    
    
    

    public String getNotRawContent() throws MyException {
        // TODO: implement
        throw new MyException(0, "This operation is not yet implemented");
    }

    /**
     * Gets the file name in which the content of body part is stored.
     * @return the file name in which the content of body part is stored
     */
//    public String getFileName() {
//        return fileName;
//    }

    /**
     * Gets the url of file in which the content of body part is stored.
     * @return the url of the file in which the content of body part 
     * is stored
     */
    public String getFileURL() {
        return fileURL;
    }
}