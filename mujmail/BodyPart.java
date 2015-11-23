/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2006 Martin Stefan <martin.stefan@centrum.cz>
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.lcdui.StringItem;

/**
 * Represents an email part - email body or email attachment. Contains
 * information about the body part such as it's name, encoding etc.
 * Does not store the content of the body part but contains object
 * of type {@link ContentStorage} that stores the content.
 * 
 * This object is stored persistently in RMS database. It's loading
 * and saving does object of instance {@link MessageHeader} that
 * contains this body part.
 * 
 */
public class BodyPart {
    
    public static final String SOURCE_FILE = "BodyPart";
    
    //body state constants
    public static final byte BS_COMPLETE = 0;
    public static final byte BS_PARTIAL = 1;
    public static final byte BS_EMPTY = 2;
    
    //types of MIME body part, separated only to these types
    //for more detailed information of the type use bodyPart.getBpExtension	
    public static final byte TYPE_TEXT = 2;
    public static final byte TYPE_HTML = 4;
    public static final byte TYPE_MULTIMEDIA = 8; //image audio video
    public static final byte TYPE_APPLICATION = 16;//pdf, postscrips...	
    public static final byte TYPE_OTHER = 32;//model, xworld,... - will never be supported by mujmail
    
    // bodypart transfer encodings
    public static final byte ENC_NORMAL = 0; //7bit
    public static final byte ENC_BASE64 = 1;
    public static final byte ENC_QUOTEDPRINTABLE = 2;
    public static final byte ENC_8BIT = 4;	//8bit 		
    //bodypart text charset
    public static final byte CH_NORMAL = 0; //ASCII and whatever
    public static final byte CH_ISO88591 = 1;
    public static final byte CH_ISO88592 = 2;
    public static final byte CH_WIN1250 = 3;
    public static final byte CH_UTF8 = 4;
    public static final byte CH_USASCII = 5;
    
    private boolean convertedContentMode = false;

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public void setOrder(

    byte order) {
        this.order = order;
    }

    public byte getOrder() {
        return order;
    }
    
    /**
     * Manages displaying body part in the sendmail form.
     */
    public class SendMailDisplayer {
        public SendMailDisplayer() {};
        
        /** The number of items of attachment info in sendmail form.
         * Used when removing attachments from the form. */
        public static final int numberOfItemsInForm = 2;
        
        /** The string item in which is displayed the size of the file in Sendmail form */
        private final StringItem siSize = new StringItem(Lang.get(Lang.SM_FILE_SIZE), "");
        
        /**
         * Displays this body part in the sendmail form.
         * @param sendmail the form to which add information about this body part
         */
        public void addAttachmentToForm(SendMail sendmail) {
            // the name of the attachment
            StringItem siFileNameAttached = new 
                    StringItem(Lang.get(Lang.SM_ATTACHMENT), getHeader().getName() + "\n");
            sendmail.append(siFileNameAttached);

            // the size of the attachment
            sendmail.append(siSize);
            updateSize();
        }
        
        
        /**
         * Updates size of the body part in the SendMail form.
         */
        public void updateSize() {
            System.out.println("Updating size");
            siSize.setText(Functions.formatNumberByteorKByte(getSize()) + "\n");
        }
    }
    
    /**
     * Returns true if the body state is at least partial - this means complete
     * or partial.
     * @return true if the body state is at least partial - this means complete
     * or partial
     */
    public boolean atLeastPartial() {
        return (bodyState == BS_COMPLETE || bodyState == BS_PARTIAL) ? true : false;
    }
    

    /**
     * Contains information about the storage of this body part.
     */
    private ContentStorage contentStorage;
    
    /**
     * Contains information about the storage of this body part.
     */
    private ContentStorage convertedContentStorage;    
    
    /** Contains information about body part header */
    private Header bodyPartHeader;
    
    
    private byte order = 0; //the order of the bodypart in a mail body
    /** Manages displaying this bodypart in sendmail form */
    private SendMailDisplayer sendMailDisplayer = new SendMailDisplayer();
    
    /**
     * The message header of message to which this body part belongs.
     */
    private MessageHeader messageHeader;
    
    /**
     * Gets the box in which this body part is.
     * @return
     */
    public PersistentBox getBox() {return getMessageHeader().getBox();}
    
    /**
     * Gets the object which manages displaying this bodypart on the SendMail form.
     * @return the object which manages displaying this bodypart on the SendMail 
     *  form.
     */
    public SendMailDisplayer getSendMailDisplayer() { return sendMailDisplayer; }
    
    /**
         * Indicates whether body part is partially downloaded or decoded.
         * BS_COMPLETE
         * BS_PARTIAL
         * BS_EMPTY
         */
        private byte bodyState = BS_COMPLETE;
        
        public byte getBodyState() { return bodyState; }
        public void setBodyState(byte bodyState) { this.bodyState = bodyState; }

    /**
     * Creates new instance of body part belonging to header header with the
     * order and body part header the same as body part copy and with the
     * same storage type as body part copy.
     * Note that it just creates the storage of the same type as BodyPart copy
     * but does not copy it's content!!
     * @param header
     * @param copy
     * @param copyMode copy mode of the content of the body part
     */
    public BodyPart(MessageHeader header, BodyPart copy, ContentStorage.CopyingModes copyMode) {
        this.messageHeader = header;
        contentStorage = 
            copy.getStorage().copy(this, copyMode);
        
        bodyPartHeader = new Header(copy.bodyPartHeader);
        
        
        order = copy.order;
    //don't copy bodyState. because InProtocol may be downloading the old incomplete bodyPart, 
    //and by marking this new bodyPart also incomplete bodyPart by default would confuse InProtocol
    //and make it unable to replace the old one with the new one

    }
    
    /**
     * Creates new body part which belongs to the message with header header.
     * The storage type will be determined according to mujMail settings.
     * @param header the header of the message to which this body part belongs
     */
    public BodyPart(MessageHeader header) {
        // TODO: determine the storage type according to mujMail settings
        this.messageHeader = header;
        bodyPartHeader = new Header();
        contentStorage = new RMSStorage(this);
    }

    //#ifdef MUJMAIL_FS
    /**
     * Creates new instance of body part with content stored in FSStorage.
     * @param messageHeader
     * @param fileURL
     * @param fileName
     * @param name
     * @param size
     */
    public BodyPart(MessageHeader messageHeader, String fileURL, String fileName, String name, long size) {
        this.messageHeader = messageHeader;
        contentStorage = new FSStorage(this, size, fileURL);
        
        bodyPartHeader = new Header();
        bodyPartHeader.setName(name);
    }
    //#endif

    //create this when sending a mail
    public BodyPart(MessageHeader messageHeader, String name) {
        this.messageHeader = messageHeader;
        
        // TODO: create storage according to global settings of mujMail
        contentStorage = new RMSStorage(this);

        bodyPartHeader = new Header();
        bodyPartHeader.setName(name);
    }

    //we can call this constructor after a complete parsing and storing a content
    public BodyPart(MessageHeader messageHeader, String nm, byte encoding, byte type, byte charset) {
        this.messageHeader = messageHeader;
        // TODO: create storage according to global settings of mujMail
        contentStorage = new RMSStorage(this);
        
        bodyPartHeader = new Header(nm, type, charset, encoding);
    }
    
    public BodyPart(MessageHeader messageHeader, Header bodyPartHeader) {
        this.messageHeader = messageHeader;
        // TODO: create storage according to global settings of mujMail
        contentStorage = ContentStorage.createStorageInstance(this, ContentStorage.StorageTypes.RMS_STORAGE.getStorageTypeNumber());
        this.bodyPartHeader = bodyPartHeader;
    }
    
    /**
     * Gets viewing mode of this bodypart determined from the content and header
     * of the bodypart.
     * @return the viewing mode of this bodypart determined from it's content and header
     */
    public MailForm.BPViewingModes.ViewingModesWithActions getAutoViewingMode() {
        if ( getHeader().getBodyPartContentType() == BodyPart.TYPE_HTML ) {
         	return MailForm.BPViewingModes.HTML;
        } else if ( getHeader().getBodyPartContentType() == BodyPart.TYPE_TEXT) {
            return MailForm.BPViewingModes.PLAIN_TEXT;
        }
        if (getHeader().getBodyPartContentType() == BodyPart.TYPE_MULTIMEDIA &&
                (getHeader().getName().toLowerCase().endsWith("jpg") || 
                getHeader().getName().toLowerCase().endsWith("jpeg") || 
                getHeader().getName().toLowerCase().endsWith("gif") || 
                getHeader().getName().toLowerCase().endsWith("png"))) {
            return MailForm.BPViewingModes.MULTIMEDIA;
        }

        if (getHeader().getBodyPartContentType() == BodyPart.TYPE_OTHER
            && getHeader().getName().toLowerCase().endsWith("pdf") ) { 
        	return MailForm.BPViewingModes.CONVERTED;
    	}

        // default viewing mode
        return MailForm.BPViewingModes.NO_VIEW;
    }
    
    /**
     * Switches to converted content. This means that method
     * {@link BodyPart#getStorage()} will return storage of
     * converted content.
     * 
     * @see BodyPart#switchToNotConvertedContent()
     * @see BodyPart#convertedContentMode
     * @see BodyPart#getStorage()
     */
    public void switchToConvertedContent() {
        convertedContentMode = true;
    }
    
    /**
     * If converted mode is active - {@link #getStorage()} returns storage
     * of converted content - returns true.
     * 
     * @return true if converted mode is active.
     * 
     * @see BodyPart#switchToNotConvertedContent()
     * @see BodyPart#switchToConvertedContent()
     * @see BodyPart#getStorage()
     */
    public boolean convertedContentMode() {
        return convertedContentMode;
    }
    
    /**
     * Switches to not converted content. This means that method
     * {@link BodyPart#getStorage()} will return storage of non
     * converted content.
     * 
     * @see BodyPart#switchToConvertedContent()
     * @see BodyPart#convertedContentMode
     * @see BodyPart#getStorage()
     */
    public void switchToNotConvertedContent() {
        convertedContentMode = false;
    }

    /**
     * Gets the storage of this body part.
     * Note that it would be returned the storage of converted
     * or not converted content accordingly to whether method
     * {@link #switchToConvertedContent()} or {@link #switchToNotConvertedContent()}
     * was called last time.
     * 
     * @return the storage of this body part.
     * 
     * @see #switchToConvertedContent()
     * @see #switchToNotConvertedContent()
     */
    public ContentStorage getStorage() {
        return (convertedContentMode) ? getConvertedStorage() : contentStorage;
    }
    
    /**
     * Gets the storage of converted content of this body part.
     * @return the storage of this body part.
     */
    private ContentStorage getConvertedStorage() {
        return convertedContentStorage;
    }
    
    public void createConvertedContentStorage() {
    	convertedContentStorage = new RMSStorage(this);
    }
    /**
     * Gets the information stored in body part header.
     * @return the information in body part header
     */
    public Header getHeader() { return bodyPartHeader; }

    /**
     * Gets the size of the body part.
     * @return the size of this body part
     */
    public long getSize() {
        //System.out.println("Here 1 " + contentStorage);
        return contentStorage.getSize();
    }

    /**
     * Sets the size of this body part.
     * @param size the new size of this body part
     */
    public void setSize(long size) {
        contentStorage.setSize(size);
    }
    
    /**
     * Saves information about body part to output stream (RMS database)
     * Does not save the content of the body part, saves only information about
     * this body part.
     * @param outputStream the output stream to which the information about
     *  this body part will be saved
     * @throws java.lang.Exception can occur while writing to the outputStream
     */
    public void saveBodyPart(DataOutputStream outputStream) throws Exception {
        outputStream.writeByte(bodyState);
        outputStream.writeByte(order);
        
        bodyPartHeader.save(outputStream);
        
        outputStream.writeByte(getStorage().getStorageType().getStorageTypeNumber());
        getStorage().saveStorageHeader(outputStream);
        if (convertedContentStorage == null)
        	outputStream.writeBoolean(false);
        else {
        	outputStream.writeBoolean(true);
        	getConvertedStorage().saveStorageHeader(outputStream);
        }
    }
    
    /**
     * Loads information about body part from input stream (of RMS database).
     * Does not load the content of body part, loads only information about
     * body part.
     * @param inputStream the input stream in which are stored information
     *  about this body part.
     * @throws java.lang.Exception can occur while reading inputStream
     */
    public void loadBodyPart(DataInputStream inputStream) throws Exception {
        setBodyState(inputStream.readByte());
        order = inputStream.readByte();
        
        bodyPartHeader = new Header();
        bodyPartHeader.load(inputStream);
        
        // create the instance of storage of given type
        byte storageType = inputStream.readByte();
        contentStorage = ContentStorage.createStorageInstance(this, storageType);
        // load the storage
        contentStorage.loadStorage(inputStream);
        if (inputStream.readBoolean()) {
			convertedContentStorage = ContentStorage.createStorageInstance(this, storageType);
        	convertedContentStorage.loadStorage(inputStream);
        }
    }

    /**
     * The header of the body part
     */
    public static class Header {
        public Header() {}
        public Header(String name, byte bodyPartContentType, byte charset, byte encoding) {
            this.name = name;
            this.bodypartContentType = bodyPartContentType;
            this.charSet = charset;
            this.encoding = encoding;
        }
        
        public Header(Header copy) {
            this.name = copy.name;
            this.bodypartContentType = copy.bodypartContentType;
            this.charSet = copy.charSet;
            this.encoding = copy.encoding;
        }
        
        private String name = "default_mail_body"; // bodypart's name or attachment's filename		
        
        
         /**
         * Bodypart transfer encoding. Possible values
         * ENC_NORMAL
         * ENC_BASE64
         * ENC_QUOTEDPRINTABLE
         * ENC_8BIT
         */
        private byte encoding = ENC_NORMAL;
        /**
         * Type of body part. Possible values:
         * TYPE_TEXT
         * TYPE_HTML
         * TYPE_MULTIMEDIA
         * TYPE_APPLICATION
         * TYPE_OTHER
         */
        private byte bodypartContentType = TYPE_TEXT;
        /**
         * Bodypart text charset. Possible values:
         * CH_NORMAL
         * CH_ISO88591
         * CH_ISO88592
         * CH_WIN1250
         * CH_UTF8
         * CH_USASCII
         */
        private byte charSet = CH_NORMAL;
        
        
        public byte getEncoding() {
            return encoding;
        }
        public void setEncoding(byte encoding) {
            this.encoding = encoding;
        }
        
        
        public byte getBodyPartContentType() { return bodypartContentType; }
        public void setBodyPartContentType(byte bodyPartContentType) { this.bodypartContentType = bodyPartContentType; }
        
        public byte getCharSet() { return charSet; }
        public void setCharSet(byte charSet) {this.charSet = charSet; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        /**
         * Gets the file extension.
         * @return the extension of the file in which the content is stored.
         */
        public String getExtension() {
            return getName().substring(getName().lastIndexOf('.') + 1);
        }
        
        /**
         * Saves this body part header to RMS database
         * @param outputStream
         * @throws java.lang.Exception
         */
        void save(DataOutputStream outputStream) throws Exception {
            outputStream.writeUTF(name);
            outputStream.writeByte(bodypartContentType);
            outputStream.writeByte(getCharSet());
            outputStream.writeByte(getEncoding());
        }
        
        /**
         * Loads this body part header to input stream (typically rms database)
         * @param inputStream
         * @throws java.lang.Exception
         */
        void load(DataInputStream inputStream) throws Exception {
            this.name = inputStream.readUTF();
            this.bodypartContentType = inputStream.readByte();
            this.charSet =inputStream.readByte();
            this.encoding =inputStream.readByte();
            
        }
        
        public String toString() {
            return super.toString() + "; name = " + name + "; encoding = " + encoding + "; bodypartContentType = " + bodypartContentType + "; charSet = " + charSet;
        }
        
    }

    /**
     * Enumeration class of all possible types of body part.
     */
    public static class BodyPartTypes {
        private final String name;
        private BodyPartTypes(String name) {this.name = name;}
        public String toString() { return name; }

        public static final BodyPartTypes BODY = new BodyPartTypes("body");
        public static final BodyPartTypes ATTACHMENT = new BodyPartTypes("attachment");

    }
}
