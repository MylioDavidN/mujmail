//#condition MUJMAIL_FS
package mujmail;

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

import mujmail.util.Callback;
import mujmail.util.StartupModes;
import mujmail.ui.FileSystemBrowser;
import java.util.Vector;
import javax.microedition.lcdui.Command;

/**
 * Handles adding attachments to the email. 
 * Adds user interface controls for attaching attachments while writing new 
 * mail to the form SendMail.
 * 
 * Usage: Method importAttachementsFromHeader imports all body parts representing 
 * attachments from given (edited) message. It is created only shallow
 * copy of such body parts. Than, user interface enables to add and remove
 * body parts. Method exportAttachementsToHeader creates deep copies of such
 * body parts and adds it to the message.
 * 
 * 
 * @see SendMail
 * @author David Hauzar <david.hauzar@seznam.cz>, 
 *  based on file attachment patch by John Dorfman
 */
public class FSAttachmentsAdder {
    
    private Vector attachments = new Vector();
    private SendMail sendMail;
    
    /** The message header used for creating body parts representing attachments. */
    private MessageHeader tempHeader;
    
    /** The command used when dialog for attaching files should be started */
    public Command attach;
    /** The command used when dialog for removing attachments should be started */
    public Command remove;
    
    /**
     * Creates new instance of file system attachments adder.
     * @param sendMail the form to which the user interface for attaching files
     *  will be added
     */
    public FSAttachmentsAdder(SendMail sendMail) {
        this.sendMail = sendMail;
        
        tempHeader = new MessageHeader(sendMail.mujMail.outBox);
        
        // TODO: if there is not JSR75, commands should be not active
        attach = new Command(Lang.get(Lang.BTN_SM_ADD_ATTACHMENT), Command.ITEM, 8);
        sendMail.addCommand(attach);
        remove = new Command(Lang.get(Lang.BTN_SM_REMOVE_ALL_ATTACHMENTS), Command.ITEM, 9);
        sendMail.addCommand(remove);
        

    }
    
    /**
     * Adds attachments to given message header.
     * Makes deep copies of attachments.
     * @param header the header to which add the attachments
     */
    public void exportAttachmentsToHeader(MessageHeader header) {
        System.out.println("Exporting attachments");
        for (int i = 0; i < getAttachments().size(); i++) {
            System.out.println(i);
            header.addBodyPart( 
                    new BodyPart( header, (BodyPart)getAttachments().elementAt(i), ContentStorage.CopyingModes.DEEP_COPY  ));
        }
        System.out.println("Attachments exported");
    }   
    
    /**
     * Adds new attachment to the attachment adder and displays it in connected
     * SendMail form.
     * 
     * @param attachment
     */
    public void addAttachment(BodyPart attachment) {
        
        
        getAttachments().addElement(attachment);
        attachment.getSendMailDisplayer().addAttachmentToForm(getSendMail());
        
        MujMail.mujmail.getDisplay().setCurrent(getSendMail());
    }
    
    /**
     * Removes all attachments from connected SendMail form and from this
     * attachment adder.
     * It assumes that attachments are the last items in SendMail form!!!
     */
    public void removeAllAttachments() {
        System.out.println("Attachments to remove: " + getAttachments().size());
        removeAllAttachmentsFromForm();
        
        getAttachments().removeAllElements();
        
        MujMail.mujmail.getDisplay().setCurrent(getSendMail());
    }
    
    /**
     * It assumes that attachments are the last items in SendMail form!!!
     */
    private void removeAllAttachmentsFromForm() {
        int itemsToRemove = getAttachments().size()*BodyPart.SendMailDisplayer.numberOfItemsInForm;
        for (int i = 0; i < itemsToRemove; i++) {
            getSendMail().delete(getSendMail().size()-1);
        }
    }
    
    private void addAllAttachmentsToForm() {
        for (int i = 0; i < getAttachments().size(); i++) {
            ((BodyPart)getAttachments().elementAt(i)).getSendMailDisplayer().
                    addAttachmentToForm(getSendMail());
        }
    }
    
    /**
     * Removes the attachment with given index from attachment adder and also
     * from connected SendMail form.
     * @param i 	index of attachment to remove from attachment adder
     */
    public void removeAttachment(int i) {
        getAttachments().removeElementAt(i);
        
        removeAllAttachmentsFromForm();
        addAllAttachmentsToForm();
        
        MujMail.mujmail.getDisplay().setCurrent(getSendMail());
    }
    
    /**
     * Gets temporary header - the header used when creating new body parts.
     * @return temporary header
     */
    MessageHeader getTempHeader() {
        return tempHeader;
    }
    
    /**
     * Adds attachments from given header to this attachments adder and displays
     * it in connected SendMail form.
     * Makes only shallow copy. Deep copies of body parts representing attachments
     * is done while exporting attachments to header.
     * @param header the header to which add attachments
     */
    public void importAttachmentsFromHeader(MessageHeader header) {
        for (int i = 0; i < header.getAttachementCount(); i++) {
            BodyPart bp = header.getAttachement(i);
            
            addAttachment(new BodyPart(header, bp, ContentStorage.CopyingModes.SHALLOW_COPY));
        }
        
        System.out.println("Imported: " + getAttachments().size());
    }
    
    /**
     * Gets the number of attachments in this attachment adder
     * @return the number of attachments in this attachment adder
     */
    public int getAttachmentsCount() {
        return getAttachments().size();
    }

    public Vector getAttachments() {
        return attachments;
    }

    public void setAttachments(Vector attachments) {
        this.attachments = attachments;
    }

    private SendMail getSendMail() {
        return sendMail;
    }
    
    /**
     * Used to add attached file. Instance of this class is passed to instance of
     * class FileSystemBrowser and called by it when some file is chosen.
     */
    private class FSBrowserOKAction implements Callback {

        public void callback(Object called, Object message) {
            // Add file as attachment
            
            // if user did not choose any file, no action is needed
            if (((String) message).equals("canceled"))  return;
            FileSystemBrowser fsBrowser = (FileSystemBrowser) called;
            
            BodyPart bp = new BodyPart(getTempHeader(), fsBrowser.getSelectedURL(), 
                    fsBrowser.getSelectedFileOrDirName(), fsBrowser.getSelectedFileOrDirName(), 0);
            addAttachment(bp);

            // find file size
            ((FSStorage) bp.getStorage()).updateSizeInNewThread(new UpdateAttachmentSizeCallback(sendMail, bp));
        }
        
    }
    
    /**
     * Instance of this class is called after ContentStorage.updateSizeInNewThread
     * is performed.
     * Updates the StringItem with the size of attachment in send mail form
     * and sets the display to send mail.
     */
    private static class UpdateAttachmentSizeCallback implements Callback {
        private final SendMail sendMail;
        private final BodyPart bodyPart;

        public UpdateAttachmentSizeCallback(SendMail sendMail, BodyPart bodyPart) {
            this.sendMail = sendMail;
            this.bodyPart = bodyPart;
        }

        public void callback(Object called, Object message) {     
            bodyPart.getSendMailDisplayer().updateSize();
            MujMail.mujmail.getDisplay().setCurrent(sendMail);
        }
        
    }
    
    /**
     * Used to choose file to attach by browsing file system.
     */
    public void attachFileSelection() {
        System.out.println("Starting FileSystemBrowser");
        Callback action = new FSBrowserOKAction(); 
        FileSystemBrowser FSBrowser = new FileSystemBrowser(getSendMail().mujMail,getSendMail(), action, 
        		FileSystemBrowser.ChoosingModes.FILES, Lang.get(Lang.FS_BROWSER_SELECT_FILE));
        FSBrowser.startBrowser(StartupModes.IN_NEW_THREAD);
    }
    
    
    /**
     * Holds information about one attached file.
     */
//    public static class AttachmentInfo {
//        /** The number of items of attachment info in sendmail form.
//         Used when removing attachments from the form. */
//        private static int numberOfItemsInForm = 2;
//        private final String attachmentURL;
//        private final String attachmentName;
//        private long attachmentSize = 0;
//        
//        /** The string item in which is displayed the size of the file in Sendmail form */
//        private StringItem siSize = new StringItem(Lang.get(Lang.SM_FILE_SIZE), "");
//        
//        AttachmentInfo(BodyPart.FSStorage fsStorage) {
//            this.attachmentName = fsStorage.getFileName();
//            this.attachmentURL = fsStorage.getFileURL();
//            this.attachmentSize = fsStorage.getSize();
//            setAttachmentSize(attachmentSize);
//        }
//        
//        AttachmentInfo(String attachmentURL, String attachmentName) {
//            this.attachmentName = attachmentName;
//            this.attachmentURL = attachmentURL;
//        }
//        AttachmentInfo(String attachmentURL, String attachmentName, long attachmentSize) {
//            this.attachmentName = attachmentName;
//            this.attachmentURL = attachmentURL;
//            this.attachmentSize = attachmentSize;
//            setAttachmentSize(attachmentSize);
//        }
//        
//        public String getAttachmentURL() {
//            return attachmentURL;
//        }
//        public String getAttachmentName() {
//            return attachmentName;
//        }
//        public long getAttachmentSize() {
//            return attachmentSize;
//        }
//        public void setAttachmentSize(long size) {
//            this.attachmentSize = size;
//            siSize.setText(Functions.formatNumberByteorKByte(size) + "\n");
//        }
//        
//        /** Creates body part which stores information about the i-th attachment.
//         * @param header the header of the message to which created body part will belong
//         * @param name the name of created body part
//         * @return the information about given attachment
//         */
//        public BodyPart createBodyPart(MessageHeader header, String name) {
//            return new BodyPart(header, getAttachmentURL(), getAttachmentName(), 
//                    name, getAttachmentSize());
//        }
//        
//        /**
//         * Displays this attachment in the sendmail form.
//         * @param sendmail the form to which add information about this attachment
//         */
//        public void addAttachmentToForm(SendMail sendmail) {
//            // the name of the attachment
//            StringItem siFileNameAttached = new 
//                    StringItem(Lang.get(Lang.SM_ATTACHMENT), getAttachmentName() + "\n");
//            sendmail.append(siFileNameAttached);
//            
//            // the size of the attachment
//            sendmail.append(siSize);
//        }
//    }

}
