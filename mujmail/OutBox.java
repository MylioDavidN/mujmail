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

package mujmail;

import javax.microedition.lcdui.Command;

import javax.microedition.lcdui.Displayable;
import mujmail.account.MailAccount;
import mujmail.protocols.InProtocol;
import mujmail.protocols.SMTP;
import mujmail.threading.ThreadedEmails;

/**
 * Stores mails which were already sent.
 * Offers form for writing mail.
 * 
 * Uses shared instance of class SMTP!!
 */
public class OutBox extends PersistentBox {
    /** The name of this source file */
    private static final String SOURCE_FILE = "OutBox";
    Command sendAll, sendThis;

    /**
     * Creates new out box.
     * 
     * @param DBFile the identifier of RMS database where the mails of this box
     *  will be stored.
     * @param mMail the main object in the application
     * @param name the name of the box
     */
    public OutBox(String DBFile, MujMail mMail, String name) {

        super(DBFile, mMail, name);
        
        
        sendThis = new Command(Lang.get(Lang.BTN_OB_SEND), Command.ITEM, 1);
        sendAll = new Command(Lang.get(Lang.BTN_OB_SENDALL), Command.ITEM, 2);

        addCommand(sendThis);
        addCommand(sendAll);
    }

    protected boolean isBusy() {
        return (SMTP.getSMTPSingleton(getMujMail()).isBusy() || super.isBusy());
    }

    public void sendSingle(MessageHeader header) {
        if (header != null) {
            SMTP.getSMTPSingleton(getMujMail()).sendMail(header, this);
        } else {
            getMujMail().mainMenu();
        }
    }

    public void sendAll() {
        if (storage.getSize() > 0) {
            SMTP.getSMTPSingleton(getMujMail()).sendMails(this);
        } else {
            getMujMail().mainMenu();
        }
    }

    public void stop() {
        SMTP.getSMTPSingleton(getMujMail()).stop();
    }

    /**
     * Fills in the form for creating new mail, creates message header and
     * body parts, saves or updates it in the database and adds the mail
     * to the Outbox storage.
     * If this instance is the same as {@link SendMail#callBox} and mode
     * is {@link SendMail#EDIT}, saves the message to already existing record
     * in the RMS database.
     * If the mail is copied from Drafts to Outbox, deletes it from Drafts.
     * 
     * @param sendMail the form for creating new mail
     * @return the header of created mail
     */
    public MessageHeader addOutMail(SendMail sendMail) {
        MessageHeader message = sendMail.createAndSaveMessage(this);
        
        message.sendStatus = MessageHeader.TO_SEND;
        
        if (sendMail.mode == SendMail.REPLY) {//we're replying, lets mark the mail being replied as replied
            MessageHeader ibHeader = (MessageHeader) getMujMail().getInBox().getMessageHeaderAt( getMujMail().getInBox().getSelectedIndex() );
            ibHeader.sendStatus = MessageHeader.REPLIED;
            
            MailAccount msgAcct = (MailAccount)getMujMail().getMailAccounts().get(ibHeader.getAccountID());
            System.out.println("SETTING REPLIED FLAG: msgAcct.type="+msgAcct.getType());
            //Set '\Answered' flag on server if it's an IMAP account
            if (msgAcct.isIMAP()) {
            	msgAcct.getProtocol().setFlags(ibHeader, "(\\Answered)", InProtocol.SET_FLAGS, this);
            }

            try {
                getMujMail().getInBox().mailDB.saveHeader(ibHeader); //update sendStatus
            } catch (MyException ex) {
                getMujMail().getInBox().report(ex.getDetails(), SOURCE_FILE);
            }
        }

        if (sendMail.mode == SendMail.EDIT && sendMail.callBox == this) {
              // update the old one with the new one
//            storage.setElementAt(message, getSelectedIndex());
            MessageHeader oldMessage = storage.getMessageAt( getSelectedIndex() );
            oldMessage.fillWith( message );
        } else {
//            storage.addElement( message );
            if (storage instanceof ThreadedEmails) {
                ((ThreadedEmails)storage).addRoot( message );
            }
        }

        if (sendMail.mode == SendMail.FORWARD) {
            sendMail.delete(sendMail.size() - 1);
        }
        sendMail.mode = SendMail.NORMAL;
        sendMail.clear();

        resort(); //we added a new mail, we have to resort to match the sorting criteria of the outbox

        // if the mail is copied from Drafts to OutBox, we want to delete it
        deleteFromDraftsIfCopyedToAnother(sendMail.callBox);

        return message;
    }
    
    /**
     * Deletes the mail that is actually marked in the box copyedFrom and that
     * is copyed to this box if copyedFrom is Drafts and the message is copyed
     * to another box.
     * 
     * @param copyedFrom the box from which the message is copyed from
     */
    private void deleteFromDraftsIfCopyedToAnother(PersistentBox copyedFrom) {
        // we assume that if copyedFrom is instanceof OutBox, it is Drafts
        if (copyedFrom instanceof OutBox && copyedFrom != this) {
            ((MessageHeader) copyedFrom.getMessageHeaderAt(
                copyedFrom.getSelectedIndex())
            ).deleteFromDBAndBox(this, Trash.TrashModes.NOT_MOVE_TO_TRASH);
        }
    }

    protected void hideButtons() {
        if (!btnsHidden) {
            if (this == getMujMail().outBox || this == getMujMail().draft) {
                removeCommand(sendAll);
                removeCommand(sendThis);
            }
            super.hideButtons();
        }
    }

    protected void showButtons() {
        if (btnsHidden) {
            if (this == getMujMail().outBox || this == getMujMail().draft) {
                addCommand(sendAll);
                addCommand(sendThis);
            }
            super.showButtons();
        }
    }

    public void commandAction(Command c, Displayable d) {
        //OutBox box = (OutBox)d; // box <--> this
        super.commandAction(c, d);

        if (c == sendAll) {
            sendAll();
        } else if (c == sendThis) {
            sendSingle(getSelectedHeader());
        } else if (c == stop) {
            stop();
        }
    }
}
