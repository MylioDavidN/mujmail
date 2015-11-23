package mujmail;

/*
MujMail - Simple mail client for J2ME
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

//defines exceptional and erroral constants 
public class MyException extends Exception {

    public final static byte COM_BASE = 1;
    public final static byte COM_IN = COM_BASE + 1;
    public final static byte COM_OUT = COM_BASE + 2;
    public final static byte COM_TIMEOUT = COM_BASE + 3;
    public final static byte COM_HALTED = COM_BASE + 4;
    public final static byte COM_UNKNOWN = COM_BASE + 5;
    public final static byte SYSTEM_BASE = 10;
    public final static byte SYS_OUT_OF_MEMORY = SYSTEM_BASE + 1;
    public final static byte SYS_IMAGE_FAILED = SYSTEM_BASE + 2;
    public final static byte PROTOCOL_BASE = 20;
    public final static byte PROTOCOL_CANNOT_CONNECT = PROTOCOL_BASE + 1;
    public final static byte PROTOCOL_CANNOT_RETRIEVE_BODY = PROTOCOL_BASE + 2;
    public final static byte PROTOCOL_COMMAND_NOT_EXECUTED = PROTOCOL_BASE + 3;
    public final static byte PROTOCOL_CANNOT_DELETE_MAILS = PROTOCOL_BASE + 4;
    public final static byte PROTOCOL_CANNOT_GET_URL =  PROTOCOL_BASE + 5;
    public final static byte VARIOUS_BASE = 30;
    public final static byte VARIOUS_BAD_EMAIL = VARIOUS_BASE + 1;
    public final static byte VARIOUS_AB_MULTIPLE_ENTRIES = VARIOUS_BASE + 2;
    public final static byte VARIOUS_DECODE_ILLEGAL_MIME = VARIOUS_BASE + 3;
    public final static byte DB_BASE = 50;
    public final static byte DB_NOSPACE = DB_BASE + 1;
    public final static byte DB_CANNOT_CLEAR = DB_BASE + 2;
    public final static byte DB_CANNOT_SAVE_BODY = DB_BASE + 3;
    public final static byte DB_CANNOT_SAVE_HEADER = DB_BASE + 4;
    public final static byte DB_CANNOT_DEL_HEADER = DB_BASE + 5;
    public final static byte DB_CANNOT_DEL_BODY = DB_BASE + 6;
    public final static byte DB_CANNOT_DEL_MAIL = DB_BASE + 7;
    public final static byte DB_CANNOT_LOAD_BODY = DB_BASE + 8;
    public final static byte DB_CANNOT_LOAD_CONTACT = DB_BASE + 9;
    public final static byte DB_CANNOT_SAVE_CONTACT = DB_BASE + 10;
    public final static byte DB_CANNOT_DEL_CONTACT = DB_BASE + 11;
    public final static byte DB_CANNOT_UPDATE_HEADER = DB_BASE + 12;
    public final static byte DB_CANNOT_LOAD_SETTINGS = DB_BASE + 13;
    public final static byte DB_CANNOT_SAVE_SETTINGS = DB_BASE + 14;
    public final static byte DB_CANNOT_LOAD_ACCOUNTS = DB_BASE + 15;
    public final static byte DB_CANNOT_SAVE_ACCOUNT = DB_BASE + 16;
    public final static byte DB_CANNOT_DELETE_ACCOUNT = DB_BASE + 17;
    public final static byte DB_CANNOT_OPEN_DB = DB_BASE + 18;
    public final static byte DB_CANNOT_LOAD_HEADERS = DB_BASE + 19;
    public final static byte DB_CANNOT_CLOSE_DB = DB_BASE + 20;
    public final static byte DB_CANNOT_SAVE_MSGID = DB_BASE + 21;
    public final static byte DB_CANNOT_LOAD_MSGID = DB_BASE + 22;
    public final static byte DB_CANNOT_DEL_MSGID = DB_BASE + 23;
    public final static byte DB_CANNOT_SAVE = DB_BASE + 24; // More generic versions
    public final static byte DB_CANNOT_DEL = DB_BASE + 25;
    
    private int errorCode = 0;
    private String details = null;

    public MyException(int errorCode) {
        this.errorCode = errorCode;
    }

    public MyException(int errorCode, String details) {
        this.errorCode = errorCode;
        this.details = details;
    }

    /** 
     * Creates MyException. As detail string uses 
     *   standard string associated with exception suffixed by 
     *  given exception e description.
     * @param errorCode Error code from prepared contants
     * @param e Exception that is add as suffinx into {@link #delails}
     */
    public MyException(int errorCode, Throwable e) {
        this.errorCode = errorCode;
        this.details = Lang.get((short)(Lang.EXP_BASE + errorCode)) + " : " + e.toString();
    }

    public String getDetails() {
        if (details != null) {
            return details;
        }
        return Lang.get((short) (Lang.EXP_BASE + errorCode));
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    public String getDetailsNocode() {
        String result = getDetails();
        if (result.startsWith("100:") || result.startsWith("200:") || result.startsWith("300:")) {
            return result.substring(4);
        }
        return result;
    }
}
