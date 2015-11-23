//#condition MUJMAIL_SEARCH
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

package mujmail.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordStore;

/**
 * Reminds the value of given variable.
 * Saves the value of the variable to the RMS database.
 * 
 * @author David Hauzar
 */
public abstract class PersistentValueReminder {
    private final String dbID;
    protected Object wasSelected;
    private boolean loaded = false;
    /** Used to create name of databases that stores the selected state. */
    public static final String DB_PREFIX = "wsr_";

    /**
     * Creates the instance of reminder.
     * @param dbID the RMS database id to that save the value of the variable.
     */
    public PersistentValueReminder(String dbID) {
        this.dbID = dbID;
    }



    /**
     * Reminds the value of the variable.
     * @return the value of the variable;
     */
    protected Object remindValue() {
        if (!loaded) {
            loadWasSelected();
        }
        return wasSelected;
    }

    /**
     * Sets variable to be stored and saves it.
     * @param variable the value of the variable to be stored.
     */
    protected void setVariable(Object variable) {
        wasSelected = variable;
        saveVariable();
    }

    /**
     * Writes the value of stored variable to the stream.
     * @param stream the stream to that write the value of stored variable.
     * @throws java.lang.Exception
     */
    protected abstract void writeVariable(DataOutputStream stream) throws Exception;
    /**
     * Reads the variable from the stream.
     * @param stream the strea from that the value should be read.
     * @return the variable written in the stream
     * @throws java.lang.Exception
     */
    protected abstract Object readVariable(DataInputStream stream) throws Exception;
    /**
     * Returns default variable - the variable that is reminded in case that it
     * was not possible to read it's value from the stream.
     * @return the default value of the variable.
     */
    protected abstract Object defaultVariable();

    /**
     * Save the value of the variable to the persistent storage (RMS database).
     */
    private void saveVariable() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(dbID, true);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(buffer);

            writeVariable(stream);
            stream.flush();
            if (rs.getNumRecords() == 1) {
                rs.setRecord(1, buffer.toByteArray(), 0, buffer.size());
            } else {
                rs.addRecord(buffer.toByteArray(), 0, buffer.size());
            }
        } catch (Exception e) {
        } finally {
            try {
                rs.closeRecordStore();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Load the value of the variable to persistent storage (RMS database).
     */
    private void loadWasSelected() {
        loaded = true;

        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(dbID, true);
            ByteArrayInputStream buffer = new ByteArrayInputStream(rs.getRecord(1));
            DataInputStream stream = new DataInputStream(buffer);

            wasSelected = readVariable(stream);

        } catch (Exception e) {
            wasSelected = defaultVariable();
        } finally {
            try {
                rs.closeRecordStore();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Reminds whether given boolan variable was true or false.
     * Saves the value of the variable to the RMS database.
     */
    public static class PersistentBooleanValueReminder extends PersistentValueReminder implements SaveableBooleanValue {

        /**
         * Creates the instance of reminder.
         * @param dbID the RMS database id to that save the value of the variable.
         */
        public PersistentBooleanValueReminder(String dbID) {
            super(dbID);
        }
        

        protected Object defaultVariable() {
            return new Boolean(true);
        }

        protected Object readVariable(DataInputStream stream) throws Exception {
            return new Boolean( stream.readBoolean() );
        }

        protected void writeVariable(DataOutputStream stream) throws Exception {
            stream.writeBoolean( ((Boolean)wasSelected).booleanValue());
        }

        /**
         * Reminds the value of the variable.
         * @return true if the value of the variable was true.
         *  false if the value of the variable was false;
         */
        public boolean loadBoolean() {
            return ((Boolean)super.remindValue()).booleanValue();
        }

        /**
         * Sets value of the variable to be stored and stores it to be reminded
         * layer.
         * @param variable the value of the variable to be stored.
         */
        public void saveBoolean(boolean variable) {
            super.setVariable(new Boolean( variable ));
        }
        
    }
}
