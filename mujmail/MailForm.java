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

import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;

import mujmail.MailForm.BPViewingModes.ViewingModesWithActions;
import mujmail.util.Functions;

//#ifdef MUJMAIL_TOUCH_SCR
import mujmail.pointer.MujMailPointerEventListener;
import mujmail.pointer.MujMailPointerEventProducer;
//#endif

//#ifdef MUJMAIL_HTML
import mujmail.html.Browser;
import mujmail.html.Parser;
//#endif

/**
 * Displays the mail header as well as whole mail.
 */
public class MailForm extends Canvas implements Runnable {

    private static final boolean DEBUG = false;

    static final int COLOR[] = {
        0x00000000,
        0x00CC0000,
        0x00008800,
        0x00000088,
        0x00CC8800,
        0x0000CCCC,
        0x00CC00CC,
        0x00330000,
        0x00003300,
        0x00000033,
    };

    private Displayable prevScreen = this;

    static final int MAX_COLORS = 10;
    static final int BACKGROUND_COLOR = 0x00FFFFFF;
    // graphical modes
    public static final byte MODE_LIST = 0; //when we only list the bodyparts of the mail
    public static final byte MODE_BASIC = 1; //when we display the first viewable bodypart	
    public static final byte MODE_BROWSE = 2; //when we display other bodyparts than the first viewable

    static final byte LOAD_BODY = 0;
    static final byte REDOWNLOAD_BODY = 1;

    private final EventListener pointerEventListener = new EventListener();
    //#ifdef MUJMAIL_TOUCH_SCR
    private final MujMailPointerEventProducer pointerEventTransformer;
    //#endif
    
    byte runMode = LOAD_BODY;
    MujMail mujMail; // TODO (Betlista): this doesn't need to here while Mujmail have public static field mujmail 
    private boolean previewMode = false;
    TheBox callBox; //the box that called this MailForm
    Command back, listAttachments, showAttachment, deleteAttachment, redownAttchment,
            showAddresses, showHeader, addMailToBook, viewConverted;
    //#ifdef MUJMAIL_FS
    Command exportToFS;
    //#endif
    // display the attachement as plain text
    Command displayAsText;
    // exports bodypart to filesystem
    Command exportBPToFS;
    Command forward, reply, replyAll, quotedReply, delete, edit;
    List attchList, mailAdrList;
    Form headerForm;
    MessageHeader msgHeader; //header of the message that is being displayed
    byte bodyPartToRedown; //the particular bodypart that should be redownloaded				
    //stuffs for displaying a text bodypart		
    int currDisplay = 0; //current text page
    byte contextID = MODE_BASIC;
    byte currAttachmentID = 0;
    byte firstViewable = -1;
    /** defines if the bodypart have to be downloaded before reading */
    boolean newTextSelected = false;
    boolean newConvertedTextSelected = false;
    // a Vector of displays that stores the text bodyparts	
    Vector textBodyDisplays;
    // contains a text of a text bodypart, 	
    String textBodyString;
    
    /** The viewing mode of the body part */
    private BPViewingModes bpViewingMode;

    /**
     * Shows previous screen, which reference is stored in {@link #prevScreen}
     * variable. Important thing is to set the {@link #contextID} variable, see
     * the comment in {@link #setContext(byte)} method.
     * 
     * @see #prevScreen
     * @see #contextID
     * @see #setContext(byte)
     */
    public void showPreviousScreen() {
        MujMail.mujmail.getDisplay().setCurrent(prevScreen);
    }

    /**
     * Enumeration class of all possible viewing modes of body parts.
     * It is possible to obtain instance of ViewingModesWithActions form each
     * viewing using method <code>getViewingModeWithActions</code>.
     */
    public static class BPViewingModes {
        /** Used for method <code>toString</code> */
        private final String viewingModeString;

        private BPViewingModes(String viewingModeString) {
            this.viewingModeString = viewingModeString;
        }
        
        /** View body part as plain text. */
        public static final ViewingModesWithActions PLAIN_TEXT = new PlainTextMode("PLAIN TEXT");
        /** View body part as HTML. */
        public static final ViewingModesWithActions HTML = new HTMLMode("HTML");
        /** View converted body part */
        public static final ViewingModesWithActions CONVERTED = new ConvertedMode("CONVERTED");
        /** View multimedia body part. */
        public static final ViewingModesWithActions MULTIMEDIA = new MultimediaMode("MULTIMEDIA");
        /** Do not view this body part. */
        public static final ViewingModesWithActions NO_VIEW = new NoViewMode("NO VIEW");
        /** View body part as text. That means without any converting.
         This viewing mode is converted by method BodyPart.getSpecifiedViewingMode
         to PLAIN_TEXT viewing mode. */
        public static final ViewingModesWithActions AS_TEXT = new AsTextMode("AS TEXT");
        
        /** View body part according of the information contained in body part. 
         * Virtual viewing mode. Method getViewingModeWithActions returns
         * some other viewing mode. */
        public static final BPViewingModes NOT_SPECIFIED = new BPViewingModes("NOT SPECIFIED");

        /**
         * Gets viewing mode with actions.
         * @param bp body part that is actually viewed.
         * @return viewing mode with actions.
         */
        public ViewingModesWithActions getViewingModeWithActions(BodyPart bp) {
            if (this instanceof ViewingModesWithActions) {
                return (ViewingModesWithActions) this;
            }

            return bp.getAutoViewingMode();
        }

        public String toString() {
            return viewingModeString;
        }


        /**
         * Viewing modes with methods representing actions that can be done with
         * viewing modes.
         * There is a subclass of this class for each viewing mode. That is,
         * subclassing is used instead of if-else conditions of type:
         * <code>if (viewingMode == BPViewingModes.PLAIN_TEXT) {...}</code>
         */
        public static class ViewingModesWithActions extends BPViewingModes {
            private ViewingModesWithActions(String viewingModeString) {
                super(viewingModeString);
            }

            protected String getBodyPartContent(MessageHeader msgHeader, byte currAttachmentID) {
                return msgHeader.getBodyPartContent(currAttachmentID);
            }

            protected boolean prepareForViewingBP(BodyPart actBodyPart, byte ID, MailForm mailForm) {
                return true;
            }

            protected void prepareBeforeDisplayingBP(MailForm mailForm) {
            }

            protected boolean displayBodyPart(Graphics g, MailForm mailForm) {
                return false;
            }

            protected String preprocessBodyPartBeforeDisplaying(MailForm mailFrom) {
                return mailFrom.textBodyString;
            }

            protected void setCorrectStateOfMailFormAfterPreparingText(MailForm mailFrom) {
                mailFrom.newTextSelected = false;
            }

            protected void up(MailForm mailForm) {
            }

            protected void down(MailForm mailForm) {
            }

            protected void initDisplayingVariables(MailForm mailForm) {
            }

            protected boolean isViewable(BodyPart actBodyPart, Displayable attchList) {
                return true;
            }
            
        }

        /**
         * Class with actions of viewing mode PLAIN_TEXT.
         */
        private static class PlainTextMode extends ViewingModesWithActions {
            private PlainTextMode(String viewingModeString) {
                super(viewingModeString);
                if (DEBUG) System.out.println("DEBUG PlainTextMode.PlainTextMode()");
            }

            protected void initDisplayingVariables(MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG PlainTextMode.initDisplayingVariables(MailForm)");
                // init displaying variables
                mailForm.newTextSelected = true;
                mailForm.currDisplay = 0;
            }

            protected void down(MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG PlainTextMode.down(MailForm)");
                // skip to the next line
                mailForm.currDisplay = (mailForm.currDisplay + (1) + mailForm.textBodyDisplays.size()) % mailForm.textBodyDisplays.size();
                mailForm.repaint();
                mailForm.serviceRepaints();
            }

            protected void up(MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG PlainTextMode.up(MailForm)");
                // skip to the previous line
                mailForm.currDisplay = (mailForm.currDisplay + (-1) + mailForm.textBodyDisplays.size()) % mailForm.textBodyDisplays.size();
                mailForm.repaint();
                mailForm.serviceRepaints();
            }

            protected boolean displayBodyPart(Graphics g, MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG MailForm - PlainTextMode.displayBodyPart(Graphics, MailForm)");
                //BodyPart actBodyPart = mailForm.msgHeader.getBodyPart(mailForm.currAttachmentID);
                try {
//                    if ( mailForm.msgHeader.getBpType(mailForm.currAttachmentID) == BodyPart.TYPE_TEXT 
//                    	 || mailForm.msgHeader.get(mailForm.currAttachmentID) ) {
                        mailForm.displayPlainTextBodyPart( g );
//                    } else if ( mailForm.msgHeader.getBpType(mailForm.currAttachmentID) == BodyPart.TYPE_HTML ) {
//                        mailForm.displayHTMLBodyPart( g );
//                    } else {
//                        
//                    }
                } catch (MyException ex) {
                    ex.printStackTrace();
                    return false;
                }

                return true;
            }

            protected void prepareBeforeDisplayingBP(MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG PlainTextMode.prepareBeforeDisplayingBP(MailForm)");
                //if a different text body part than the first viewable was chosen
                if (mailForm.currAttachmentID != mailForm.firstViewable ) {
                    mailForm.newTextSelected = true;
                } //then we have to reparse the text
            }

            protected boolean prepareForViewingBP(BodyPart actBodyPart, byte ID, MailForm mailForm) {
            	if (DEBUG) System.out.println("DEBUG PlainTextMode.prepareForViewingBP(...)");
                // if a different text bodypart was chosen
                if (ID != mailForm.currAttachmentID || !mailForm.newConvertedTextSelected) {
                    mailForm.newTextSelected = true;
                } //set the flag to null in order to parse a new text
                mailForm.currDisplay = 0;

                return true;
            }
        }

        /**
         * Class with actions of viewing mode AS_TEXT.
         */
        private static class AsTextMode extends PlainTextMode {
            private AsTextMode(String viewingModeString) {
                super(viewingModeString);
            }

            protected String preprocessBodyPartBeforeDisplaying(MailForm mailFrom) {
                return mailFrom.textBodyString;
            }

//            protected String getBodyPartContent(MessageHeader msgHeader, byte currAttachmentID) {
//                if (msgHeader.getBodyPart(currAttachmentID).getStorage().isContentRaw()) {
//                    return msgHeader.getBodyPart(currAttachmentID).getStorage().getContentRawAsString();
//                } else {
//                    return super.getBodyPartContent(msgHeader, currAttachmentID);
//                }
//            }
        }

        /**
         * Class with actions of viewing mode CONVERTED.
         */
        private static class ConvertedMode extends PlainTextMode {

            private ConvertedMode(String viewingModeString) {
                super(viewingModeString);
            }

            protected void setCorrectStateOfMailFormAfterPreparingText(MailForm mailFrom) {
                mailFrom.newConvertedTextSelected = false;
            }

            protected boolean displayBodyPart(Graphics g, MailForm mailForm) {
                if (DEBUG) System.out.println("DEBUG - MailForm.CONVERTED_MODE.displayBodyPart");
                // if CONVERTED view mode was selected set it back to PLAIN_TEXT
                // in order to avoid conversion of previously displayed bodypart
                // when Back command is selected
                boolean retVal = super.displayBodyPart(g, mailForm);
                
                mailForm.bpViewingMode = BPViewingModes.PLAIN_TEXT;
                return retVal;
            }

            protected void prepareBeforeDisplayingBP(MailForm mailForm) {
                if (mailForm.currAttachmentID != mailForm.firstViewable) {
                    mailForm.newConvertedTextSelected = true;
                } //then we have to reparse the text
            }

            protected boolean prepareForViewingBP(BodyPart actBodyPart, byte ID, MailForm mailForm) {
                //if a different bodypart was chosen
                if (ID != mailForm.currAttachmentID || !mailForm.newTextSelected) {
                    mailForm.newConvertedTextSelected = true;
                } //set the flag to null in order to parse a new text
                mailForm.currDisplay = 0;

                return true;
            }

            protected String getBodyPartContent(MessageHeader msgHeader, byte currAttachmentID) {
                return Functions.removeTags( msgHeader.getConvertedBodyPartContent(currAttachmentID) );
            }
        }

        /**
         * Class with actions of viewing mode MULTIMEDIA.
         */
        private static class MultimediaMode extends ViewingModesWithActions {
            private MultimediaMode(String viewingModeString) {
                super(viewingModeString);
            }

            protected boolean displayBodyPart(Graphics g, MailForm mailForm) {
                mailForm.displayMultimediaBodyPart(g);

                return true;
            }

            protected String getBodyPartContent(MessageHeader msgHeader, byte currAttachmentID) {
                // multimedia content is raw data, so does not return anything.
                return "";
            }


        }

        /**
         * Class with actions of viewing mode NO_VIEW_MODE.
         */
        private static class NoViewMode extends ViewingModesWithActions {
            private NoViewMode(String viewingModeString) {
                super(viewingModeString);
            }

            protected boolean prepareForViewingBP(BodyPart actBodyPart, byte ID, MailForm mailForm) {
                MujMail.mujmail.alert.setAlert(mailForm, mailForm.attchList, Lang.get(Lang.ALRT_MF_UNSUPPORTED_FORMAT), MyAlert.DEFAULT, AlertType.ERROR);
                return false;
            }

            protected boolean isViewable(BodyPart actBodyPart, Displayable attchList) {
                MujMail.mujmail.alert.setAlert(this, attchList, Lang.get(Lang.ALRT_MF_UNSUPPORTED_FORMAT), MyAlert.DEFAULT, AlertType.ERROR);
                return false;
            }
        }

        /**
         * Class represents mode for viewing HMTL content.
         * 
         * @author Betlista
         */
        private static class HTMLMode extends PlainTextMode {
        	public HTMLMode(String viewingModeString) {
				super(viewingModeString);
			}
        	
        	protected boolean displayBodyPart(Graphics g, MailForm mailForm) {
        		  if (DEBUG) System.out.println("DEBUG HTMLMode.displayBodyPart(Graphics, MailForm) - start");
        		try {
                                //#ifdef MUJMAIL_HTML
                                mailForm.displayHTMLBodyPart( g );
                                //#else
//#                                 mailForm.displayPlainTextBodyPart( g );
                                //#endif
                                mailForm.displayPlainTextBodyPart(g); // TODO remove tags
                                
        		} catch (Throwable t) {
        			System.err.println("Problem displaying HTML:");
        			System.out.println("--- HTML START ---");
        			System.out.println( mailForm.textBodyString );
        			System.out.println("--- HTML END ---");
        			t.printStackTrace();
        		}
        		return true;
        	}

            protected String preprocessBodyPartBeforeDisplaying(MailForm mailFrom) {
                //#ifdef MUJMAIL_HTML
                return mailFrom.textBodyString;
                //#else
//#                 return Functions.removeTags(mailFrom.textBodyString);
                //#endif
            }
        }
    }

    private void updateEditButton() {
        removeCommand(edit);
        if (callBox instanceof InBox) {
            edit = new Command("Bounce", Command.ITEM, 10);
        } else {
            edit = new Command(Lang.get(Lang.BTN_EDIT), Command.ITEM, 10);
        }
        addCommand(edit);
    }
    
    private void displayMultimediaBodyPart(Graphics g) {
        String extension = msgHeader.getBpExtension(currAttachmentID).toLowerCase();
        if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("gif") || extension.equals("png")) {
            drawImage(g);
        }
    }

    private void displayPlainTextBodyPart(Graphics g) throws MyException {
        if (newConvertedTextSelected || newTextSelected) 
        {
            prepareNewText(g);
            // TODO: on some slow phones repaint was needed: test on more phones
            // now and eventually uncomment it
            repaint();
        }
        // draw the body part, which has been already parsed and stored to "textBodyDisplays" variable
        drawText(textBodyString, textBodyDisplays, g);
    }

    //#ifdef MUJMAIL_HTML
    private void displayHTMLBodyPart( final Graphics g ) {
          if (DEBUG) System.out.println("DEBUG MailForm.displayHTMLBodyPart(Graphics)");
        BPViewingModes.ViewingModesWithActions viewingMode = getBPViewingModeWithActions();
     	textBodyString = viewingMode.getBodyPartContent(msgHeader, currAttachmentID);

        if (DEBUG) System.out.println("text:\n" + textBodyString );

        Parser parser = new Parser( textBodyString );
      
        Vector elements = parser.parse();
        prevScreen = mujMail.getDisplay().getCurrent();
        Browser browser = new Browser( elements );
        mujMail.getDisplay().setCurrent( browser );
    }
    //#endif

    /**
     * Gets body part viewing mode with actions.
     * @return body part viewing mode with actions.
     */
    private ViewingModesWithActions getBPViewingModeWithActions() {
        return bpViewingMode.getViewingModeWithActions(msgHeader.getBodyPart(currAttachmentID));
    }


    /**
     * Loads the text of the message to be displayed and prepares textBodyString 
     * and textBodyDisplays to displaying.
     * @param g
     * @throws mujmail.MyException if loading of text was not successful
     */
    private void prepareNewText(Graphics g) throws MyException {
        //DebugConsole.println("Prepare new text");
        BPViewingModes.ViewingModesWithActions viewingMode = getBPViewingModeWithActions();
        try {
            if (DEBUG) System.out.println("DEBUG - MailForm.prepareNewText - getting body part content, Viewing mode = " + viewingMode);
            textBodyString = viewingMode.getBodyPartContent(msgHeader, currAttachmentID);

            if (textBodyString == null) {
                throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
            }

            textBodyString = viewingMode.preprocessBodyPartBeforeDisplaying(this);

            if (Settings.replaceTabs) {
                textBodyString = textBodyString.replace('\t', ' ');
            }
        } catch (OutOfMemoryError e) {
        	e.printStackTrace();
            back();
            mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_VIEW_ATT) + Lang.get(Lang.FAILED) + " " + Lang.get(Lang.ALRT_SYS_LOW_MEMORY), MyAlert.DEFAULT, AlertType.ERROR);
        } catch (MyException ignored) {
        	ignored.printStackTrace();
        }

        if (textBodyString == null) {
            Displayable nextDisp;
            if (contextID == MODE_BROWSE) {
                nextDisp = attchList;
            } else {
                nextDisp = callBox;
            } //we need to return the focus back to the box
            mujMail.alert.setAlert(this, nextDisp, Lang.get(Lang.ALRT_MF_LOAD_BODY) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
            throw new MyException(0);
        }

        if (textBodyString.equals("")) {
            textBodyString = "<" + Lang.get(Lang.ML_BODY) + Lang.get(Lang.EMPTY) + ">\r\n.\r\n";
        }
        
        textBodyDisplays = parseTextToDisplay(textBodyString, g);
        viewingMode.setCorrectStateOfMailFormAfterPreparingText(this);
    }

    private void setFont(Graphics g) {
        if (Settings.smallFontMailForm) {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        } else {
            g.setFont(Font.getDefaultFont());
        }
    }

    private class TextDisplayLine {

        int beginLn = 0;
        int endLn = 0;
        byte countReplies = 0;

        public TextDisplayLine() {
        }

        public TextDisplayLine(int beginLn, int endLn, byte countReplies) {
            this.beginLn = beginLn;
            this.endLn = endLn;
            this.countReplies = countReplies;
        }
    }
    
    /**
     * Listens to the transformed pointer events produced by 
     * MujMailPointerEventProducer.
     * Used also for handling appropriate key pressed events.
     */
    //#ifdef MUJMAIL_TOUCH_SCR
    private class EventListener extends  MujMailPointerEventListener.MujMailPointerEventListenerAdapter {
    //#else
//#     private class EventListener {
    //#endif
        public void left() {
            mujMail.commandAction(back, MailForm.this);
        }

        public void right() {
            changeFontSize();
        }

        public void up() {
            getBPViewingModeWithActions().up(MailForm.this);
        }

        public void down() {
            getBPViewingModeWithActions().down(MailForm.this);
        }

        public void upQuartersSlash() {
            up();
        }

        public void downQuartersStar() {
            down();
        }

        public void fire() {
            changeFontSize();
        }
        
    }

    /**
     * @param mujMail - the main application object.
     */
    public MailForm(MujMail mujMail) {
        this.mujMail = mujMail;
        //mailForm
        back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        reply = new Command(Lang.get(Lang.BTN_TB_REPLY), Command.OK, 2);
        delete = new Command(Lang.get(Lang.BTN_DEL_UNDEL), Command.ITEM, 3);
        showHeader = new Command(Lang.get(Lang.BTN_MF_HEADERS_DETAILS), Command.ITEM, 4);
        //#ifdef MUJMAIL_FS
        exportToFS = new Command(Lang.get(Lang.BTN_MF_EXPORT_MAIL_TO_FS), Command.ITEM, 10);
        //#endif
        listAttachments = new Command(Lang.get(Lang.BTN_MF_ATTACHMENTS), Command.ITEM, 5);
        forward = new Command(Lang.get(Lang.BTN_TB_FORWARD), Command.ITEM, 6);
        //bounce = new Command("Bounce", Command.ITEM, 11);
        quotedReply = new Command(Lang.get(Lang.BTN_TB_QUOTED_REPLY), Command.ITEM, 7);
        replyAll = new Command(Lang.get(Lang.BTN_TB_REPLY_ALL), Command.ITEM, 8);
        showAddresses = new Command(Lang.get(Lang.BTN_MF_EMAIL_ADDRS), Command.ITEM, 9);
        //address list
        addMailToBook = new Command(Lang.get(Lang.BTN_MF_ADD_EMAIL), Command.ITEM, 1);
        //attachments list
        deleteAttachment = new Command(Lang.get(Lang.BTN_MF_DEL_ATTACHMENTS), Command.ITEM, 1);

        addCommand(delete);
        addCommand(forward);
        addCommand(back);
        addCommand(listAttachments);
        addCommand(showHeader);
        addCommand(showAddresses);
        //#ifdef MUJMAIL_FS
        addCommand(exportToFS);
        //#endif
        //addCommand(bounce);
        setCommandListener(mujMail);
    
        //#ifdef MUJMAIL_TOUCH_SCR
        pointerEventTransformer = new MujMailPointerEventProducer(
                pointerEventListener, getWidth(), getHeight());
        //#endif
    }
    
    private void changeFontSize() {
        Settings.smallFontMailForm = !Settings.smallFontMailForm;
        mujMail.getSettings().updateValuesToForms();
        mujMail.getSettings().saveSettings(true);
        viewMessage(msgHeader, callBox);
    }
    
    //#ifdef MUJMAIL_FS
    /**
     * Exports the mail to filesystem.
     */
    public void exportToFilesystem() {
        
        FileSystemMailExporter exporter = new FileSystemMailExporter(mujMail);
        exporter.exportMail(callBox, msgHeader);
    }
    //#endif
    
    /**
     * Shows a Form with actualized header information
     * @param header header that details will be shown
     * @param prevScreen the screen that will be displayed after user presses
     *  back button.
     */
    public void showHeader(MessageHeader header, Displayable prevScreen) {
          if (DEBUG) System.out.println( "DEBUG MailForm.showHeader(MessageHeader, Displayable) - displayable: " + prevScreen.getClass().toString() );
        this.prevScreen = prevScreen;
        msgHeader = header;
        setContext(MODE_BROWSE);

        if (headerForm == null) {
            headerForm = new Form(Lang.get(Lang.ML_HEADERS_DETAIL));
        }

        // remove old Items
        for (int i = headerForm.size(); i > 0; --i) {
            headerForm.delete(i - 1);
        }

        // append new ones
        String sender = Functions.emailOnly(header.getFrom());
        if (mujMail.getAddressBook().getContactName(sender) != null) //if the sender is in adressbook, show his full name
        {
            sender = mujMail.getAddressBook().getContactName(sender) + " <" + sender + ">";
        } else {
            sender = header.getFrom();
        }
        headerForm.append(new StringItem(Lang.get(Lang.ML_FROM) + " ", sender));
        headerForm.append(new StringItem(Lang.get(Lang.ML_RECIPIENTS) + " ", header.getRecipients() ));
        headerForm.append(new StringItem(Lang.get(Lang.ML_SUBJECT) + " ", header.getSubject()));
        headerForm.append(new StringItem(Lang.get(Lang.ML_TIME) + " ", header.getTimeStr()));
        headerForm.append(new StringItem(Lang.get(Lang.ML_ATTACHMENTS) + " ", String.valueOf(header.getBodyPartCount())));

        headerForm.append(new StringItem(Lang.get(Lang.ML_SIZE) + " ", (header.getSize() < 10000 ? header.getSize() + " B" : (header.getSize() >> 10) + " kB")));


        headerForm.addCommand(back);
        mujMail.getDisplay().setCurrent(headerForm);
        headerForm.setCommandListener(mujMail);
    }

    //used to redownload a particular bodypart and then display the attachments list
    public void regetAndList(MessageHeader header, byte index) {
        if (header != null && index >= 0 && index < header.getBodyPartCount()) {
            msgHeader = header;
            bodyPartToRedown = header.getBpOriginalOrder(index);
            // TODO: (david) this will work with user folders??
            callBox = mujMail.getInBox();
            runMode = REDOWNLOAD_BODY;
            Thread t = new Thread(this);
            t.start();
            t.setPriority(Thread.MAX_PRIORITY);
        }
    }

    /**
     * Lists all attachments of the message
     */
    public void listAttachments() {

        // create a new object of List if necessary		
        if (attchList == null) {

            attchList = new List(Lang.get(Lang.BTN_MF_ATTACHMENTS), Choice.IMPLICIT);
            deleteAttachment = new Command(Lang.get(Lang.BTN_MF_DEL_ATTACHMENTS), Command.ITEM, 2);
            redownAttchment = new Command(Lang.get(Lang.BTN_MF_REDOWN_ATTACHMENTS), Command.ITEM, 3);
            displayAsText = new Command(Lang.get(Lang.BTN_MF_DISPLAY_AS_TEXT), Command.ITEM, 4);
            exportBPToFS = new Command(Lang.get(Lang.BTN_MF_EXPORT_BP_TO_FS), Command.ITEM, 5);
            viewConverted = new Command(Lang.get(Lang.BTN_MF_VIEW_CONVERTED), Command.ITEM, 6);

            if (msgHeader.getBox() == mujMail.getInBox()) {
                attchList.addCommand(redownAttchment);
            }
            attchList.addCommand(back);
            attchList.addCommand(deleteAttachment);
            attchList.addCommand(displayAsText);
            attchList.addCommand(exportBPToFS);
            attchList.addCommand(viewConverted);
            attchList.setCommandListener(mujMail);
        }

        for (int i = attchList.size(); i > 0; i--) {
            attchList.delete(i - 1);
        }

        System.out.println(msgHeader);
        int bodyPartCount = msgHeader.getBodyPartCount();
        for (byte i = 0; i < bodyPartCount; i++) {
            // choose different icons according to the bodypart type
            String prefix = msgHeader.getBpState(i) == BodyPart.BS_COMPLETE ? "" : " [!] ";
            switch (msgHeader.getBpType(i)) {
                case BodyPart.TYPE_TEXT:
                    attchList.append(prefix + msgHeader.getBodyPart(i).getHeader().getName() + " (" +
                            (msgHeader.getBodyPart(i).getSize() > 1024 ? (msgHeader.getBodyPart(i).getSize() >> 10) + "kB" : msgHeader.getBodyPart(i).getSize() + "B") + ")",
                            Functions.getIcon("attch_txt.png"));
                    break;
                case BodyPart.TYPE_MULTIMEDIA:
                    attchList.append(prefix + msgHeader.getBodyPart(i).getHeader().getName() + " (" +
                            (msgHeader.getBodyPart(i).getSize() > 1024 ? (msgHeader.getBodyPart(i).getSize() >> 10) + "kB" : msgHeader.getBodyPart(i).getSize() + "B") + ")",
                            Functions.getIcon("attch_image.png"));
                    break;
                case BodyPart.TYPE_HTML:
                    attchList.append(prefix + msgHeader.getBodyPart(i).getHeader().getName() + " (" +
                            (msgHeader.getBodyPart(i).getSize() > 1024 ? (msgHeader.getBodyPart(i).getSize() >> 10) + "kB" : msgHeader.getBodyPart(i).getSize() + "B") + ")",
                            Functions.getIcon("attch_html.png"));
                    break;
                case BodyPart.TYPE_APPLICATION:
                case BodyPart.TYPE_OTHER:
                    attchList.append(prefix + msgHeader.getBodyPart(i).getHeader().getName() + " (" +
                            (msgHeader.getBodyPart(i).getSize() > 1024 ? (msgHeader.getBodyPart(i).getSize() >> 10) + "kB" : msgHeader.getBodyPart(i).getSize() + "B") + ")",
                            Functions.getIcon("attch_unknown.png"));
                    break;

            }
        }
        mujMail.getDisplay().setCurrent(attchList);
    }

    /**
     * Shows a list of e-mail addresses in a MULTIPLE list.
     */
    public void listMailAddr() {
        if (mailAdrList == null) {
            mailAdrList = new List(Lang.get(Lang.BTN_MF_EMAIL_ADDRS), Choice.MULTIPLE);
            mailAdrList.addCommand(back);
            mailAdrList.addCommand(addMailToBook);
            mailAdrList.setCommandListener(mujMail);
        }

        // delete old items
        for (int i = mailAdrList.size(); i > 0; i--) {
            mailAdrList.delete(i - 1);
        }

        // refresh address' Vector and List
        Vector addr = Functions.parseRcp(msgHeader.getRecipients() );
        addr.addElement(Functions.emailOnly(msgHeader.getFrom()));

        for (int i = 0; i < addr.size(); i++) {
            mailAdrList.append((String) addr.elementAt(i), null);
        }
        mujMail.getDisplay().setCurrent(mailAdrList);
    }

    /**
     * Save all checked email addresses in the <code>mailAdrList</code>.
     */
    public void saveContacts() {
        boolean[] checked = new boolean[mailAdrList.size()];
        mailAdrList.getSelectedFlags(checked);
        AddressBook.Contact contact;
        for (int i = 0; i < mailAdrList.size(); ++i) {
            if (checked[i]) {
                contact = new AddressBook.Contact("", mailAdrList.getString(i), "");
                try {
                    mujMail.getAddressBook().saveContact(contact);
                } catch (MyException ex) {
                }
            }
        }
        mujMail.getDisplay().setCurrent(this);
    }

    /**
     * This method completely deletes a method attachment, that is chosen. This bodypart must be deleted from both
     * <code> bodyParts Vector </code> and <code> MailDb </code> that is done by <code>deleteBodyPart</code> method.
     * @param ID of the attachment. It is attchList.getSelectedIndex + 1.
     * @see MessageHeader
     */
    public void deleteBodyPart(byte ID) {
        synchronized (msgHeader) {
            byte bodyPartCount = msgHeader.getBodyPartCount();
            if (bodyPartCount == 0) {
                mujMail.alert.setAlert(this, this, Lang.get(Lang.ALRT_MF_NO_ATTACHMENTS), MyAlert.DEFAULT, AlertType.ERROR);
                return;
            }
            if (0 <= ID && ID < bodyPartCount) {
                try {
                    msgHeader.getBodyPart(ID).getStorage().deleteContent();
                    attchList.delete(ID);
                    msgHeader.removeBodyPart(ID);
                    //to prevent accessing non-existing bodypart
                    if (firstViewable == ID) {
                        firstViewable = -1;
                    }
                    if (currAttachmentID == ID) {
                        currAttachmentID = firstViewable;
                    }
                    
                    msgHeader.saveHeader(); //we have to update info about msgHeader as well
                } catch (MyException ex) {
                    mujMail.alert.setAlert(attchList, callBox, Lang.get(Lang.ALRT_MF_DEL_ATT) + Lang.get(Lang.FAILED) + ex.getDetails(), MyAlert.DEFAULT, AlertType.ERROR);
                    msgHeader.notify();
                    return;
                }
                mujMail.alert.setAlert(attchList, attchList, Lang.get(Lang.ALRT_MF_DEL_ATT) + Lang.get(Lang.SUCCESS), MyAlert.DEFAULT, AlertType.INFO);
            }
            msgHeader.notify();
        }
    }

    /**
     * Thread method that serves to load body either from MailDB or from remote server.
     */
    public void run() {
        setContext(MODE_LIST);
        switch (runMode) {
            case LOAD_BODY:
                try {
                    if (loadBody(msgHeader) == null) { //try to load the body of the mail						
                        mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_LOAD_BODY) + Lang.get(Lang.FAILED), MyAlert.DEFAULT, AlertType.ERROR);
                        return;
                    }
                    //only inBox needs a counter for unread mails
                    if (msgHeader.readStatus == MessageHeader.NOT_READ && msgHeader.getBox() == mujMail.getInBox()) {
                        msgHeader.readStatus = MessageHeader.READ;

                        //Seen flag is set automatically when
                        //FETCH BODY is called -> no need to set \Seen flag
                        /*
                        MailAccount msgAcct = (MailAccount)mujMail.mailAccounts.get(msgHeader.accountID);
                        
                        //Set '\Seen' flag on server if it's an IMAP account
                        if (msgAcct.type == MailAccount.IMAP)
                        {
                        	msgAcct.protocol.setFlags(msgHeader, "(\\Seen)");
                        }
                        */
                        
                        try {
                            msgHeader.saveHeader(); //update new data into DBase
                        } catch (MyException me) {
                        }
                        ((InBox) msgHeader.getBox()).changeUnreadMails(-1);
                    }
                    firstViewable = getFirstViewableBody(msgHeader); //find the first viewable bodypart;
                    if (firstViewable == -1) { //non are viewable																		
                        listAttachments();
                        attchList.addCommand(showHeader);
                        attchList.addCommand(showAddresses);
                        attchList.addCommand(delete);
                        attchList.addCommand(forward);
                        attchList.addCommand(edit);
                        
                        //#ifdef MUJMAIL_FS
                        attchList.addCommand(exportToFS);
                        //#endif
                        mujMail.alert.setAlert(this, attchList, Lang.get(Lang.ALRT_MF_NO_VIEWABLE_BODY), MyAlert.DEFAULT, AlertType.INFO);
                    } else { //there are some viewable bodyparts
                        BodyPart bp = msgHeader.getBodyPart(firstViewable);
                        currAttachmentID = firstViewable;
                        setContext(MODE_BASIC);
                        bp.getAutoViewingMode().initDisplayingVariables(this);
                        
                        repaint();

                        mujMail.getDisplay().setCurrent(this);
                    }
                } catch (Exception ex) {
                    back();
                    mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_LOAD_BODY) + Lang.get(Lang.FAILED) + ex, MyAlert.DEFAULT, AlertType.ERROR);
                } catch (Error e) {
                    back();
                    mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_LOAD_BODY) + Lang.get(Lang.FAILED) + e, MyAlert.DEFAULT, AlertType.ERROR);
                }
                break;

            case REDOWNLOAD_BODY:
                synchronized (msgHeader) {
                    mujMail.getInBox().regetBody(msgHeader, bodyPartToRedown);
                    mujMail.getDisplay().setCurrent(callBox);
                    try {
                        msgHeader.wait();
                    } catch (Exception ignored) {
                    }
                }
                listAttachments();
                break;
        }
    }

    /**
     * Preview the message that user immediately writes.
     * When button back is pressed, user can continue writing the message.
     */
    public void previewMessage() {
        previewMode = true;
        // the message is saved to outbox, it must be deleted when BACK button
        // is pressed!!
        viewMessage(mujMail.sendMail.createAndSaveMessage(mujMail.outBox), null);
    }

    /**
     * <p>
     * Is used to display body of a message with all attachments. Firstly, it 
     * shows just a textual part of a body. If the message contains some other 
     * attachments, the Canvas will have listAttachments Command to display it.
     * <p>
     * Must be run in separate thread.
     *
     * @param mh - {@link MessageHeader} that will be displayed.
     * @param callBox - this is a screen, that will be shown after an user clicks back button
     * Viewing must be run a in different thread, other it would cause the mujmail main thread unable to
     * perform any other vital tasks - commandListening, displaying dialog windows!!!
     */
    public void viewMessage(MessageHeader msgHeader, TheBox callBox) {
        if (msgHeader == null) {
            return;
        }
        bpViewingMode = BPViewingModes.NOT_SPECIFIED;
        
        this.callBox = callBox;
        this.msgHeader = msgHeader;
        runMode = LOAD_BODY;
        Thread t = new Thread(this);
        t.start();
        t.setPriority(Thread.MAX_PRIORITY);
    }
    
    //#ifdef MUJMAIL_FS
    /**
     * Exports given bodypart to filesystem.
     * @param ID ID of the BodyPart to be exported.
     */
    void exportBPToFS(byte ID) {
        FileSystemBodyPartExporter.exportBPToFS(this, msgHeader, ID);
    }
    //#endif

    /**
     * Displays the chosen bodypart.
     * @param ID ID of the BodyPart.
     * @param viewingMode the viewing mode of the body part
     */
    public void viewBodyPart(byte ID, BPViewingModes viewingMode) {
    	  if (DEBUG) System.out.println("DEBUG MailForm.viewBodyPart(ID=" + ID + ", viewingMode=" + viewingMode + ")");
        BodyPart actBodyPart = msgHeader.getBodyPart(ID);
          if (DEBUG) System.out.println("DEBUG MailForm.viewBodyPart(byte, BPViewingModes) - " + actBodyPart );
        bpViewingMode = viewingMode;

        if ( !getBPViewingModeWithActions().isViewable(actBodyPart, attchList) ) {
        	  if (DEBUG) System.out.println( "DEBUG MailForm.viewBodyPart(byte, BPViewingModes) - BodyPart is not viewable" );
        	return;
        }

        if (0 <= ID && ID < msgHeader.getBodyPartCount()) {
            if (ID != firstViewable) //another bodypart than firstViewable was chosen
            {
                setContext(MODE_BROWSE);
            } else {
                setContext(MODE_BASIC);
            } //prevents viewing the same bodypart twice

            if ( ! getBPViewingModeWithActions().prepareForViewingBP(actBodyPart, ID, this) ) {
                return;
            }

            currAttachmentID = ID;
            repaint();
        }
        mujMail.getDisplay().setCurrent(this);
    }

    private byte getFirstViewableBody(MessageHeader mh) {
        byte bpCount = mh.getBodyPartCount();
        byte i;
        BodyPart bp;
        for (i = 0; i < bpCount; i++) {
            bp = mh.getBodyPart((byte) i);
            if (bp.getAutoViewingMode() != BPViewingModes.NO_VIEW) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Loads and displays the first text or HTML part of a body.
     * If the body has not been downloaded yet, than it is necessary to retrieve the whole message from the remote server.
     * @param mh - {@link MessageHeader} that body should be read.
     * @return first bodyPart String. If there is no textual part of the body, then it returns a <code> String </code> informing
     * about it.
     */
    public MessageHeader loadBody(MessageHeader mh) {
        this.msgHeader = mh;
        // if body is in the RecordStore
        if (mh.getBodyPartCount() > 0) {
            return mh;
        } // body must be downloaded
        else {
            //its not inBox or user folder (ie trash), but we would save the mail's body the inbox 
            //and it would cause a mess in Database
            if (!(mh.getBox() instanceof InBox)) {
                return null;
            }
            synchronized (mh) {
                ((InBox)mh.getBox()).getBody(mh);
                try {
                    mh.wait();
                } catch (Exception ex) {
                }
            }
            if (mh.getBodyPartCount() == 0) //message could not be downloaded
            {
                return null;
            }

            return mh;
        }

    }

    /**
     * Changes a graphical context, which is used to recognize, what is to be displayed. There are three possible
     * choices.
     * <ol>
     * 	<li> TEXT_MODE context - is used to display plain text.
     * 	<li> MODE_BROWSE context - is used to display image attachments.
     * 	<li> MODE_LIST context - is used when some form or list is supposed to be displayed .
     * </ol>
     *
     * <p> It sets up appropriate Commands to the listAttachments object.
     * @param contextID - graphical context.
     */
    public void setContext(byte contextID) {
        this.contextID = contextID;
        currDisplay = 0;

        switch (contextID) {
            // text/html context
            case MODE_BASIC:
                if (msgHeader.messageFormat == MessageHeader.FRT_MULTI && msgHeader.getBodyPartCount() > 1) {
                    addCommand(listAttachments);
                }
                addCommand(back);
                addCommand(listAttachments);
                addCommand(showHeader);
                addCommand(showAddresses);
                //#ifdef MUJMAIL_FS
                addCommand(exportToFS);
                //#endif
                break;

            // attachment contexts
            case MODE_BROWSE:
                removeCommand(listAttachments);
                removeCommand(showHeader);
                removeCommand(showAddresses);
                //#ifdef MUJMAIL_FS
                removeCommand(exportToFS);
                //#endif
                break;
        }
    }

    /**
     * Returns a contextID value.
     */
    public byte getContext() {
        return contextID;
    }

    /**
     * Shows an *.PNG image attachment on the display. We use <code>scaleImage</code>
     * function to resize image. Since MIDP 1.0 dosn't support loading of pixels from the 
     * image to the buffer, the resized image will lose some information compared to the
     * previous one. 
     */
    private void drawImage(Graphics g) {
        // clear the display
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());

        // encoding is base64
        if (msgHeader.getBpEncoding(currAttachmentID) == BodyPart.ENC_BASE64) {
            try {
                byte[] data = msgHeader.getBodyPartContentRaw(currAttachmentID);
                if (data == null) {
                    throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
                }
                Image img = Image.createImage(data, 0, data.length);
                int imgWidth = img.getWidth();
                int imgHeight = img.getHeight();
                short width = (short) getWidth(), height = (short) getHeight();
                short dstHeight, dstWidth;
                int widthRatio = (width << 16) / imgWidth;
                int heightRatio = (height << 16) / imgHeight;

                if (imgWidth <= width && imgHeight <= height) {
                    g.drawImage(img, ((width - imgWidth) >> 1), ((height - imgHeight) >> 1), Graphics.TOP | Graphics.LEFT);
                } // if image is bigger than display then scale it.
                else {
                    if (widthRatio > heightRatio) {
                        dstHeight = height;
                        dstWidth = (short) ((imgWidth * heightRatio) >> 16);
                        g.drawImage(Functions.scaleImage(img, dstWidth, dstHeight), ((width - dstWidth) >> 1), 0, Graphics.TOP | Graphics.LEFT);
                    } else {
                        dstHeight = (short) ((imgHeight * widthRatio) >> 16);
                        dstWidth = width;
                        g.drawImage(Functions.scaleImage(img, dstWidth, dstHeight), 0, ((height - dstHeight) >> 1), Graphics.TOP | Graphics.LEFT);
                    }
                }
            } catch (MyException ex) {
                back();
                mujMail.alert.setAlert(this, callBox, ex.getDetails(), MyAlert.DEFAULT, AlertType.ERROR);
            } catch (Exception ex) {
                back();
                mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_VIEW_ATT) + Lang.get(Lang.FAILED) + " " + ex.toString(), MyAlert.DEFAULT, AlertType.ERROR);
            } catch (Error e) {
                back();
                mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_VIEW_ATT) + Lang.get(Lang.FAILED) + " " + e.toString(), MyAlert.DEFAULT, AlertType.ERROR);
            }
        } else {
            back();
            mujMail.alert.setAlert(this, callBox, Lang.get(Lang.ALRT_MF_UNSUPPORTED_FORMAT), MyAlert.DEFAULT, AlertType.ERROR);
        }
    }

    /**
     * <p>
     * This method draws all text body parts on the <code> Canvas </code>. It also count replies represented
     * by ">" character at the beginning of each line and changes a color of the line.
     * <p>
     * If the message contains any attachment, the Canvas has a command <code>listAttachments<code> that serves to
     * list out all attachments available.
     *
     * @param dsplText - a Vector that contains displays of text.
     * @param g - a graphical object.
     */
    private void drawText(String body, Vector dsplText, Graphics g) {
        // init
        if (dsplText == null || dsplText.size() == 0) {
            return;
        }
        int fontHeight = g.getFont().getHeight();
        int width = getWidth(), height = getHeight();

        // clear the display
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());

        // draw a scrollbar
        g.setColor(0x00CCCCCC);
        g.fillRect(width - 2, 0, 2, height);
        g.setColor(0x00FF0000);
        short scrollStep = (short) (height / dsplText.size());
        short minStep = 10;
        if (scrollStep < minStep) {
            g.fillRect(width - 2, (currDisplay * height) / dsplText.size(), 2, minStep);
        } else {
            g.fillRect(width - 2, (currDisplay * height) / dsplText.size(), 2, scrollStep);
        }

        // draw a display of text
        Vector lines = (Vector) dsplText.elementAt(currDisplay);
        int lineCount = lines.size();
        TextDisplayLine line;
        int x = 0, y = 0;
        //String substr;
        int k;
        for (int i = 0; i < lineCount; i++) {
            line = (TextDisplayLine) lines.elementAt(i);
            g.setColor(0x00000000);
            for (byte j = 1; j <= line.countReplies; j++) {
                x += 2;
                g.setColor(COLOR[j % MAX_COLORS]);
                g.drawLine(x - 1, y, x - 1, y + fontHeight);
            }
            k = line.endLn;
            while (line.beginLn < k) {
                //we have to remove the '\r' as its displayed on nokia 7370 and maybe others as "..."
                if (body.charAt(k - 1) == '\n' || body.charAt(k - 1) == '\r') {
                    --k;
                } else {
                    break;
                }
            }
            if (line.beginLn < k) {
                g.drawSubstring(body, line.beginLn, k - line.beginLn, x + 1, y, Graphics.TOP | Graphics.LEFT);
            }
            x = 0;
            y += fontHeight;
        }
    }

    private Vector addLineToPage(TextDisplayLine line, Vector page, Vector pages, int maxLinesPerPage) {
        if (page.size() < maxLinesPerPage) { //this line still fit in the page
            page.addElement(line);
            return page;
        }
        Vector newPage = new Vector(); //we have to create a new page
        newPage.addElement(line);
        pages.addElement(newPage);
        return newPage;
    }

    private Vector parseTextToDisplay(String body, Graphics g) {
        int bodyLen = body.length();
        int x = 0;
        int dspWidth = getWidth() - 4; // -(left + scrollbarWidth) 
        Vector currentPage = new Vector();
        Vector pages = new Vector();
        TextDisplayLine line = new TextDisplayLine();
        int cursor = 0;
        char c;
        Font font = g.getFont();
        int maxLinesPerPage = getHeight() / font.getHeight();
        byte repliesCounter = 0;
        boolean quoteMayFollow = true;

        int bow; //position of the beginning of a word
        int wordWidth;
        pages.addElement(currentPage);
        while (cursor < bodyLen) {
            c = body.charAt(cursor);
            switch (c) {
                case ' ':
                case '\t':
                    if (x + repliesCounter * 2 + font.charWidth(c) > dspWidth) { //this space doesnt fit in
                        //we add the line to page
                        currentPage = addLineToPage(line, currentPage, pages, maxLinesPerPage);
                        //create a new line that begins where the current line end 
                        //with this extra space char in the beginning						
                        line = new TextDisplayLine(line.endLn, line.endLn, repliesCounter);
                        //reset to the left
                        x = 0;
                    }
                    ++line.endLn;
                    x += font.charWidth(c); //move to right by this space char											
                    break;

                case '\r':
                    ++line.endLn;
                    break;

                case '\n':
                    repliesCounter = 0;
                    ++line.endLn;
                    currentPage = addLineToPage(line, currentPage, pages, maxLinesPerPage);
                    line = new TextDisplayLine(line.endLn, line.endLn, repliesCounter);
                    x = 0;
                    break;

                case '>':
                    if (quoteMayFollow) {
                        //needs to raise line's counter because it can not be affected bt repliesCounter							
                        ++line.countReplies;
                        ++repliesCounter;
                        ++line.beginLn; //don't draw this char
                        ++line.endLn;
                        break;
                    }
                default: //non-whitespace					
                    bow = cursor; //beginning of the word is this position
                    wordWidth = font.charWidth(c);
                    //read the whole word
                    while ((cursor + 1) < bodyLen && (c = body.charAt(cursor + 1)) != ' ' && c != '\r' && c != '\t' && c != '\n') {
                        wordWidth += font.charWidth(c);
                        ++cursor;
                    }

                    if (x + repliesCounter * 2 + wordWidth <= dspWidth) {//the word does fit in display width
                        //don't forget +1 because of char at cursor position is the last char of the word
                        line.endLn = cursor + 1; //and String.substring's endIndex is the last char?s position+1
                        x += wordWidth;

                    } else {//doesn't fit in																		
                        if (wordWidth + repliesCounter * 2 <= dspWidth) { //does fit in next new line																															
                            currentPage = addLineToPage(line, currentPage, pages, maxLinesPerPage);
                            //create a new line with the word including the last word's char - cursor+1
                            line = new TextDisplayLine(line.endLn, cursor + 1, repliesCounter);
                        } else { //too long word that doesn't even fit in new line
                            int i = 0;
                            wordWidth = 0;
                            while (bow + i <= cursor) { //let's try to break it to smaller parts that fit in
                                c = body.charAt(bow + i);
                                if (x + wordWidth + repliesCounter * 2 + font.charWidth(c) <= dspWidth) {
                                    ++line.endLn;
                                    wordWidth += font.charWidth(c);
                                    ++i;
                                } else { //this part of the word doesn't fit in display width
                                    //break the word									
                                    currentPage = addLineToPage(line, currentPage, pages, maxLinesPerPage);
                                    line = new TextDisplayLine(line.endLn, line.endLn, repliesCounter);
                                    //and reread the char at position (bow+i) -- don't increase i
                                    wordWidth = 0;
                                    x = 0;
                                }
                            }
                        }
                        x = wordWidth;
                    }

                    break;
            }//end of switch
            ++cursor; //for each iteration the cursor moves forward

            if (c == '\n' || (c == '>' && quoteMayFollow)) {
                quoteMayFollow = true;
            } else {
                quoteMayFollow = false;
            }
        }

        if (line.beginLn != line.endLn) //something remained
        {
            addLineToPage(line, currentPage, pages, maxLinesPerPage);
        }

        return pages;
    }

    protected void paint(Graphics g) {
        updateEditButton();

        setFont(g);
        //when 	no first viewable body is not available or is deleted			
        if (getFirstViewableBody(msgHeader) == -1 || 
                (getFirstViewableBody(msgHeader) != firstViewable && contextID != MODE_BROWSE)) {
            back();
            return;
        }
        setContextCommand();
        // display bodyparts
        switch (contextID) {
        	case MODE_LIST:
        		listAttachments();
        		break;
        		
            case MODE_BASIC:
                getBPViewingModeWithActions().prepareBeforeDisplayingBP(this);
                currAttachmentID = firstViewable; //useful when the attachments list returns focus back to the mailform
            
            case MODE_BROWSE:   // and MODE_BASIC
                if (!getBPViewingModeWithActions().displayBodyPart(g, this))
                    return;
                break;
        }
        
        if (Settings.smallFontMailForm) //set font to back to the default
        {
            g.setFont(Font.getDefaultFont());
        }
    }

    private void setContextCommand() {
        if (msgHeader.getBox() == mujMail.getInBox()) {
            addCommand(reply);
            addCommand(quotedReply);
            addCommand(replyAll);
        } else {
            removeCommand(reply);
            removeCommand(quotedReply);
            removeCommand(replyAll);
        }
    }

    //defaults values
    public void back() {
        currDisplay = 0;
        currAttachmentID = 0;
        textBodyDisplays = null;
        textBodyString = null;
        attchList = null;
        mailAdrList = null;
        headerForm = null;
        if (previewMode) {
            previewMode = false;
            mujMail.outBox.deleteNowFromBoxAndDB(msgHeader, 
                    Trash.TrashModes.NOT_MOVE_TO_TRASH);
            mujMail.getDisplay().setCurrent(mujMail.sendMail);
            return;
        }
        
        mujMail.getDisplay().setCurrent(callBox);
    }

    //#ifdef MUJMAIL_TOUCH_SCR
    protected void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        
        System.out.println("Pointer pressed: " + x + "; " + y);
        pointerEventTransformer.pointerPressed(x, y);
    }
    //#endif

    protected void keyPressed(int keyCode) {
        switch (getGameAction(keyCode)) {
            case UP:
                pointerEventListener.up();
                break;

            case DOWN:
                pointerEventListener.down();
                break;

            case LEFT:
                pointerEventListener.left();
                break;
            case RIGHT:
                pointerEventListener.right();
                break;
            case FIRE:
                pointerEventListener.fire();
        }
        if (keyCode == '7') {
            pointerEventListener.right();
        }
    }
}


