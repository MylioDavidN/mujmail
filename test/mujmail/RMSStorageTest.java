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
package test.mujmail;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

import mujmail.BodyPart;
import mujmail.Decode;
import mujmail.InBox;
import mujmail.MessageHeader;
import mujmail.MyException;
import mujmail.PersistentBox;
import mujmail.RMSStorage;
import mujmail.Settings;

/**
 * Tests for the class RMSStorage.
 * @author David Hauzar
 */
public class RMSStorageTest extends TestCase {
    /** Tests work with content storage of plain text bodypart in saved in normal mode */
    public final static String PLAIN_TEXT_BODYPART_NORMAL_MODE = "1";
    /** Tests work with content storage of plain text bodypart in saved in safe normal mode */
    public final static String PLAIN_TEXT_BODYPART_SAFE_MODE = "2";
    public final static String BINARY_BODYPART_NORMAL_MODE = "3";
    public final static String BINARY_BODYPART_SAFE_MODE = "4";

    protected void runTest() throws Throwable {
        if (getName().equals(PLAIN_TEXT_BODYPART_NORMAL_MODE)) {
            testPlainTextRMSStorage(false);
        }
        
        if (getName().equals(PLAIN_TEXT_BODYPART_SAFE_MODE)) {
            testPlainTextRMSStorage(true);
        }
        
        if (getName().equals(BINARY_BODYPART_NORMAL_MODE)) {
            testBinaryRMSStorage(false);
        }
        
        if (getName().equals(BINARY_BODYPART_SAFE_MODE)) {
            testBinaryRMSStorage(true);
        }
    }
    
    /**
     * Creates an rms storage.
     * @return rms storage.
     */
    private RMSStorage createRMSStorage(boolean binary) {
        PersistentBox box = new InBox("database", "inbox");
        MessageHeader header = new MessageHeader(box);
        BodyPart bp = new BodyPart(header);
        if (binary) {
            bp.getHeader().setEncoding(BodyPart.ENC_BASE64);
            bp.getHeader().setCharSet(BodyPart.CH_NORMAL);
        }
        RMSStorage storage = new RMSStorage(bp);
        
        return storage;
    }
    
    

    public Test suite() {
        return new TestSuite(new RMSStorageTest().getClass(), new String[] {
            PLAIN_TEXT_BODYPART_NORMAL_MODE,
            PLAIN_TEXT_BODYPART_SAFE_MODE,
            BINARY_BODYPART_NORMAL_MODE,
            BINARY_BODYPART_SAFE_MODE});
    }

    private Vector createVectorWithStrings() {

        Vector vector = new Vector(3);
        vector.addElement("Ahoj ");
        vector.addElement("drahy ");
        vector.addElement("kamarade.");
        return vector;
    }
    
    private Vector createVectorWithStringsOfNumbers() {

        Vector vector = new Vector(3);
        vector.addElement("1234");
        vector.addElement("5678");
        vector.addElement("9123");
        return vector;
    }

    private byte[] getByteArray(String savedContent) throws MyException {
        ByteArrayOutputStream decodedBodyB = Decode.decodeBase64(savedContent);
        byte[] savedContentRaw = decodedBodyB.toByteArray();
        return savedContentRaw;
    }

    private StringBuffer itterativelySaveContentToStorage(Vector savedContents, RMSStorage storage) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < savedContents.size(); i++) {
            String contentPart = (String) savedContents.elementAt(i);
            try {
                storage.addToContentBuffered(contentPart);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sb.append(contentPart);
        }
        try {
            storage.flushBuffer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return sb;
    }

    /**
     * Save and load rms storage test. Writes a string to rms storage and tests
     * whether loaded string is the same then deletes the content of the storage
     * and saves to the storage iterratively more strings and compares whether
     * it loades them all.
     * @param safeMode
     * @throws mujmail.MyException
     */
    
    private void testPlainTextRMSStorage(boolean safeMode) throws Throwable {
        Settings.safeMode = safeMode;
        RMSStorage storage = createRMSStorage(false);
        String savedContent = "The content of the storage";
        
        saveAndLoadString(storage, savedContent);
        storage.deleteContent();
        saveToMorePartsAndLoadString(storage, createVectorWithStrings());
    }
    
    /**
     * See documentation of plainTextRMSStorageTest - this is analogy of this
     * test for binary data.
     * @param safeMode
     * @throws mujmail.MyException
     */
    private void testBinaryRMSStorage(boolean safeMode) throws MyException {
        Settings.safeMode = safeMode;
        RMSStorage storage = createRMSStorage(true);
        String savedContent = "0123";
        
        saveAndLoadBinary(storage, savedContent);
        storage.deleteContent();
        saveToMorePartsAndLoadBinary(storage, createVectorWithStringsOfNumbers());
    }

    private void saveAndLoadBinary(RMSStorage storage, String savedContent) throws MyException {
        try {
            storage.addToContentBuffered(savedContent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            storage.flushBuffer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertEquals( new ByteArray(getByteArray(savedContent)), new ByteArray(storage.getContentRaw()));
        assertTrue(storage.isContentRaw());
    }
    
    private void writeByteArray(byte[] array) {
        System.out.println("To string: " + array.toString());
        System.out.println("Lenght: " + array.length);
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + "; ");
        }
    }
    
    private void saveToMorePartsAndLoadBinary(RMSStorage storage, Vector savedContents) throws MyException {
        StringBuffer sb = itterativelySaveContentToStorage(savedContents, storage);
        
        assertTrue(storage.isContentRaw());
        assertEquals(new ByteArray(getByteArray(sb.toString())), new ByteArray(storage.getContentRaw()));
        
    }
    
    private void saveToMorePartsAndLoadString(RMSStorage storage, Vector savedContents) throws Throwable {
        StringBuffer sb = itterativelySaveContentToStorage(savedContents, storage);
        assertEquals(sb.toString(), storage.getContent());
        assertTrue(!storage.isContentRaw());
        
    }

    private void saveAndLoadString(RMSStorage storage, String savedContent) throws Throwable {
        try {
            // save and load
            storage.addToContentBuffered(savedContent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            storage.flushBuffer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertEquals(savedContent, storage.getContent());
        assertTrue(!storage.isContentRaw());
    }
    
    private static class ByteArray {
        private final byte[] array;

        public ByteArray(byte[] array) {
            this.array = array;
        }

        public boolean equals(Object object) {
            if ( !(object instanceof ByteArray) ) {
                return false;
            }
            
            byte[] otherArray = ((ByteArray) object).array;
            if (array.length != otherArray.length) {
                return false;
            }
            
            for (int i = 0; i < array.length; i++) {
                if (array[i] != otherArray[i]) {
                    return false;
                }
            }
            
            return true;
        }

        public int hashCode() {
            return super.hashCode();
        }
        
        
        
        
    }
    
    

}
