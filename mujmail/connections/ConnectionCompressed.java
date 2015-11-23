package mujmail.connections;
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

import java.io.*;
import javax.microedition.io.*;

import mujmail.Lang;
import mujmail.MyException;

/**
 * Implements ConnectionInterface that compress data. (Compression filter)
 * Now using RLE "compression".
 * No output buffering, no flush.
 * InOut is line oriented
 */
 public class ConnectionCompressed implements ConnectionInterface {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;
    
    /** Connection to sending compressed data */
    private ConnectorInterface connector = null;
    /** Indicates whether is connection open or close */
    private boolean connected = false;

    private boolean quit = false;
    
    /** String that identify compression type.
     *  Note: Pointer to one of predefined constants, no content comparison
     */
    private String compression = COMPRESSION_TYPE_NONE; // None have to be default
    
    /** Compression type identification constants */
    /** No compression is used for communication. */
    public static final String COMPRESSION_TYPE_NONE    = "NONE";
    //#ifdef MUJMAIL_COMPRESSED_CONNECTION
    /** Communication with server is compressed by RLE compression. */
    public static final String COMPRESSION_TYPE_RLE     = "RLE";
    /** Communication from server is compressed by GZIP compression. */
    public static final String COMPRESSION_TYPE_GZIP    = "GZIP";

    /** Communication initialization constants */
    private static final String INIT_HELLO = "Xmujmail-compression";
    private static final String INIT_VERSION = "0.5";
    //#endif

    /** Counts changes of compression type */
    private static int counter = 0;
    
    /** Count send data. Data incoming from higher level into send method */
    private int dataSendIn = 0;
    /** Count read data. Data gets to higher level from getLine method */
    private int dataReadIn = 0;
    
    private static final int BUFFER_LEN = 256;
    /** Buffer for outgoing compression data */
    private InOutBuffer buff = null;
    
    private Object syncIn = new Object();
    private Object syncOut = new Object();

    /** Last that was take back */
    private StringBuffer lastLine = new StringBuffer();
    /** Mark whether we want to get the last line back or new data from buffer */
    private boolean backMark;

    /*** RLE Compression specifies stuff */
    // Hold state of RLE from last stop
    private char RLE_Char;
    private int  RLE_Count = 0; // How many times put into output

    /** Creates new socket connection. 
     * <p>{@link mujmail.connections.ConnectorSocket} are used for communication.
     * <p>No compression is set at first.
     */
    public ConnectionCompressed() {
        connector = new ConnectorSocket();
        buff = new InOutBuffer( BUFFER_LEN, connector);
    }

    /**
     * Construct connection proxy
     * @param _connector Connector used for data delivery. 
     *          If null standard ConnectorSocket will be used.
     * @param _compression Initial type of compression
     * 
     */
    public ConnectionCompressed(ConnectorInterface _connector, String _compression) {
        if (_connector != null) 
            connector = _connector;
        else
            connector = new ConnectorSocket();
        compression = _compression;
        buff = new InOutBuffer( BUFFER_LEN, connector);
    }
    
    
    public synchronized void close() {
        connected = false;
        buff.InSkipBuffer(); // Flushing buffer
        try {
            if (connector != null) connector.close();
        } catch (IOException ex) {
            // Connection closing problem, let is connection closed by timeout
        }
    
    }

    public synchronized void open(String url, boolean ssl, byte sslType) throws MyException {
        if (connector == null) return;
        compression = COMPRESSION_TYPE_NONE;
        try {
            connector.open(url, ssl, sslType);
        } catch (ConnectionNotFoundException ex) {
            ex.printStackTrace();
            throw new MyException(MyException.COM_UNKNOWN, Lang.get( Lang.EXP_COM_UNKNOWN) + " Server not found." + ex.getMessage()); // Try to open invalid location
        } catch (EOFException ex) {
            ex.printStackTrace();
            throw new MyException(MyException.COM_HALTED);
        } catch (IOException ex) {
            //System.out.println(ex.getMessage());
            //ex.printStackTrace();
            ex.printStackTrace();
            throw new MyException(MyException.COM_HALTED, ex);
        }
        connected = true;
    }
    
    //#ifdef MUJMAIL_COMPRESSED_CONNECTION
     /** 
     * Sets new compression type
     * 
     * @param newCompression Compression type to uses. 
     *      It have to be one from predefined strings in class.
     *      Test is done string addressed (not content)
     * <p>
     * Note: Checks if server counter support compression. Sends data into output.
     */
    public void changeCompression(String newCompression) throws MyException {
        if (connector == null) return;

    // Check if newCompression is supported 
        // Note not comparing strings but addresses
        if ( (newCompression != COMPRESSION_TYPE_NONE) &&
             (newCompression != COMPRESSION_TYPE_RLE) &&
             (newCompression != COMPRESSION_TYPE_GZIP) ) {
            return;
        }

        // GZIP compression cann't be changed unsupported
        if ( (compression == COMPRESSION_TYPE_GZIP) && 
             (newCompression != COMPRESSION_TYPE_GZIP) ) {
            return;
        }

        // Place for preinitialization
       
        // Setting compression

        String tag = "compress" + counter++ + " ";
        String command = tag + INIT_HELLO + " " + INIT_VERSION + " " + newCompression; // Add necessary compression type initialization informations if needed
        String result = null;
        String oldCompression = compression;
        try {
            sendCRLF(command);
            result = getLine(); // Last line in old compression

            while (!result.startsWith(tag)) { //multiline response
                result = getLine();
            }
            
            result = result.substring(tag.length());

            if (result.startsWith("OK")) { // All correct - set parameters
                compression = newCompression; // This have to be set here, to connector.flush works correctly
                if (compression == COMPRESSION_TYPE_GZIP) {
                    connector = new ConnectorGZip(connector, buff);
                    buff.changeConnector(connector);
                    connector.flush();
                }
                getLine(); // To read server reply "* compression OK"
            }
        } catch ( IOException ex) {
            // We need close connection immediately, 
            //   because each side tries to use different compression
            compression = oldCompression;
            throw new MyException( MyException.COM_UNKNOWN, ex); 
        }
    }
    //#endif
    
    public void sendCRLF(String command) throws MyException {
        send((command + CRLF).getBytes());
    }

    /**
     * Send line (or multiline) to server.
     * @param command The line to be written.
     * @throws MyException If connection is closed or transmission error
     */
    public void send(String command) throws MyException {
       send(command.getBytes());
    } 
    
    /**
     * Buffer to server.
     * @param command bytes to be sent
     * @throws MyException if connection is closed or transmission error
     * Note: Have to be synchronized ... share one instance of output buffer
     */
    public void send(byte[] command) throws MyException {
        if (connector == null ) return;
        if (command == null || command.length == 0) return;

        if (quit) {
            throw new MyException(MyException.COM_HALTED);
        }
        
        synchronized (syncIn) {
            // Counting send (uncompressed)data 
            dataSendIn += command.length;
            
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if (compression == COMPRESSION_TYPE_RLE) {
                try {
                    byte prevByte = command[0];
                    int inBufPos = 1; // Position in input buffer -- command
                    int prevByteCount = 1;
                    while (inBufPos < command.length) {
                        if (command[inBufPos] == prevByte) {
                            // Same character as before
                            inBufPos++;
                            prevByteCount++;
                            
                            // Test if length of same char in not too long
                            if (prevByteCount == 18) {
                                // To many same character, put out sequence
                                buff.OutAddByte((byte)(0xFF)); // Writes 17times same char
                                buff.OutAddByte(prevByte);
                                prevByteCount = 1;
                            }
                        } else {
                            // Different character in the input --> write current run
                            if ((command[inBufPos] & 0xF0) == 0xF0 || prevByteCount > 1) {
                                // Longer run or problematic char --> double char code
                                buff.OutAddByte((byte)(0xF0 + prevByteCount - 1));
                                buff.OutAddByte(prevByte);
                            } else {
                                // Single non problematic char (not binary 1111????) -> one byte code
                                buff.OutAddByte(prevByte); 
                            }
                            // Read next char from input
                            prevByte = command[inBufPos];
                            prevByteCount = 1;
                            inBufPos++;
                        }
                    } // End while

                    // Process last character (cann't be too long)
                    if ( prevByte == 0xF0 || prevByteCount > 1) {
                        // Longer run or problematic char --> double char code
                        buff.OutAddByte((byte)(0xF0 + prevByteCount - 1));
                        buff.OutAddByte(prevByte);
                    } else {
                        // Single non problematic char (not binary 1111????) -> one byte code
                        buff.OutAddByte(prevByte); 
                    }
                    
                    buff.OutFlush();
                } catch (EOFException ex) {
                    throw new MyException(MyException.COM_HALTED);
                } catch (IOException e) {
                    throw new MyException(MyException.COM_OUT, e);
                }
                return;
            } // end RLE compression
            //#endif

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            // Sending none compression
            if ( (compression == COMPRESSION_TYPE_NONE) ||
                 (compression == COMPRESSION_TYPE_GZIP) ) {
            //#endif
                try {
                    connector.write(command, 0, command.length);
                    connector.flush();
                } catch (EOFException ex) {
                    throw new MyException(MyException.COM_HALTED);
                } catch (IOException e) {
                    throw new MyException(MyException.COM_OUT, e);
                }
                return;
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            } // end None compression
            //#endif
        } // end synchronized
    } // end send

    public String getLine() throws MyException {
        if (connector == null ) return null;

        synchronized (syncOut) {
            if (backMark) {
                backMark = false;
                if (DEBUG) { System.out.println( "DEBUG ConnectionCompressed.getLine() =" + lastLine.toString()); }
                return lastLine.toString();
            }
        
            lastLine.delete(0, lastLine.length());
            char ch;

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if ( (compression == COMPRESSION_TYPE_NONE) ||
                 (compression == COMPRESSION_TYPE_GZIP) ){
            //#endif
                while (!quit) {
                    ch = (char) buff.InGetByte();
                    lastLine.append(ch);
                    if (ch == '\n') {
                        dataReadIn += lastLine.length();
                        if (DEBUG) { System.out.println( "DEBUG ConnectionCompressed.getLine() =" + lastLine.toString()); }
                        return lastLine.toString();
                    }
                }
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            }
            //#endif

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            int i1;
            int i2;
            if (compression == COMPRESSION_TYPE_RLE) {
                try {
                    while (!quit) {
                        // Appending into output string part
                        // Check if any work to do
                        if ( RLE_Count > 0) {
                            if (RLE_Char == '\n') { // End of line --> stop decoding
                                RLE_Count--;
                                lastLine.append(RLE_Char);
                                dataReadIn += lastLine.length();
                                return lastLine.toString();
                            }
                            for(int i = 0; i < RLE_Count; i++)
                                lastLine.append(RLE_Char);
                        }

                        // Decoding part
                        i1 = buff.InLookInBuffer(0);
                        i2 = buff.InLookInBuffer(1);
                        if (i1 == InOutBuffer.NO_DATA_IN_BUFFER)
                            buff.InFillBuffer(true);
                        i1 = buff.InLookInBuffer(0); // i1 != InOutBuffer.NO_DATA_IN_BUFFER ... timeout exception guards this
                        i2 = buff.InLookInBuffer(1);

                        if ((i1 & 0xF0) == 0xF0) {
                            // Two byte char
                            if (i2 == InOutBuffer.NO_DATA_IN_BUFFER)
                                buff.InFillBuffer(true);
                            i2 = buff.InLookInBuffer(1);
                            buff.InGetByte();
                            buff.InGetByte();
                            RLE_Count = (i1 & 0x0F) + 1;
                            RLE_Char = (char)i2;
                        } else {
                            buff.InGetByte();
                            RLE_Char = (char)i1;
                            RLE_Count = 1;
                        }
                    } // end While quit
                } catch (MyException e) {
                    // Repait RLE state
                    RLE_Count = 0;
                    throw e;
                } catch (EOFException ex) {
                    RLE_Count = 0;
                    throw new MyException(MyException.COM_HALTED);
                } catch (IOException e) {
                    RLE_Count = 0;
                    throw new MyException(MyException.COM_IN, e);
                }
            } // end RLE
            //#endif

            // TODO getIt into InOutBuffer filling routine to quit connection as soon as possible
            if (quit) { //user pressed the stop button
                throw new MyException(MyException.COM_HALTED);
            }
            

        } // end sync

        //this situation never happens, cause it either throws TIMEOUT exception, 
        //or returns a line or another exception
        //all cases are handled above, but its here because JAVA wants it :)
        return "";
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public void unGetLine() {
        synchronized (syncIn) {
            backMark = true;
        }
    }
    //note: the available() method doesn't work with some SSL implementation
    //so practically, the POP3 and IMAP4 implementations 
    //are responsible for disconnecting from the server to get the input really cleared!
    public void clearInput() throws MyException {
        if (connector == null ) return;
        if (connected == false) return; 
        if (quit) {
            throw new MyException(MyException.COM_HALTED);
        }
        synchronized (syncIn) {
            backMark = false;

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if ( (compression == COMPRESSION_TYPE_NONE) ||
                 (compression == COMPRESSION_TYPE_GZIP) ) {
            //#endif
                try {
                    buff.InFillBuffer(false);
                    while (buff.InLookInBuffer(0) != InOutBuffer.NO_DATA_IN_BUFFER) {
                        buff.InSkipBuffer();
                        buff.InFillBuffer(false);
                    }
                } catch (EOFException ex) {
                    throw new MyException(MyException.COM_HALTED);
                } catch (IOException e) {
                    throw new MyException(MyException.COM_IN, e);
                }
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            } // End no compressed
            //#endif

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if (compression == COMPRESSION_TYPE_RLE) {
                try {
                    byte b;
                    // Decode current buffer

                    // Check if char available in buffer
                    while ( !((buff.InLookInBuffer(0) == InOutBuffer.NO_DATA_IN_BUFFER) || 
                            (((buff.InLookInBuffer(0) & 0xF0) == 0xF0) && 
                            (buff.InLookInBuffer(1) == InOutBuffer.NO_DATA_IN_BUFFER))) ) {
                        
                        // Check if char available in buffer
                        while ( !((buff.InLookInBuffer(0) == InOutBuffer.NO_DATA_IN_BUFFER) || 
                                (((buff.InLookInBuffer(0) & 0xF0) == 0xF0) && 
                                (buff.InLookInBuffer(1) == InOutBuffer.NO_DATA_IN_BUFFER))) ) {
                            b = buff.InGetByte();
                            if ((b & 0xF0) == 0xF0) buff.InGetByte();
                        }
                        buff.InFillBuffer(false);
                    }
                } catch (EOFException ex) {
                    throw new MyException(MyException.COM_HALTED);
                } catch (IOException e) {
                    throw new MyException(MyException.COM_IN, e);
                }
            } // End RLE compression
            //#endif
        } // end sync
    }

    public boolean available() {
        if (connector == null) return false;
        if (connected == false) return false;
        try {
	        if (((ConnectorSocket)connector).getInputStream().available() > 0) {
	        	return true;
	        }
	        else {
	        	return false;
	        }
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
        synchronized (syncIn) {
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if ( (compression == COMPRESSION_TYPE_NONE) ||
                 (compression == COMPRESSION_TYPE_GZIP) ) {
            //#endif
                    if (buff.InLookInBuffer(0) != InOutBuffer.NO_DATA_IN_BUFFER) {
                        return true;
                    }
                    try {
                        if (connector.available() > 0) return true;
                    } catch (Exception e) {}
                    return false;
            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            }
            //#endif    

            //#ifdef MUJMAIL_COMPRESSED_CONNECTION
            if (compression == COMPRESSION_TYPE_RLE) {
                    if (buff.InLookInBuffer(1) != InOutBuffer.NO_DATA_IN_BUFFER) return true;
                    if (buff.InLookInBuffer(0) != InOutBuffer.NO_DATA_IN_BUFFER && (buff.InLookInBuffer(0) & 0xF0) != 0xF0 ) return true;
                    try {
                        if (connector.available() > 1) return true;
                    } catch (Exception e) {}
                    // TODO available_bug --> what TODO? in separate tread try to still read?
                    return false;
            }
            //#endif
        } // End synch 
        //#ifdef MUJMAIL_COMPRESSED_CONNECTION
        return false;
        //#endif
    }
    
    public void quit() {
        quit = true;
    }

    public void unQuit() {
        quit = false;
    }

}
