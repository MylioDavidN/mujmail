/*
MujMail - Simple mail client for J2ME
Copyright (C) 2003 Petr Spatka <petr.spatka@centrum.cz>
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2008 Alf <melmeck@someWhere.inSpace>
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

package mujmail.connections;

import mujmail.MyException;

/** 
 * Interface used for communication with servers.
 * <p>Create line oriented communication abstraction
 * <p>Intended for usage from higher level methods. 
 * <p> Note: Replace BasicConnection 
 */
public interface ConnectionInterface {
    /** New line constant */
    public static final String CRLF = "\r\n";

    /** 
     * Create new connection to server
     * 
     * @param url Address of server. Usually DNS name or IP address
     * @param ssl Cipher connection or not
     * @param sslType Use mobile inbuild or library SSL
     * @throws java.io.IOException Throws if connection can't be established. 
     *          Server switch off, no network coverage, ...
     */
    public abstract void open(String url, boolean ssl, byte sslType) throws MyException;

    /** 
     * Try to ends connection with server
     */
    public abstract void close(); 

    /** 
     * Checks if connection was opened.
     * @return true if successfully connected to server
     */
    public boolean isConnected();

    /** 
     * Marks connection to by closed, no more data read and send if marked.
     * Note: Don't close connection. It has to by done by close call
     */
    public void quit();

    /** Remove quit mark, so connection can be again used if not closed before */
    public void unQuit();

    /**
     * Sent line (or multiline) to server.
     *  Adds Newline constant to end of string
     * @param command the line to be written
     * @throws MyException if connection is closed or transmission error
     */
    public void sendCRLF(String command) throws MyException;

    /**
     * Sent line (or multiline) to server.
     * @param command the line to be written
     * @throws MyException if connection is closed or transmission error
     */
    public void send(String command) throws MyException;
    
    /**
     * Buffer to server.
     * @param command bytes to be sent
     * @throws MyException if connection is closed or transmission error
     */
    public void send(byte[] command) throws MyException;

    /**
     * Gets line from input stream.
     * @return String that contains line from input.
     */
    public String getLine() throws MyException;

    /**
     * Get back last readed line from connection.
     * Next {@link #getLine} call returns same string again.
     */ 
    public void unGetLine();

    /**
     * Read all data availeble in input stream.
     * <p>Note: Typically used by protocols (IMAP, POP3, SMTP) 
     *             before commands (like SELECT, NOOP...) to ensure 
     *             that input to be read from connection
     *             is servers answer to executed command 
     *             and no result of any previous command 
     *             that remain unread in buffer.
     */
    public void clearInput() throws MyException;
    
    /**
     * Returns the number of bytes that can be read (or skipped over) from this 
     * input stream without blocking by the next caller of a method for this 
     * input stream.
     * @return Byte to read
     * Note: If available bug present, than can return 0 if some data pending
     */
    public boolean available();
    
}
