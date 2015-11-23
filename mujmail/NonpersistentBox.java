//#condition MUJMAIL_SEARCH
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

import mujmail.threading.Algorithm;

/**
 * The box which mails are not persistently stored in RMS database. The mails
 * are stored only in container of the box. 
 * Each mail must belong to some persistent box. So mails stored in this box
 * belong to several persistent boxes. 
 * If the mail is deleted, it is deleted from container of this box and also
 * from container and RMS database of the box to which the mail belongs. If
 * the mail is in more nonpersistent boxes and it is deleted from one
 * nonpersistent box, it is not deleted from others.
 * 
 * @author David Hauzar
 */
public abstract class NonpersistentBox extends TheBox {

    private static final boolean DEBUG = false;

    public NonpersistentBox(MujMail mMail, String name) {
        super(mMail, name);
    }

    public MessageHeader storeMail(MessageHeader header) {
        System.out.println("Storing mail: " + header);
        synchronized (storage) {
            storage.addMessage(header);
        }
        repaint();
        
        return header;
    }
    
    
    
    /**
     * Deletes all mails stored in Vector in this box from databases of boxes 
     * to which stored mails belong.
     */
    protected void deleteAllMailsFromDB() {
        for (int i = 0; i < storage.getSize(); i++) {
            MessageHeader message = (MessageHeader) storage.getMessageAt( i );
            message.deleteFromDBAndBox(this, Trash.TrashModes.CONDITIONALLY_MOVE_TO_TRASH);
        }
    }
    
    protected void doDeleteMarkedFromBoxAndDB() {
        for (int i = storage.getSize() - 1; i >= 0; i--) {
            MessageHeader message = (MessageHeader) storage.getMessageAt( i );
            if (message.deleted) {
                message.deleteFromDBAndBox(this, Trash.TrashModes.CONDITIONALLY_MOVE_TO_TRASH);
                storage.removeMessageAt( i );
            }
        }
    }
    
    public void setMessages(Vector/*<MessageHeader>*/ messages) {
          if (DEBUG) System.out.println( "name: " + getName() );
        this.storage = Algorithm.getAlgorithm().invoke( messages );
        resort();
        repaint();
    }

}
