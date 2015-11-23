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

import java.io.IOException;

/** 
 * Used by Connections classes as abstraction of different 
 *      methods of communications (Socket, HTTP, ...)
 * Note: Not intended to direct communication from application level
 */
public interface ConnectorInterface {
    /** Specify that connector have to use inbuild (firmware) version of ssl. Used for openning connection. */
    public static final byte nativeSSL = 0;
    /** Specify that connector have to use own library verion of ssl. Used for openning connection. */
    public static final byte mujMailSSL = 1;

    /**
     * Sends data from given buffer.
     *
     * @param data Buffer with data to send.
     * @param off Start position in buffer.
     * @param len How many bytes send.
     * <p>
     * See {@link java.io.OutputStream#write(byte[], int, int) }
     */
    public void write(byte[] data, int off, int len) throws IOException;

    /**
     * Reads data from input and fill given buffer.
     * @param buffer Buffer where new incomming data to be stored.
     * @param offset Position in buffer buff from where data will be stored.
     * @param length Maximal number of bytes to be read into buffer.
     * @return Number of new bytes stored buffer.
     * <p>
     * See {@link java.io.InputStream#read(byte[], int, int) }
     */
    public int read(byte[] buffer, int offset, int length) throws IOException;

    /**
     * Returns the number of bytes that can be read (or skipped over) from this 
     * input stream without blocking by the next caller of a method for this 
     * input stream. The next caller might be the same thread or another thread.
     * 
     * @return the number of bytes that can be read from this input stream 
     *  without blocking.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public int available() throws IOException;
    
    /** 
     * Get info about if connectors available method - if works correctly or not.
     * 
     * @return true if available method have problem with precise byte counting.
     *          if bug present it excepts non blocking read that return 0 
     *          if nodata available
     */
    public boolean available_bug();

    /** Ends connection with server */
    public void close() throws IOException;

    /** Send all buffered data */
    public void flush() throws IOException;

    /** 
     * Create new connection to server
     * 
     * @param url Address of server. Usually DNS name or IP adress
     * @param ssl Cipher connection or not
     * @param sslType Use mobile inbuild or library SSL
     * @throws java.io.IOException Throws if connection cann't be established. 
     *          Server switch off, no network coverage, ...
     */
    public void open(String url, boolean ssl, byte sslType) throws IOException;
}
