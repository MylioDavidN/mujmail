/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

import java.util.Hashtable;
import java.util.Vector;

/** Maps MailDB instances into PersistenBox instances that uses them */
public class MailDBManager {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false; /// Debugging output for this file

    private Hashtable/*<String, structDBList>*/ dbList = new Hashtable/*<String, structDBList>*/();
    
    /**
     * Gets MailDB with specified name (database name).
     * If multiple boxes uses same database return 
     * one shared MailDB instance
     *
     * @param owner box that will uses this database
     * @param name database name
     */
    public MailDB getMailDB( PersistentBox owner, String name) {
        if (DEBUG) { System.out.println("DEBUG MailDBMAnager.getMailDB( " + owner.getDBFileName() + "," + name + ")"); }
        // Try if folder exists
        structDBList db = (structDBList)dbList.get(name);
        if (db == null) { // Not created now --> create DB file and add into internal structures
            db = new structDBList();
            db.db = new MailDB(name);
            db.owners = new Vector(1);
            db.owners.addElement(owner);
            dbList.put(name, db);
        } else // entry exists
        if ( db.owners.contains(owner) == false) {
            db.owners.addElement(owner);
        }
        return db.db;
    }
    
    public void loadedDB(MailDB invoker) {
        if (DEBUG) { System.out.println("DEBUG MailDBManager.loadedDB - " + invoker.getDBName());}

        structDBList db = (structDBList)dbList.get( invoker.getDBName());
        if (db == null) {
            if (DEBUG) { System.out.println("DEBUG MailDBMAnager.loadedDB - unknown database " + invoker.getDBName()); }
            return; // Not known database
        }

        // Notify all registered boxes
        int size = db.owners.size();
        for( int i = 0; i < size; i++) {
            PersistentBox box = (PersistentBox)db.owners.elementAt(i);
            box.loadedDB();
        }
    }
    
    public void changeUnreadMails(MailDB invoker, int count) {
        if (DEBUG) { System.out.println("DEBUG MailDBManager.changeUnreadMails( " + invoker.getDBName() + ", " + count + ")"); }

        structDBList db = (structDBList)dbList.get( invoker.getDBName());
        if (db == null) {
            if (DEBUG) { System.out.println("DEBUG MailDBMAnager.changeUnreadMails - unknown database " + invoker.getDBName()); }
            return; // Not known database
        }

        // Notify all registered InBoxes
        int size = db.owners.size();
        for( int i = 0; i < size; i++) {
            Object box = db.owners.elementAt(i);
            if (box instanceof InBox) {
                // Inbox or user folder
                ((InBox)box).changeUnreadMails(count);
            }
        }
    }
    
    /** Remove message from storages of all Boxes where message is stored */
    public void removeMessage(MessageHeader header) {
        if (DEBUG) { System.out.println("DEBUG MailDBManager.removeMessage( " + header + " ... " + header.getMailDB().getDBName() + ")"); }

        structDBList db = (structDBList)dbList.get( header.getMailDB().getDBName());
        if (db == null) {
            if (DEBUG) { System.out.println("DEBUG MailDBMAnager.removeMessage - unknown database " + header.getMailDB().getDBName()); }
            return; // Not known database
        }
        int size = db.owners.size();
        if (DEBUG) { System.out.println("DEBUG MailDBMAnager.removeMessage - deleting from  " + size + " boxes."); }
        for( int i = 0; i < size; i++) {
            TheBox box = (TheBox)(db.owners.elementAt(i));
            if (DEBUG) System.out.println("DEBUG MailDBMAnager.removeMessage - deleting from box " + box.toString());
            box.getStorage().removeMessage(header);
        }
    }
}

class structDBList{
    public MailDB   db;
    public Vector /*<PersistentBox>*/ owners;
        // Needed to know folders to call resort after loading completed
};