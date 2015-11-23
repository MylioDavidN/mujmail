package mujmail;

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

import java.util.Date;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;

import mujmail.account.MailAccount;
import mujmail.util.Functions;
//#ifdef MUJMAIL_SEARCH
import mujmail.search.MessageSearchResult;
//#endif

/**
 * Represents the header of the mail. 
 * It contains list of BodyParts (body of the mail + attachments) of given 
 * mail.
 * It stores information from which account it was downloaded (accountID).
 * The header is stored in rms database (recordID)
 */
public class MessageHeader {

      /* ****************
       *    CONSTANTS   *
       ******************/

	private static final boolean DEBUG = false;
    private static final String EMPTY_STRING = "";


    // formats of message
    public static final byte FRT_PLAIN = 0;
    public static final byte FRT_MULTI = 1;
    
    //body-fetch status
    public static final byte NOT_READ = 0; //if was read by user, is set by mailForm
    public static final byte READ = 1;
    //if a header was stored on the device
    public static final byte NOT_STORED = 0;
    public static final byte STORED = 1; //if the header(not bodyparts) was stored once, no need to saved it once more in MailDb.saveHeaders()
    //the size of bodyparts vector indicates whether each bodypart was stored
    //because when a bodypart is stored to DB, bodyparts vector's size is increased
    //body-sent status
    public static final byte FAILED_TO_SEND = 0;
    public static final byte TO_SEND = 1;
    public static final byte SENT = 2;
    public static final byte REPLIED = 4;
    
    /** Message ID separator beetween Folder name and messageID obtained from server
     *  See RFC3501 5.1.  // character This should not contain mailbox mane
     */
    public static final char MSG_ID_SEPARATOR = '&';
    /** String representation of {@link #MSG_ID_SEPARATOR} char */
    public static final String MSG_ID_SEPARATOR_STR = new String( String.valueOf( MSG_ID_SEPARATOR ) );
      /* *************************
       *    INSTANCE VARIABLES   *
       ***************************/

    //#ifdef MUJMAIL_SEARCH
    /** Search result of the message. This item makes sense only when the message
     has been searched and it was returned as the result of searching. */
    private MessageSearchResult searchResult = MessageSearchResult.NO_MATCH;
    //#endif
    /** The record ID of the header in rms database */
    private int recordID;
    /** Original box where the message was before being deleted */
    private char orgLocation;
    //we must initiate from and recipients otherwise sort wont always work
    private String from;
    //we must initiate from and recipients otherwise sort wont always work
    private String recipients;
    private String subject;

    /**
     * Contains value of the boundary parameter of the Content-Type field.
     * <p>
     * Example:<br>
     * if header contains line
     * <pre>Content-Type: multipart/related; boundary=0016e659f8c86d2f8b0463bf768e</pre>
     * than the <code>boundary</code> value is 0016e659f8c86d2f8b0463bf768e
     * </p>
     */
    private String boundary;
    /** Unique primary key, that is by the way used to test existence of a mail in the mobile device; */
    private String messageID;
    /** Folder name where on server mail is stored */
    private String imapFolder;
    /**
     * This field represents content of Message-ID field in e-mail message
     * header. It used in threading algorithm to reference to parent message.
     * 
     * While "Message-ID" field is optional in e-mail header (see
     * <a href="http://tools.ietf.org/html/rfc5322#section-3.6.4">RFC 5322,
     * section 3.6.4</a>), when not found in message header value from
     * {@link #messageID} is used.
     */
    private String threadingMessageID;
    /** Account from which the mail was downloaded - must be given for every mail */
    private String accountID;
    byte messageFormat;
      // TODO (Betlista): why aren't these statuses commented ?
    byte readStatus;
    byte DBStatus;
    byte sendStatus;

    /**
     * Represents size of the email. The size is in bytes and could be little
     * bit different from real received message size.
     */
    private int size; //in Bytes
    private long time;
    boolean deleted = false;
    boolean flagged = false;
    // bodyParts vector is here to cache reading and displaying mail parts	
    private Vector bodyParts = new Vector();
    // attachFileParts vector stores attachment file name, path and size
    //Vector attachFileParts = new Vector();

      // fields used in threading
    /** flag that indicates whether the message is empty (have no body) */
    // TODO: that is not true: the message has no body if message.getBodyPartCount() == 0
    // this is some threading field!!
    private boolean isEmpty = true;
    /**
     * Represents thread parent - all messages in thread have same parent 
     * parent message can be empty (see {@link #isEmpty})
     * parent ID can be empty, but it's not null, it's ""
     * 
     * Parent ID is stored also in {@link #parentIDs parentIDs vector}.
     * Parent ID could be changed when threading of messages is performed.
     */
    private String parentID = null;
    /**
     * Represents value of <code>In-Reply-To</code> header from e-mail header.
     * There is (just small) probability that there is no messageID in this header.
     */
    private String replyTo = null;
    /**
     * Represents path to root message.
     * It can be empty (Vector with 0 size).
     * It is never null, to prevent comparing with null again and again.
     */
    private Vector/*<String>*/ parentIDs = null;

    /** The box to which this message belongs */
    private final PersistentBox box;

      /* *******************
       *    CONSTRUCTORS   *
       *********************/

    public MessageHeader(PersistentBox box) {
        this.box = box;
        orgLocation = 'X';
        from = "+sender@server.com+";
        recipients = "+recipient@server.com+";
        subject = EMPTY_STRING;
        accountID = "mujmail@cia.gov";
        messageFormat = FRT_PLAIN;
        readStatus = NOT_READ;
        DBStatus = NOT_STORED;
        sendStatus = TO_SEND;
        parentIDs = new Vector();
        parentID = EMPTY_STRING;
        threadingMessageID = Long.toString( System.currentTimeMillis(), 35 ) + "@mujmail.org";
        isEmpty = false;
        imapFolder = EMPTY_STRING;
    }    

    /**
     * Makes a copy of given message header. Does not copy bodyparts.
     * @param box
     * @param copy
     */
    public MessageHeader(PersistentBox box, MessageHeader copy) {
        this(box);
        orgLocation = copy.orgLocation;
        recordID = copy.recordID;
        from = copy.from;
        recipients = copy.recipients;
        subject = copy.subject;
        boundary = copy.boundary;
        messageID = copy.messageID;
        imapFolder = copy.imapFolder;
        accountID = copy.accountID;
        messageFormat = copy.messageFormat;
        readStatus = copy.readStatus;
        DBStatus = copy.DBStatus;
        sendStatus = copy.sendStatus;
        size = copy.size;
        time = copy.time;
        deleted = copy.deleted;

        threadingMessageID = copy.threadingMessageID;
        parentID = copy.parentID;
        Vector copyParentIDs = copy.parentIDs;
        if ( copyParentIDs != null ) {
            final int size = copyParentIDs.size();
            parentIDs = new Vector( size );
            for ( int i = 0; i < size; ++i) {
                parentIDs.addElement( copyParentIDs.elementAt( i ) );
            }
        }
    }

    /*
     * call this when sending a new mail
     * practically we don't need to create an instance of MessageHeader to send a simple mail, just use strings
     * but this constructor will be needed when forwarding or send a multipart mail.
     * ID must be generated randomly for each header.
     * So we create one extra MessageHeader instance...
     */
    public MessageHeader(PersistentBox box, String frm, String rcps, String sbj, String ID, long tm) {
        this(box);
        from = frm;
        recipients = rcps;
        subject = sbj;
        messageID = ID;
        time = tm;
    }

      /* **************************
       *    GETTERS AND SETTERS   *
       ****************************/

    /**
     * Gets the box to which this message belongs.
     * @return
     */
    public PersistentBox getBox() {
        return box; 
    }
    
    public MailDB getMailDB() {
        return box.getMailDB();
    }
    
    /**
     * Returns true if this message is special threading message.
     * @return
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     *
     * @param isEmpty true if this message is special threading message
     */
    public void setEmpty(boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        if (parentID == null) {
            this.parentID = EMPTY_STRING;
        } else {
        	if ( parentID.equals( this.threadingMessageID ) ) {
        		System.out.println( "ERROR MessageHeader.setParentID(String) - trying to set parentID same as the threadingMessageID" );
        		if (DEBUG) {
        			System.out.println( "DEBUG MessageHeader.setParentID(String) - header: " + this.toString() );
        			throw new IllegalArgumentException();
        		}
        	} else {
        		this.parentID = parentID;
        	}
        }
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public Vector/*<String>*/ getParentIDs() {
        return parentIDs;
    }

    public void setParentIDs(Vector parentIDs) {
        if ( parentIDs == null ) {
            this.parentIDs = new Vector(0);
        } else {
            this.parentIDs = parentIDs;
        }
    }

    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int recordID) {
        this.recordID = recordID;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public void setOrgLocation(char orgLocation) {
        this.orgLocation = orgLocation;
    }

    public char getOrgLocation() {
        return orgLocation;
    }

    /**
     * Call if this message failed to send.
     */
    public void setFailedToSend() {
        sendStatus = FAILED_TO_SEND;
    }
    
    /**
     * Call if this message was successfully sent.
     */
    public void setSent() {
        sendStatus = SENT;
    }
    
    /**
     * Returns true if this message was already read.
     * @return true if this message was already read.
     */
    public boolean wasRead() {
        return readStatus == READ;
    }
    
    /**
     * Marks this message as plain message.
     */
    public void markAsPlain() {
        messageFormat = FRT_PLAIN;
    }
    
    /**
     * Marks this message as multipart message.
     */
    public void markAsMultipart() {
        messageFormat = FRT_MULTI;
    }
    
    /**
     * Marks this message as read.
     */
    public void markAsRead() {
        readStatus = READ;
    }
    
    /**
     * Adds recipient to the message.
     * @param recipient the recipient to be added.
     */
    public void addRecipient(String recipient) {
        if (recipients.startsWith("+")) //has default value
        {
            recipients = recipient + " *";
        } else {
            recipients += recipient + " *";
        }
    }
    
    public String toString() {
        return super.toString() + "/n Number of bodyparts = " + getBodyPartCount();
    }
    
    /**
     * Ensures whether there is a recipient specified. If not, adds default 
     * recipient
     * @param defaultRecipient the recipient to be added if no recipient is yet
     *  specified.
     */
    public void ensureRecipient(String defaultRecipient) {
        if (recipients.startsWith("+")) {
            // TODO (Betlista): what is the difference between this call and 
            // addRecipient(defaultRecipient)
            // I know, the result is different, there is missing " *" at the end
            // but is that important ?
            recipients = defaultRecipient;
        }
    }
    
    /**
     * Marks this message as read.
     */
    public void markAsReplied() {
        sendStatus = REPLIED;
    }
    
    /**
     * Marks this message as read.
     */
    public void markAsDeleted() {
        deleted = true;
    }
    
    /**
     * Was this message deleted?
     * @return true if this message was deleted.
     */
    public boolean wasDeleted() {
        return deleted;
    }
    
    /**
     * Marks this message as read.
     */
    public void markAsFlagged() {
        flagged = true;
    }

    
    /**
     * Saves the header of the message and of all bodyparts to the RMS database.
     * Does not save the content of the message.
     * If the status of the message is header.DBStatus == MessageHeader.STORED
     * saves the header to existing record in the database (just updates it)
     * @return the record ID under which the header is saved
     * @throws mujmail.MyException
     */
    public int  saveHeader() throws MyException {
        return box.getMailDB().saveHeader(this);
    }

    /**
     * Gets the message id without special characters.
     * @return message id without special characters
     */
    public String getMessageIDWithoutSpecCharacters() {
        String tempID = messageID.replace('/', '1');
        tempID = tempID.replace('\\', '2');
        tempID = tempID.replace('.', '3');
        tempID = tempID.replace(MessageHeader.MSG_ID_SEPARATOR, '4');
        
        return tempID;
    }

    /** ...and then start adding body parts headers to it
     *
     * The method adds a <code>BodyPart</code> to the message header to cache info about each mail part, but this method doesn't store it in the <code>RecordStore</code>.
     * Its called only when the real content is stored and so we have a proper functioning recordID
     * Note: Storing the real content is done in the <code>saveBodyPart</code> method in <code>MailDb</code> class.
     * @see #MailDB
     */
    public void addBodyPart(BodyPart bpHeader) {
        bodyParts.addElement(bpHeader);
    }

    /* Add attachment file info to Vector (in <code>MessageHeader</code>) 
     *
     * The method adds an <code>AttachmentPart</code> to the message header to store the attachment file info.
     * Only the file url, name, and size are stored.
     * @param apHeader <code>AttachmentPart</code> with attachment file info
     */
//    public void addAttachFilePart(AttachmentPart apHeader) {
//        attachFileParts.addElement(apHeader);
//    }

    public BodyPart getBodyPart(int index) {
        return (BodyPart) bodyParts.elementAt(index);
    }
    
    /**
     * Removes all body parts.
     */
    public void deleteAllBodyParts() {
        bodyParts.removeAllElements();
    }
    
    /**
     * Replaces the body of this message
     * @param bodyPart
     */
    public void replaceBody(BodyPart newBody) {
        bodyParts.setElementAt(newBody, 0);
    }
    
    /**
     * Replaces the attachment at given index with new attachment.
     * @param newAttachment the attachment which replaces the old attachment
     * @param i the index of attachment to be replaced
     */
    public void replaceAttachment(BodyPart newAttachment, int i) {
        bodyParts.setElementAt(newAttachment, i+1);
    }
    
    /**
     * Replaces the body part at given index with new body part.
     * @param newBodyPart the body part which replaces the old body part
     * @param i the index of the body part to be replaced
     */
    public void replaceBodyPart(BodyPart newBodyPart, int i) {
        bodyParts.setElementAt(newBodyPart, i);
    }
    
    //#ifdef MUJMAIL_SEARCH
    /**
     * Sets the result of searching to this message.
     * @param searchResult the result of searching.
     */
    public void setSearchResult(MessageSearchResult searchResult) {
        this.searchResult = searchResult;
    }
    //#endif
    
    //#ifdef MUJMAIL_SEARCH
    /**
     * Gets the result of searching in this message.
     * @return the result of searching in this message.
     *  if this message have not been searched yet, alwayes returns
     * {@link mujmail.search.MessageSearchResult.NO_MATCH}.
     */
    public MessageSearchResult getSearchResult() {
        return searchResult;
    }
    //#endif
    
    /**
     * Insert the bodypart to the list of body parts. Each body part with an index
     * greater or equal is shifted upward to have an index one greater than the 
     * value it had previously.
     * @param bodyPart
     * @param i
     */
    public void insertBodyPartAt(BodyPart bodyPart, int i) {
        bodyParts.insertElementAt(bodyPart, i);
    }
    
    /**
     * Deletes this message from database and the box to which the message belongs.
     * @param reportBox the box to which should be reported warning messages.
     * @param  trashMode describes the storing of deleted mail to trash
     */
    public void deleteFromDBAndBox(TheBox reportBox, Trash.TrashModes trashMode) {
        deleted = true;
        MujMail.mujmail.getTrash().storeToTrash(this, trashMode);
        if ( getMailDB() != null) {
            // Delete from DB, here comes only persistent folders
            getMailDB().deleteMail(this, (PersistentBox)reportBox);
        }
        MujMail.mujmail.getMailDBManager().removeMessage(this); // Remove from Boxes
    }
    
    /**
     * Ads the body to this email. Note that the body should not be already
     * inserted.
     * @param body the body part representing the body of the mail
     */
    public void addBody(BodyPart body) {
        if (bodyParts.size() == 0) bodyParts.addElement(body);
        else insertBodyPartAt(body, 0);
    }

    /*
     * Get the attachment file info (path, name, and size) from <code>MessageHeader</code>
     * @param index	Element in <code>AttachmentPart</code> Vector (in <code>MessageHeader</code>
     * @return	<code>AttachmentPart</code> with attachment file path, name, and size
     */
//    public AttachmentPart getAttachmentPart(byte index) {
//        if (!attachFileParts.isEmpty()) {
//			return (AttachmentPart)attachFileParts.elementAt(index);
//		} else {
//			return (AttachmentPart)null;
//		}
//    }

    public String getBodyPartContent(byte index) {
        try {
            BodyPart bp = (BodyPart) bodyParts.elementAt(index);
            return bp.getStorage().getContent();
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.out.println("exception in get body part content");
            return "This bodypart was not yet downloaded or deleted. To see it, redownload it.";
        }
    }

    /**
     * Gets converted bodypart content.
     * @param index		ID of bodypart of the email
     * @return			content as String
     */
    public String getConvertedBodyPartContent(byte index) {
        BodyPart bp = (BodyPart) bodyParts.elementAt(index);
        try {
            bp.switchToConvertedContent();
            
            if (DEBUG) System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent Bodypart chosen="+(index+1));
			
            String extension = bp.getHeader().getExtension(); 
            if (!("pdf".equals(extension) || "jpg".equals(extension))) {
            	MujMail.mujmail.alert.setAlert(Lang.get(Lang.ALRT_MF_UNSUPPORTED_CONVERSION), AlertType.ERROR);
            	return "";
            }

            if (bp.getStorage() == null) {
            	/*** Download converted bodypart ***/
            	//TODO: Check if this message comes from mujMail server account
            	if (DEBUG) System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent - Retrieving converted body");
            	if (!(getBox() instanceof InBox)) throw new MyException(0, "Only inbox can retrive converted bodies");
                ((MailAccount)MujMail.mujmail.getMailAccounts().get(this.accountID)).getProtocol().
            		getConvertedBody(this, index, (InBox)getBox());
            	if (DEBUG) System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent - Converted body retrieved");
            	/*** Save the bodypart ***/
            }

			while (bp.getStorage() == null) {
				bp = (BodyPart)bodyParts.elementAt(index);
				synchronized (bp) {
					if (DEBUG) {
						System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent - Waiting while converted body retrieved");
					}
					bp.wait();
				}
				if (DEBUG) {
	            	System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent - Still waiting ...");					
				}
            }
            
			if (DEBUG) System.out.println("DEBUG MessageHeader.getConvertedBodyPartContent - Finished waiting, storage instance = " + bp.getStorage());
			bp.switchToConvertedContent();
           	return bp.getStorage().getContent();
        } catch (Throwable ex) {
        	ex.printStackTrace();
            System.out.println("exception in get body part content");
            return "";
        } finally {
            bp.switchToNotConvertedContent();
        }
    }    
    
    public byte[] getBodyPartContentRaw(byte index) {
        try {
            return ((BodyPart) bodyParts.elementAt(index)).getStorage().getContentRaw();
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    public byte getBodyPartCount() {
        return (byte) bodyParts.size(); //byte because we never have more bodyparts than 127
    }
    
    /**
     * Removes body part at specified index.
     * @param i
     */
    public void removeBodyPart(int i) { // TODO (Betlista): this (int) is inconsistent with getBodyPartContent(byte index)
        bodyParts.removeElementAt(i);
    }
    
    /**
     * Gets the body of the mail with this header.
     * @return the body of the mail with this header
     */
    public BodyPart getBody() {
        return (BodyPart) bodyParts.elementAt(0);
    }
    
    /**
     * Gets the number of attachments.
     * @return the number of attachments
     */
    public int getAttachementCount() {
        return bodyParts.size()-1;
    }
    
    //#ifdef MUJMAIL_FS
    /**
     * Removes all filesystem attachments (not others!).
     */
    public void removeFSAttachments() {
        Vector toRemove = new Vector();
        for (int i = 0; i < getAttachementCount(); i++) {
            BodyPart bp = getAttachement(i);
            if (bp.getStorage().getStorageType() == ContentStorage.StorageTypes.FS_STORAGE) {
                toRemove.addElement(bp);
            }
        }
        
        for (int i = 0; i < toRemove.size(); i++) {
            bodyParts.removeElement(toRemove.elementAt(i));
        }
    }
    //#endif
    
    /**
     * Removes the attachment with index i (the attachment got by calling
     * getAttachement(i)).
     * @param i
     */
    private void removeAttachment(int i) {
        bodyParts.removeElementAt(i);
    }
    
    /**
     * Gets recipients of the message (field "To").
     * @return the recipients of the message.
     */
    public String getRecipients() {
        return recipients;
    }
    
    /**
     * Gets the sender of the message (field "From").
     * @return the sender of the message.
     */
    public String getSender() {
        return from;
    }
    
    /**
     * Gets the subject of the message.
     * @return the subject of the message
     */
    public String getSubject() {
        return subject;
    }
    
    /**
     * Gets the i-th attachment
     * @param i the number of attachment to get
     * @return the i-th attachment
     */
    public BodyPart getAttachement(int i) {
        return (BodyPart) bodyParts.elementAt(i+1);
    }
    
    /**
     * Returns true if this message is plain.
     * @return true if this message is plain.
     */
    public boolean isPlain() {
        return messageFormat == FRT_PLAIN;
    }

    public byte getBpEncoding(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getHeader().getEncoding();
    }

    public byte getBpType(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getHeader().getBodyPartContentType();
    }

    public String getBpName(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getHeader().getName();
    }

    public String getBpExtension(byte i) {
        String name = ((BodyPart) bodyParts.elementAt(i)).getHeader().getName();
        byte j = (byte) name.lastIndexOf('.');
        return (j == -1) ? name : name.substring(j + 1);
    }

    public byte getBpCharSet(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getHeader().getCharSet();
    }

    public long getBpSize(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getStorage().getSize();
    }

    //gets emails from one big string of emails - recipients
    public Vector getRcp() {
        return Functions.parseRcp(recipients);
    }

    //changes the size of the mail to the current situation
    public int updateSize() {
    	int newSize = Functions.getStringByteSize(from) + Functions.getStringByteSize(subject) + Functions.getStringByteSize(recipients);

        for (byte i = (byte) (bodyParts.size() - 1); i >= 0; --i) {
        	newSize += ((BodyPart) bodyParts.elementAt(i)).getStorage().getSize();
        }

        setSize( newSize );
        return newSize;
    }
    
    /**
     * Gets the number of milliseconds since the standard base time known as 
     *  "the epoch", namely January 1, 1970, 00:00:00 GMT to the date of the message.
     * @return  the unix timestamp of the message.
     */
    public long getTime() {
        return time;
    }

    //converts long time to string format "Tue, 28 Nov 2006 17:00:05", this method is extremely slow, use it wisely.
    public String getTimeStr() {
        String time = new Date(this.time).toString();
        StringBuffer sb = new StringBuffer(37);
        if (time.length() >= 33) //has timezone info and time shift (nokia 7500 has this)
        {
            sb.append(time.substring(0, 3)).append(", ").append(time.substring(8, 10)).append(time.substring(3, 7)).append(time.substring(29)).append(time.substring(10, 19));
        } else if (time.length() >= 27) //has timezone info
        {
            sb.append(time.substring(0, 3)).append(", ").append(time.substring(8, 10)).append(time.substring(3, 7)).append(time.substring(23)).append(time.substring(10, 19));
        } else {
            sb.append(time.substring(0, 3)).append(", ").append(time.substring(8, 10)).append(time.substring(3, 7)).append(time.substring(20)).append(time.substring(10, 19));
        }
        return sb.toString();
    }

    //converts long time to string format "17:00"
    public String getShortTimeStr() {
        int minutes = (int) ((time / 60000) % 60);
        int hours = (int) ((time / 3600000) % 24);
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        String hoursStr = (hours < 10 ? "0" : "") + hours;
        return hoursStr + ":" + minutesStr;
    }

    public byte getBpState(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getBodyState();
    }

    public byte getBpOriginalOrder(byte i) {
        return ((BodyPart) bodyParts.elementAt(i)).getOrder();
    }

    //tells if a bodypart of a concrete type can be saved to the DB if it's damaged or only partially downloaded
    static public boolean canBePartiallySaved(BodyPart body) {
        switch (body.getHeader().getBodyPartContentType()) {
            case BodyPart.TYPE_TEXT:
            case BodyPart.TYPE_HTML:
                return true;
            default:
                return false;
        }

    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getMessageID() {
        return messageID;
    }
    
    public String getIMAPFolder() {
        return imapFolder;
    }
    
    /**
     * Sets imap folder name where message take place. 
     * Note for POP3 can be left empty.
     * @param folderName INBOX a so on [Gmail]/All mails
     */
    public void setIMAPFolder(String folderName) {
        if (DEBUG) System.out.println("DEBUG MessageHeader.setIMAPFolder : " + folderName);
        imapFolder = folderName;
    }
    
    /**
     * @return the threadingMessageID
     */
    public String getThreadingMessageID() {
        return threadingMessageID;
    }

    /**
     * @param threadingMessageID the threadingMessageID to set
     */
    public void setThreadingMessageID(String threadingMessageID) {
        this.threadingMessageID = threadingMessageID;
    }

    /**
     * Getter for {@link #boundary} field.
     * 
     * @return {@link #boundary}
     */
    public String getBoundary() {
        return boundary;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTime( //in Bytes TODO: <- Betlista: ?!?
    long time) {
        this.time = time;
    }

    /**
     * Setter for {@link #boundary} field.
     * 
     * @param boundary new mail boundary
     */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public void setAccountID( /** Account from which the mail was downloaded - must be given for every mail */
    String accountID) {
        this.accountID = accountID;
    }

    public //we must initiate from and recipients otherwise sort wont allways work
    String getFrom() {
        return from;
    }

    public String getAccountID() {
        return accountID;
    }

    public int getSize() {
        return size;
    }

    /**
     * Setter for {@link #size} field.
     * 
     * @param size new size
     */
    public void setSize(int size) {
        this.size = size;
    }

    private void copyVectorToVector(final Vector from, final Vector to) {
        Object o;
        for (int i = 0; i < from.size(); ++i) {
            o = from.elementAt( i );
            to.addElement( o );
        }
    }

    public void fillWith(final MessageHeader messageHeader) {
        this.DBStatus = messageHeader.DBStatus;
        this.accountID = messageHeader.accountID;
        Vector v = new Vector();
        copyVectorToVector(messageHeader.bodyParts, v);
        this.bodyParts = v;
        this.boundary = messageHeader.boundary;
//        this.box = messageHeader.box;
        this.DBStatus = messageHeader.DBStatus;
        this.deleted = messageHeader.deleted;
        this.flagged = messageHeader.flagged;
        this.from = messageHeader.from;
        this.isEmpty = messageHeader.isEmpty;
        this.messageFormat = messageHeader.messageFormat;
        this.messageID = messageHeader.messageID;
        this.imapFolder = messageHeader.imapFolder;
        this.threadingMessageID = messageHeader.threadingMessageID;
        this.orgLocation = messageHeader.orgLocation;
        this.parentID = messageHeader.parentID;
        v = new Vector();
        copyVectorToVector(messageHeader.parentIDs, v);
        this.parentIDs = v;
        this.readStatus = messageHeader.readStatus;
        this.recipients = messageHeader.recipients;
        this.recordID = messageHeader.recordID;
        this.replyTo = messageHeader.replyTo;
        //#ifdef MUJMAIL_SEARCH
        this.searchResult = new MessageSearchResult( messageHeader.searchResult );
        //#endif
        this.sendStatus = messageHeader.sendStatus;
        this.size = messageHeader.size;
        this.subject = messageHeader.subject;
        this.time = messageHeader.time;
    }

    /* ********************
     *   Object methods   *
     **********************/
    private static final String CLASS_NAME = "MessageHeader";

    //#ifdef MUJMAIL_DEVELOPMENT
//#     /**
//#      * Returns string representation for this class.
//#      * It's used only in development environment, that's the reason why
//#      * the method is not fully implemented (doesn't contain all fields
//#      * in string representation).
//#      */
//#     public String toString() {
//#         StringBuffer buff = new StringBuffer( CLASS_NAME ).append('[');
//#         buff.append("messageID='").append( messageID ).append('\'')
//#             .append(", threadedMessageID='").append( threadingMessageID ).append('\'')
//#             .append(", parentID='").append( parentID ).append('\'')
//#             .append(", parentIDs=").append( parentIDs )
//#             .append(", subject='").append( subject ).append('\'')
//#             .append(", isEmpty=").append( isEmpty )
//#             .append(", deleted=").append( deleted )
//#             .append(']');
//#         return buff.toString();
//#     }
    //#endif

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + DBStatus;
        result = prime * result
                + ((accountID == null) ? 0 : accountID.hashCode());
        result = prime * result
                + ((bodyParts == null) ? 0 : bodyParts.hashCode());
        result = prime * result
                + ((boundary == null) ? 0 : boundary.hashCode());
        result = prime * result + ((box == null) ? 0 : box.hashCode());
        result = prime * result + (deleted ? 1231 : 1237);
        result = prime * result + (flagged ? 1231 : 1237);
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + (isEmpty ? 1231 : 1237);
        result = prime * result + messageFormat;
        result = prime * result
                + ((messageID == null) ? 0 : messageID.hashCode());
        result = prime * result
                + ((threadingMessageID == null) ? 0 : threadingMessageID.hashCode());
        result = prime * result + orgLocation;
        result = prime * result
                + ((parentID == null) ? 0 : parentID.hashCode());
        result = prime * result
                + ((parentIDs == null) ? 0 : parentIDs.hashCode());
        result = prime * result + readStatus;
        result = prime * result
                + ((recipients == null) ? 0 : recipients.hashCode());
        result = prime * result + recordID;
        result = prime * result + ((replyTo == null) ? 0 : replyTo.hashCode());
        //#ifdef MUJMAIL_SEARCH
        result = prime * result
                + ((searchResult == null) ? 0 : searchResult.hashCode());
        //#endif
        result = prime * result + sendStatus;
        result = prime * result + size;
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        result = prime * result + (int) (time ^ (time >>> 32));
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        MessageHeader other = (MessageHeader) obj;
        if (DBStatus != other.DBStatus)
            return false;
        if (accountID == null) {
            if (other.accountID != null)
                return false;
        } else if (!accountID.equals(other.accountID))
            return false;
        if (bodyParts == null) {
            if (other.bodyParts != null)
                return false;
        } else if (!bodyParts.equals(other.bodyParts))
            return false;
        if (boundary == null) {
            if (other.boundary != null)
                return false;
        } else if (!boundary.equals(other.boundary))
            return false;
        if (box == null) {
            if (other.box != null)
                return false;
        } else if (!box.equals(other.box))
            return false;
        if (deleted != other.deleted)
            return false;
        if (flagged != other.flagged)
            return false;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (isEmpty != other.isEmpty)
            return false;
        if (messageFormat != other.messageFormat)
            return false;
        if (messageID == null) {
            if (other.messageID != null)
                return false;
        } else if (!messageID.equals(other.messageID))
            return false;

        if (imapFolder == null) {
            if (other.imapFolder != null)
                return false;
        } else if (!imapFolder.equals(other.imapFolder))
            return false;
        
        if (threadingMessageID == null) {
            if (other.threadingMessageID != null)
                return false;
        } else if (!threadingMessageID.equals(other.threadingMessageID))
            return false;

        if (orgLocation != other.orgLocation)
            return false;
        if (parentID == null) {
            if (other.parentID != null)
                return false;
        } else if (!parentID.equals(other.parentID))
            return false;
        if (parentIDs == null) {
            if (other.parentIDs != null)
                return false;
        } else if (!parentIDs.equals(other.parentIDs))
            return false;
        if (readStatus != other.readStatus)
            return false;
        if (recipients == null) {
            if (other.recipients != null)
                return false;
        } else if (!recipients.equals(other.recipients))
            return false;
        if (recordID != other.recordID)
            return false;
        if (replyTo == null) {
            if (other.replyTo != null)
                return false;
        } else if (!replyTo.equals(other.replyTo))
            return false;
        //#ifdef MUJMAIL_SEARCH
        if (searchResult == null) {
            if (other.searchResult != null)
                return false;
        } else if (!searchResult.equals(other.searchResult))
            return false;
        //#endif
        if (sendStatus != other.sendStatus)
            return false;
        if (size != other.size)
            return false;
        if (subject == null) {
            if (other.subject != null)
                return false;
        } else if (!subject.equals(other.subject))
            return false;
        if (time != other.time)
            return false;
        return true;
    }

}

