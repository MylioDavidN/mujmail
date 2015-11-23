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


import mujmail.util.Functions;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Serves for restoring of deleted messages.
 */
public class Trash extends PersistentBox {

    Command restore, restoreNow;
    Hashtable toRestore;
    Image imToRestore = Functions.getIcon("m_toRestore.png");

    /**
     * Creates thrash.
     * 
     * @param DBFile the identifier of RMS database where the mails of this box
     *  will be stored.
     * @param mMail the main object in the application
     * @param name the name of the box
     */
    public Trash(String DBFile, MujMail mujMail, String name ) {
        super(DBFile, mujMail, name);
        toRestore = new Hashtable();
        restore = new Command(Lang.get(Lang.BTN_TR_RESTORE), Command.OK, 0);
        restoreNow = new Command(Lang.get(Lang.BTN_TR_RESTORE_NOW), Command.ITEM, 1);
        addCommand(restore);
        addCommand(restoreNow);
    }

    /**
     * Stores message to trash according to trashMode.
     * @param header the header of the message to be moved to trash
     * @param trashMode
     */
    public void storeToTrash(MessageHeader header, TrashModes trashMode) {
        trashMode.storeToTrash(header);
    }
    
    private void storeToTrash(MessageHeader header) {
        if (header != null) {
            header.DBStatus = MessageHeader.NOT_STORED; //not to confuse the mailDB.saveHeader() to update the headerSystem.out.println("Trash dbfile: " + DBFile);
            storeMail(header);
        }
    }

    public void restoreNow() {
        MessageHeader header;
        boolean[] resortNeeded = new boolean[4];
        for (Enumeration e = toRestore.elements(); e.hasMoreElements();) {
            header = (MessageHeader) e.nextElement();
            switch ( header.getOrgLocation() ) {
                case 'I':
                    if (getMujMail().getInBox().storeMail(header) != null) {
                        toRestore.remove(header.getMessageID() + header.getRecordID() );
                        resortNeeded[0] = true;
                    } else {
                        continue;
                    }
                    break;
                case 'O':
                    if (getMujMail().outBox.storeMail(header) != null) {
                        toRestore.remove(header.getMessageID() + header.getRecordID() );
                        resortNeeded[1] = true;
                    } else {
                        continue;
                    }
                    break;
                case 'D':
                    if (getMujMail().draft.storeMail(header) != null) {
                        toRestore.remove(header.getMessageID() + header.getRecordID() );
                        resortNeeded[2] = true;
                    } else {
                        continue;
                    }
                    break;
                case 'S':
                    if (getMujMail().getSentBox().storeMail(header) != null) {
                        toRestore.remove(header.getMessageID() + header.getRecordID() );
                        resortNeeded[3] = true;
                    } else {
                        continue;
                    }
                    break;
            }

            if (!header.deleted) {
                ++deleted;
                header.deleted = true;
            }
        }

        deleteMarkedFromBoxAndDB(); //remove the restored mails from the trash
        if (resortNeeded[0]) {
            getMujMail().getInBox().resort();
        }
        if (resortNeeded[1]) {
            getMujMail().outBox.resort();
        }
        if (resortNeeded[2]) {
            getMujMail().draft.resort();
        }
        if (resortNeeded[3]) {
            getMujMail().getSentBox().resort();
        }
    }

    public void restore(MessageHeader header) {
        if (header != null) {
            if (toRestore.containsKey(header.getMessageID() + header.getRecordID() )) {
                toRestore.remove(header.getMessageID() + header.getRecordID() );
            } else //we must identify it by messageID+recordID, 
            //because the same mail can be downloaded and moved to the trash many times
            {
                System.out.println("Added to restore");
                toRestore.put(header.getMessageID() + header.getRecordID(), header);
            }
            shiftSelectedIndex(true);
            repaint();
        }
    }

    protected void keyPressed(int keyCode) {
        if (isBusy()) {
            return;
        }
        if (keyCode == '3' && getSelectedHeader() != null) {
            restore(getSelectedHeader());
        }
        super.keyPressed(keyCode);
    }

    protected void hideButtons() {
        if (!btnsHidden) {
            removeCommand(restore);
            removeCommand(restoreNow);
            super.hideButtons();
        }
    }

    protected void showButtons() {
        if (btnsHidden) {
            addCommand(restore);
            addCommand(restoreNow);
            super.showButtons();
        }
    }

    protected void drawIcons(MessageHeader header, Graphics g, int x, int y) {
        if (toRestore.containsKey(header.getMessageID() + header.getRecordID() )) {
            g.drawImage(imToRestore, x, y + 3, Graphics.TOP | Graphics.LEFT);
        } else {
            super.drawIcons(header, g, x, y);
        }
    }
    
    /**
     * Strategy class that describes possible modes of storing message to the Trash.
     */
    public abstract static class TrashModes {
        /** Do not move the message to the trash. */
        public static final TrashModes NOT_MOVE_TO_TRASH = new NotMoveToTrash();
        /** Move the message to the trash. */
        public static final TrashModes MOVE_TO_TRASH = new MoveToTrash();
        /** Move the message to the trash if it's appropriate, that means: settings allow it, not deleting a mail from the Trash. */
        public static final TrashModes CONDITIONALLY_MOVE_TO_TRASH = new ConditionallyMoveToTrash();
        
        protected abstract void storeToTrash(MessageHeader message);
        
        /**
         * Returns true if it is appropriate to store given message to trash.
         * that means: settings allow it, not deleting a mail from the Trash.
         * This test is used by CONDITIONALLY_MOVE_TO_TRASH.
         * 
         * @param message the message that is tested whether it is appropriate to
         *  move it to trash.
         * @return true if it is appropriate to store given message to trash.
         */
        private static boolean shouldStoreToTrash(MessageHeader message) {
            //move it the trash if it's appropriate, that means: settings allow it, not deleting a mail from the Trash
            if (!Settings.safeMode && 
                    Settings.moveToTrash && 
                    (message.getMailDB() != MujMail.mujmail.getTrash().getMailDB()) && // nejsem v kosi
                    !(((message.getMailDB() == MujMail.mujmail.outBox.getMailDB()) ||
                       (message.getMailDB() == MujMail.mujmail.draft.getMailDB())) && 
                      message.sendStatus == MessageHeader.SENT)) {
                return true;
            } else {
                return false;
            }
        }
        
        private final String name;

        private TrashModes(String name) {
            this.name = name;
        }
        
        protected void doStoreToTrash(MessageHeader message) {
            MujMail.mujmail.getTrash().storeToTrash(message);
        }
        
        

        public String toString() {
            return name;
        }
        
        private static class NotMoveToTrash extends TrashModes {

            public NotMoveToTrash() {
                super("NOT_MOVE_TO_TRASH");
            }

            protected void storeToTrash(MessageHeader message) {}
            
            
        }
        
        private static class MoveToTrash extends TrashModes {

            public MoveToTrash() {
                super("MOVE_TO_TRASH");
            }

            protected void storeToTrash(MessageHeader message) { doStoreToTrash(message);}
            
            
        }
        
        private static class ConditionallyMoveToTrash extends TrashModes {

            public ConditionallyMoveToTrash() {
                super("CONDITIONALLY_MOVE_TO_TRASH");
            }

            protected void storeToTrash(MessageHeader message) {
                if (shouldStoreToTrash(message)) {
                    doStoreToTrash(message);
                }
            }
        }
        
        
    }

    public void commandAction(Command c, Displayable d) {
        super.commandAction(c, d);

        if (c == restore) {
            restore(getSelectedHeader());
        } else if (c == restoreNow) {
            restoreNow();
        }
    }
}
