package mujmail;

/*
MujMail - Simple mail client for J2ME
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

import mujmail.util.Functions;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import mujmail.account.MailAccount;

/**
 * The form for creating new mail.
 * 
 * TODO: better handling of messages which are edited in createAndSaveMessageHeader
 * 
 * TODO: Better comments
 * 
 * Implementation detail: Attachments must be last items in this form.
 * Otherwise removing attachments will not work and there will be removed
 * another items. See method FSAttachemtsAdder.removeAllAttachements()
 * 
 * @see FSAttachmentsAdder#removeAllAttachments()
 */
public class SendMail extends Form
        implements ItemStateListener
         {

    /** The name of this source file */
    private static final String SOURCE_FILE = "SendMail";

    private Displayable nextScreen;

    static final byte NORMAL = 0;
    static final byte EDIT = 1;
    static final byte REPLY = 2;
    static final byte FORWARD = 3;

//    //http://forum.java.sun.com/thread.jspa?threadID=5245522&messageID=10017092
//    Canvas iCanvas;

    /** The header of the message which will be created from this form */
//    private MessageHeader header;
    byte mode = NORMAL;
    TextField to, bcc, cc, subject, body, selectedField;
    StringItem from;
    Command preview;
    Command ok, cancel, send, sendLater, draft, bc, clear, addRcp, chooseAccount, editBody, updateBody, cancelBody;
    boolean bcc_cc_added;
      // TODO (Betlista): comment this, why is this persistent box called "callBox" ?
    PersistentBox callBox;
    AccountsForm accountsForm;
    ChoiceGroup accountsCG;
//    TextBoxCB editbodyTB;
    TextBox editbodyTB;
    MujMail mujMail;


    //#ifdef MUJMAIL_FS
    // Filesystem 
    /** Handles the support of filesystem in SendMail form */
    private FSAttachmentsAdder attachementsAdder = null;

    public FSAttachmentsAdder getAttachementsAdder() {
        return attachementsAdder;
    }
    //#endif

    /**
     * Shows form with active account list. 
     * Permit users to select mail used as sender (From entry)
     */
    class AccountsForm extends Form {

        public AccountsForm() {
            super(Lang.get(Lang.AS_ACCOUNTS));
            accountsCG = new ChoiceGroup(Lang.get(Lang.SM_SENDING_ACCOUNT), Choice.EXCLUSIVE);
            append(accountsCG);
            addCommand(ok);
            addCommand(cancel);
            setCommandListener(mujMail);
        }

        public void refresh() {
            if (!mujMail.getInBox().hasAccountToRetrieve()) {
                mujMail.alert.setAlert(this, null, Lang.get(Lang.ALRT_AS_NO_ACCOUNT_SET_ACTIVE), MyAlert.DEFAULT, AlertType.WARNING);
                return;
            }

            //markAsDeleted old things
            for (int i = accountsCG.size() - 1; i >= 0; --i) {
                accountsCG.delete(i);
            }

            int i = 0;
            //let's show the primary account on the first place
            if (!Settings.primaryEmail.equals(Settings.notSetPE)) {
                accountsCG.append(Settings.primaryEmail, null);
                accountsCG.setSelectedIndex(i, true);
                ++i;
            }


            //let's list all of the active rest			
            for (Enumeration e = mujMail.getMailAccounts().elements(); e.hasMoreElements();) {
                MailAccount account = (MailAccount) e.nextElement();
                if (account.isActive() && !account.getEmail().equals(Settings.primaryEmail)) {
                    accountsCG.append(account.getEmail(), null);
                    //if it was selected already, select it
                    if (account.getEmail().equals(from.getText())) {
                        accountsCG.setSelectedIndex(i, true);
                    }
                    ++i;
                }
            }

            //lets choose the primary or the first one if none was selected
            if (accountsCG.getSelectedIndex() == -1) {
                accountsCG.setSelectedIndex(0, true);
            }

            mujMail.getDisplay().setCurrent(this);
        }
    }

    public SendMail(MujMail mujMail) {
        super(Lang.get(Lang.AC_WRITE_MAIL));
        
//        header = new MessageHeader(mujMail.outBox);
        
        this.mujMail = mujMail;

        from = new StringItem(Lang.get(Lang.ML_FROM), "");
        if ( Properties.textFieldMailIncorrect ) {
            bcc = new TextField("Bcc:", "", 256, TextField.ANY);
            cc = new TextField("Cc:", "", 256, TextField.ANY);
            to = new TextField(Lang.get(Lang.ML_TO), "", 256, TextField.ANY); 
        } else {
            bcc = new TextField("Bcc:", "", 256, TextField.EMAILADDR);
            cc = new TextField("Cc:", "", 256, TextField.EMAILADDR);
            to = new TextField(Lang.get(Lang.ML_TO), "", 256, TextField.EMAILADDR); 
        }
        subject = new TextField(Lang.get(Lang.ML_SUBJECT), "", 256, TextField.ANY);
        body = new TextField(Lang.get(Lang.ML_BODY), "", 5000, TextField.ANY);

        //#ifdef MUJMAIL_FS
        if ( Properties.JSR75Available() == true) {
            attachementsAdder = new FSAttachmentsAdder(this);
        }
        //#endif
        
        preview = new Command(Lang.get(Lang.BTN_SM_PREVIEW), Command.ITEM, 9);
        ok = new Command(Lang.get(Lang.BTN_OK), Command.OK, 0);
        cancel = new Command(Lang.get(Lang.BTN_CANCEL), Command.BACK, 5);
        send = new Command(Lang.get(Lang.BTN_SM_SEND), Command.ITEM, 1);
        addRcp = new Command(Lang.get(Lang.BTN_SM_USE_AB), Command.ITEM, 2);
        sendLater = new Command(Lang.get(Lang.BTN_SM_SEND_LATTER), Command.ITEM, 3);
        draft = new Command(Lang.get(Lang.BTN_SAVE), Command.ITEM, 4);
        bc = new Command(Lang.get(Lang.BTN_SM_BC), Command.ITEM, 5);
        editBody = new Command( Lang.get(Lang.BTN_SM_EDIT_BODY_FULLSCREEN), Command.ITEM, 6);
        clear = new Command(Lang.get(Lang.BTN_SM_CLEAR), Command.ITEM, 7);
        chooseAccount = new Command(Lang.get(Lang.BTN_SM_CHOOSE_FROM), Command.ITEM, 8);

        accountsForm = new AccountsForm();

        editbodyTB = new TextBox(Lang.get(Lang.SM_EDIT_BODY), "", 5000, TextField.ANY);		
//        editbodyTB = new TextBoxCB("Edit body", "", 5000, TextField.ANY, this.mujMail);		
//        editbodyTB.cmStartMark = new Command("Mark", Command.SCREEN, 2);
//        editbodyTB.cmCopy = new Command("Copy", Command.SCREEN, 3);
//        editbodyTB.cmCut = new Command("Cut", Command.SCREEN, 4);
//        editbodyTB.cmPaste = new Command("Paste", Command.SCREEN, 5);

        updateBody = new Command( Lang.get(Lang.BTN_SM_UPDATE_BODY), Command.ITEM,9);
        cancelBody = new Command( Lang.get(Lang.BTN_SM_CANCEL_BODY), Command.BACK, 1);

        append(from);
        append(to);
        append(subject);
        append(body);
        addCommand(preview);
        addCommand(bc);
        addCommand(addRcp);
        addCommand(cancel);
        addCommand(send);
        addCommand(sendLater);
        addCommand(draft);
        addCommand(editBody);
        addCommand(clear);
        addCommand(chooseAccount);
        setCommandListener(mujMail);
        setItemStateListener(this);

        editbodyTB.addCommand(cancelBody);
//        editbodyTB.addCommand(editbodyTB.cmStartMark);
//        editbodyTB.addCommand(editbodyTB.cmCopy);
//        editbodyTB.addCommand(editbodyTB.cmCut);
//        editbodyTB.addCommand(editbodyTB.cmPaste);
        editbodyTB.addCommand(updateBody);
        editbodyTB.setCommandListener(mujMail);
  

    }
    
    /* *
     * Creates the message from this form.
     * @return
     */
//    public MessageHeader createMessage() {
//    }

    public void itemStateChanged(Item item) {
        selectedField = (TextField) item;
    }
    
    public TextField getSelectedItem() {
        return selectedField;
    }

    public void replyAll(MessageHeader header) {
        nextScreen = MujMail.mujmail.mailForm;

        reply(header);
        Vector rcp = Functions.parseRcp(header.getFrom() + ", " + header.getRecipients() );
        
        //remove my own email address - doesn't make sense to reply myself
        rcp.removeElement(header.getAccountID());
        
        StringBuffer bf = new StringBuffer();
        for (int i = rcp.size() - 1; i >= 0; --i) {
            bf.append(((String) rcp.elementAt(i)) + ", ");
        }
        to.setString(bf.toString());
    }

    public void reply(MessageHeader header) {
        nextScreen = MujMail.mujmail.mailForm;

        mode = REPLY;
        from.setText(header.getAccountID());
        to.setString(header.getFrom().trim());
        if (header.getSubject().toLowerCase().startsWith("re:")) {
            subject.setString(header.getSubject());
        } else {
            subject.setString("Re: " + header.getSubject());
        }
        this.callBox = header.getBox();
        mujMail.getDisplay().setCurrent(this);
    }

    public void quotedReply(MessageHeader header) {
        nextScreen = MujMail.mujmail.mailForm;

        if (header.getBodyPartCount() == 0 || !(header.getBpType((byte) 0) == BodyPart.TYPE_HTML || header.getBpType((byte) 0) == BodyPart.TYPE_TEXT)) {
            mujMail.alert.setAlert(null, null, Lang.get(Lang.ALRT_SM_CAN_NOT_ATTACH_BODY), MyAlert.DEFAULT, AlertType.WARNING);
        } else {
            if ((header.getBpType((byte) 0) == BodyPart.TYPE_TEXT || header.getBpType((byte) 0) == BodyPart.TYPE_HTML) && header.getBpState((byte) 0) <= BodyPart.BS_PARTIAL) {
                StringBuffer newBody = new StringBuffer(header.getBodyPartContent((byte) 0));
                int i = 0;
                newBody.insert(i, '>');
                while (i < newBody.length()) {
                    if (newBody.charAt(i) == '\n') {
                        newBody.insert(i + 1, '>');
                        i += 2;
                    } else {
                        i++;
                    }
                }
                if (newBody.length() > body.getMaxSize()) {
                    body.setMaxSize(newBody.length() + 1000);
                }
                body.setString("\r\n" + newBody.toString() + "\r\n");
            }
            reply(header);
        }
    }

    public void edit(MessageHeader header, Displayable nextScreen) {
        edit(header, nextScreen, NORMAL);
    }

    private void edit(MessageHeader header, Displayable nextScreen, byte mode) {
//        this.header = header;
        this.nextScreen = nextScreen;
        this.mode = mode;
        
        from.setText(header.getFrom());

        String rcps = header.getRecipients();
        int x, y, z;
        //detect To: Bcc: Cc: 
        x = rcps.indexOf("Bcc:");
        y = rcps.indexOf("Cc:");
        z = rcps.indexOf("To:");
        if (z != -1) {
            to.setString(rcps.substring(z + 3, rcps.indexOf(" *", z + 4)).trim());
        }
        if (x != -1 || y != -1) {
            set(1, bcc);
            set(2, cc);
            if (x != -1) {
                bcc.setString(rcps.substring(x + 4, rcps.indexOf(" *", x + 5)).trim());
            }
            if (y != -1) {
                cc.setString(rcps.substring(y + 3, rcps.indexOf(" *", y + 4)).trim());
            }
            append(subject);
            append(body);
            removeCommand(bc);
        }

        subject.setString(header.getSubject());
        BodyPart bp = header.getBodyPart((byte) 0);
        try {
            if (mode == NORMAL) {
                body.setString(bp.getStorage().getContent());
            } else {
                body.setString("\n---------- Forwarded message ----------\n" +
                    "From: " + header.getFrom() + "\n" +
                    "Date: " + header.getTimeStr() + "\n" +
                    "Subject: " + header.getSubject() + "\n" + "" +
                    "Recipients: " + header.getRecipients() + "\n" + "\n" +
                    header.getBodyPartContent((byte) 0));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        //#ifdef MUJMAIL_FS
        attachementsAdder.importAttachmentsFromHeader(header);
        //#endif
        
        this.callBox = header.getBox();
        mujMail.getDisplay().setCurrent(this);
    }

    /**
     * Forward the message.
     * @param callBox
     */
    public void initForward(TheBox callBox, Displayable nextScreen) {
        edit(callBox.getSelectedHeader(), nextScreen, FORWARD);
    }

    public void addBc() {
        if (bcc_cc_added == false) {
            set(2, bcc); //set subject textfield to bcc
            set(3, cc); //set body to cc
            append(subject);
            append(body);
            if (mode == FORWARD) {
                StringItem fwdMsgNotice = (StringItem) get(4);
                delete(4); //remove the old one
                append(fwdMsgNotice); //append again the forwarded message string item			
            }
            removeCommand(bc);
            bcc_cc_added = true;
        }
    }

    public void chooseAccounts() {
        accountsForm.refresh();
    }
    
    
    /**
     * Creates the message from data filled in this form and saves it to the
     * database. If the storingBox is the same as {@link SendMail#callBox}
     * and mode is {@link SendMail#EDIT}, saves the message to already existing
     * record in the RMS database.
     * 
     * @param storingBox the box to which the header will be stored (will belong)
     * @return the message header of message created form data filled in this 
     *  form.
     */
    public MessageHeader createAndSaveMessage(PersistentBox storingBox) {
        StringBuffer rcps = new StringBuffer();
        String recipients;
        //now try to append all the recipients ()
        if (bcc.getString().length() != 0) {
            rcps.append("Bcc: ").append(bcc.getString()).append(" *");
        }
        if (cc.getString().length() != 0) {
            rcps.append("Cc: ").append(cc.getString()).append(" *");
        }
        if (to.getString().length() != 0) {
            rcps.append("To: ").append(to.getString());
        }
        recipients = rcps.toString().trim();
        if (!recipients.endsWith("*")) {
            recipients = recipients + " *";
        }
        if (recipients.length() < 6) { //its to short, must be invalid address
            storingBox.report("100: " + Lang.get(Lang.ALRT_SM_NO_RCP), SOURCE_FILE);
            return null;
        }
        
        String ID = Functions.genID();
        MessageHeader tmpM = new MessageHeader(storingBox, from.getText(), recipients, subject.getString(), ID, System.currentTimeMillis());
        BodyPart bp = new BodyPart(tmpM, "default_mail_body");
        tmpM.addBodyPart(bp);

        //#ifdef MUJMAIL_FS
        //add attachment file to header if user attached one
        if (getAttachementsAdder().getAttachmentsCount() > 0) {
            // remove all existing file system attachments (in case of editing message)
            tmpM.removeFSAttachments();
            getAttachementsAdder().exportAttachmentsToHeader(tmpM);
            
            tmpM.messageFormat = MessageHeader.FRT_MULTI;
        } else {
            tmpM.messageFormat = MessageHeader.FRT_PLAIN;
        }
        //#else
//#         tmpM.messageFormat = MessageHeader.FRT_PLAIN;
        //#endif

        
        
        // bodyparts are already saved while making deep copy
        try {
            bp.getStorage().addToContent(body.getString() + "\r\n");

            if (mode == SendMail.EDIT && storingBox == callBox) {
                // the message will be only updated while calling storingBox.mailDB.saveHeader(tmpM);
                // it will be stored to existing database record
                tmpM.setRecordID( storingBox.getMessageHeaderAt( storingBox.getSelectedIndex() ).getRecordID() );
                tmpM.DBStatus = MessageHeader.STORED; //so we can update the old mail in the mailDB.saveHeader()call later
            }


            storingBox.mailDB.saveHeader(tmpM);
        } catch (Exception ex) { //rollback & cleaning
            ex.printStackTrace();
            storingBox.report(ex.getMessage() + " " + subject.getString(), SOURCE_FILE);
            bp.getStorage().deleteContent();
            return null;
        }

        tmpM.updateSize();

        return tmpM;
    }

    public void selectFromAccount() {
        from.setText(accountsCG.getString(accountsCG.getSelectedIndex()));
        mujMail.getDisplay().setCurrent(this);
    }

    /**
     * Displays the form for writingn new mail.
     * @param nextScreen
     */
    public void writeMail(Displayable nextScreen) {
        this.nextScreen = nextScreen;
        initFrom();
        MujMail.mujmail.getDisplay().setCurrent(this);
    }

    private void initFrom() {
        from.setText(Settings.primaryEmail);
        //primaryEmail is not set, let's select the first one at least
        if (Settings.primaryEmail.equals(Settings.notSetPE)) {
            for (Enumeration e = mujMail.getMailAccounts().elements(); e.hasMoreElements();) {
                MailAccount account = (MailAccount) e.nextElement();
                if (account.isActive()) {
                    from.setText(account.getEmail());
                    break;
                }
            }
        }
    }

    /**
     * Clears text fields and attachments.
     */
    public void clear() {
        to.setString("");
        subject.setString("");
        body.setString("");
        bcc.setString("");
        cc.setString("");
        if (bcc_cc_added) {
            delete(2);
            delete(2);
            addCommand(bc);
            bcc_cc_added = false;
        }
        
        //#ifdef MUJMAIL_FS
        attachementsAdder.removeAllAttachments();
        //#endif
    }

    public void CheckRecordSize(List list) throws RecordStoreException {
        RecordStore rs = RecordStore.openRecordStore("test_this", true);
        try {
            list.append(String.valueOf(rs.getSizeAvailable()), null);
        } finally {
            rs.closeRecordStore();
        }
    }

    //edits body in a whole screen textbox
    public void editBody() {
        if (body.size() > editbodyTB.getMaxSize()) {
            editbodyTB.setMaxSize(body.size()+1000);
        }
        editbodyTB.setString(body.getString());
        //editbodyTB.insert(" ", 0); //reset caret? no
        mujMail.getDisplay().setCurrent(editbodyTB);
    }

    /**
     * Displays the screen that should follow after this form.
     */
    public void showNextScreen() {
        System.out.println("Next screen is " + nextScreen);
        MujMail.mujmail.getDisplay().setCurrent(nextScreen);
    }

    //updates body from a whole screen textbox
    public void updateBody() {
        if (editbodyTB.size() > body.getMaxSize()) {
            body.setMaxSize(editbodyTB.size()+1000);            
        }
        body.setString(editbodyTB.getString());
        mujMail.getDisplay().setCurrent(this);
        //unfortunately, no set focus in MIDP 1
        //body.insert("", body.getCaretPosition());
        // this don't work for focus either: IllegalStateException at javax.microedition.lcdui.Form.append(Form.java:218)
        //this.markAsDeleted(3);
        //this.append(body);
    }

    //cancels body from a whole screen textbox
    public void cancelBody() {
        mujMail.getDisplay().setCurrent(this);
        selectedField = this.body;
    }
 }
