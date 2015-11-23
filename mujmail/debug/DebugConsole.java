//#condition MUJMAIL_DEBUG_CONSOLE
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

package mujmail.debug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

/**
 * Serves for printing debug texts on mobile phones. 
 * Class DebugConsoleUI serves for displaying of debug console.
 * 
 * @author David Hauzar
 */
public class DebugConsole {
    /** Set to true if you want persistent writes information into database */
    private static final boolean reallyPersistent = false;
    private static final String recordsDB = "MC_RECORDS_DB";
    
    /** Vector with memory stored record entries
     *   on application creation it loads persistent data
     *   on run-time it holds persistent and non persistent data
     */
    private static Vector records = new Vector();
    
  
    /**
     * Prints the string to the mobile console.
     * @param string the string to be printed to the console.
     */
    public static void print(String string) {
        System.out.print(string);
        addEntry(string, false);
    }

    /**
     * Prints the string to the mobile console.
     * @param string the string to be printed to the console.
     */
    public static void printPersistent(String string) {
        System.out.print(string);
        addEntry(string, true && reallyPersistent);
    }


    /**
     * Prints the string to the mobile console.
     * @param string the string to be printed to the console.
     */
    public static void println(String string) {
        System.out.println(string);
        addEntry(string, false);
    }

    /**
     * Prints the string to the mobile console.
     * @param string the string to be printed to the console.
     */
    public static void printlnPersistent(String string) {
        System.out.println(string);
        addEntry(string, true && reallyPersistent);
    }

    /**
     * Store entry into runtime storage and possibly into database too.
     * @param string String to store
     * @param persistent Flag signaling that string hata yto be stored into persistent database to.
     */
    private synchronized static void addEntry(String string, boolean persistent) {
        records.addElement(string);
        if ( persistent ) {
            RecordStore rs = null;
            try {
                rs = RecordStore.openRecordStore(recordsDB, true);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(buffer);

                stream.writeInt( rs.getNumRecords()); // Entry counter
                stream.writeUTF(string); // Entry text

                rs.addRecord(buffer.toByteArray(), 0, buffer.size());
            } catch (Exception ex) {
                System.err.println(ex.toString());
                ex.printStackTrace();
            } finally {
                try {
                    rs.closeRecordStore();
                } catch (Exception ex) {
                    System.err.println(ex.toString());
                    ex.printStackTrace();
                }
            }
        }
   }

    /**
     * Gets records of the mobile console.
     * @return the records of the mobile console.
     */
    public static Vector getRecords() {
        return records;        
    }
    
   /**
    * Remove current records.
    * @param persistent If true delete records from persistent storage too
    */
    public static synchronized void deleteRecords(boolean persistent) {
        records.removeAllElements();
        if (reallyPersistent && persistent) {
            try {
                RecordStore.deleteRecordStore(recordsDB);
            } catch (Exception ex) {
                System.err.println(ex.toString());
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Initial loading of persistent entries 
     * Call before start using DebugConsole
     */
    public static synchronized void loadEntries() {
            RecordStore rs = null;
            try {
                rs = RecordStore.openRecordStore(recordsDB, true);
                RecordEnumeration re = rs.enumerateRecords(null, new RMSComparator(), false);
                
                while (re.hasNextElement()) {
                    ByteArrayInputStream buffer = new ByteArrayInputStream(re.nextRecord());
                    DataInputStream stream = new DataInputStream(buffer);
                    stream.readInt(); // Entry counter
                    addEntry(stream.readUTF(), false);
                }
            } catch (Exception ex) {
                System.err.println(ex.toString());
                ex.printStackTrace();
            } finally {
                try {
                    rs.closeRecordStore();
                } catch (Exception ex) {
                    System.err.println(ex.toString());
                    ex.printStackTrace();
                }
            }
    }
}

/** Private class that helps sort entries from storage 
 * Reads Entry counter (first integer) and compare them
 * */
class RMSComparator implements RecordComparator {

    public int compare(byte[] arg0, byte[] arg1) {
        int v0 = 0;
        int v1 = 0;
        
        try {
            ByteArrayInputStream buffer1 = new ByteArrayInputStream(arg0);
            DataInputStream stream1 = new DataInputStream(buffer1);
            v0 = stream1.readInt();
        
            ByteArrayInputStream buffer2 = new ByteArrayInputStream(arg1);
            DataInputStream stream2 = new DataInputStream(buffer2);
            v1 = stream2.readInt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (v0 == v1) return RecordComparator.EQUIVALENT;
        if (v0 < v1) return RecordComparator.PRECEDES;
        return RecordComparator.FOLLOWS;
    }
    
}
