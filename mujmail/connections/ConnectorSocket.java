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

package mujmail.connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.StreamConnection;

import mujmail.Properties;
//#ifdef MUJMAIL_SSL
import org.bouncycastle.crypto.tls.TlsProtocolHandler;
//#endif
//#ifdef MUJMAIL_DEBUG_CONSOLE
import mujmail.debug.DebugConsole;
//#endif

/**
 * Implements ConnectorInterface using network sockets.
 * Mostly used connector.
 */
public class ConnectorSocket implements ConnectorInterface {

    /**
     * Some implementation of SSL can not work with availalbe() properly
     * This mark problematic avail method.
     */
    private boolean available_bug = false;

    private OutputStream outputStream;
    private InputStream inputStream;
    private StreamConnection streamConnection;
    
    //#ifdef MUJMAIL_SSL
    private TlsProtocolHandler tlsHandler = null;
    private boolean usemujMailSSL;
    //#endif

    private static StringBuffer tmp_buf = null; /// Buffer for debugging output
    
    /** 
     * Creates connector that uses socket to communitate over network.
     */
    public ConnectorSocket() {
        if ( Properties.debugConnections )
            tmp_buf = new StringBuffer(512);
    }
    
    public InputStream getInputStream() {
    	return inputStream;
    }
    
    public OutputStream getOutputStream() {
    	return outputStream;
    }

    public void write(byte[] data, int off, int len) throws IOException {
        if (Properties.debugConnections) {
            //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.printPersistent("Debug protocol out : ");
            //#else
//#             System.out.print("Debug protocol out : ");
            //#endif
            tmp_buf.delete(0, tmp_buf.length());
            for( int i = 0; i < data.length && data[i] != 0; ++i) 
                if ((char)data[i]!='\r') tmp_buf.append((char)data[i]);
            //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.print( tmp_buf.toString());
            //#else
//#             System.out.print( tmp_buf.toString());
            //#endif
        }
        //#ifdef MUJMAIL_SSL
        if (usemujMailSSL) {
            tlsHandler.getOutputStream().write(data, off, len);
        } else {
        //#endif
            outputStream.write(data, off, len);
        //#ifdef MUJMAIL_SSL
        }
        //#endif
        flush();
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (Properties.debugConnections)
            //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.printPersistent("Debug protocol in : ");
            //#else
//#             System.out.print("Debug protocol in : ");
            //#endif

        int result = 0;
        //#ifdef MUJMAIL_SSL
        if (usemujMailSSL) {
            //System.out.println("Reading data using SSL");
            result = tlsHandler.getInputStream().read(buffer, offset, length);
        } else {
        //#endif
            result = inputStream.read(buffer, offset, length);
        //#ifdef MUJMAIL_SSL
        }
        //#endif

        if (Properties.debugConnections) {
            tmp_buf.delete(0, tmp_buf.length());
            for( int i = 0; i < result; ++i) 
                if ((char)buffer[i]!='\r') tmp_buf.append((char)buffer[i]);
            //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.printPersistent( tmp_buf.toString());
            //#else
//#             System.out.print( tmp_buf.toString());
            //#endif
        }
        return result;
    }

    //returns how many bytes are ready in the inputStream for further reading
    public int available() throws IOException {
        //#ifdef MUJMAIL_SSL
        if (usemujMailSSL) {
            return tlsHandler.getInputStream().available();
        } else {
        //#endif
            return inputStream.available();
        //#ifdef MUJMAIL_SSL
        }
        //#endif
    }

    public void close() throws IOException {
        //#ifdef MUJMAIL_SSL
        if (usemujMailSSL) {
            tlsHandler.close();
        } else {
        //#endif
            streamConnection.close();
            outputStream.close();
            inputStream.close();
        //#ifdef MUJMAIL_SSL
        }
        //#endif
    }

    public void open(String url, boolean ssl, byte sslType) throws IOException {
        if ((ssl) && (sslType == nativeSSL)) {
            // TODO: following line leads to compilation error
            try {
                streamConnection = (SocketConnection) Connector.open("ssl://" + url, Connector.READ, true);
            } catch (Exception iOException) {
                iOException.printStackTrace();
                throw (IOException)iOException; // Dekuju za zmenu chovani panove
            }

            //streamConnection = (StreamConnection) Connector.open("ssl://" + url);
            available_bug = true;
        } else if (!ssl || sslType == mujMailSSL) {
           streamConnection = (SocketConnection) Connector.open("socket://" + url, Connector.READ_WRITE, true);
        }
        inputStream = streamConnection.openInputStream(); 
        outputStream = streamConnection.openOutputStream();

        //#ifdef MUJMAIL_SSL
        usemujMailSSL = false;
        if (ssl && sslType == mujMailSSL) {
            usemujMailSSL = true;
            available_bug = true;
            tlsHandler = new TlsProtocolHandler(inputStream, outputStream);
            ServerCertificateVerifier verifier = new ServerCertificateVerifier();
            tlsHandler.connect(verifier);
        }
        //#endif
    }

    public boolean available_bug() {
        return available_bug;
    }

    public void flush() throws IOException {
        if (Properties.debugConnections) {
            //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.printlnPersistent("Debug protocol out flushing");
            //#else
//#             System.out.println("Debug protocol out : ");
            //#endif
        }
        
        //#ifdef MUJMAIL_SSL
        if (usemujMailSSL) {
            tlsHandler.getOutputStream().flush();
        } else {
        //#endif
            outputStream.flush();
        //#ifdef MUJMAIL_SSL
        }
        //#endif
    }
}
