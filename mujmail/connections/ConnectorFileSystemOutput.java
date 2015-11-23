//#condition MUJMAIL_FS
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

package mujmail.connections;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.ConnectionNotFoundException;
import mujmail.jsr_75.FilesystemFactory;
import mujmail.jsr_75.MyFileConnection;

import mujmail.util.OutputBuffer;

/**
 * Implements Basic connection using filesystem - the data are being sent
 * to the filesystem.
 * Implements only output functionality of BasicConnection
 * 
 * @author David Hauzar
 */
public class ConnectorFileSystemOutput implements ConnectorInterface {
    
    private InputStream inputStream = null;
    private OutputBuffer.OutputStreamBuffer outputStream = null;
    private MyFileConnection connection;
    
    /** 
     * Gets well typed connector used for writing data into filesystem.
     * @return FileConnector used for writeing data.
     */
    public MyFileConnection getFileConnection() {
        return connection;
    }

    /**
     * Creates connector that saves data into filesystem of mobile.
     * @param connection Filesystem connection used for writes.
     */
    public ConnectorFileSystemOutput(MyFileConnection connection) throws IOException {
        this.connection = connection;
        
        outputStream = connection.openOutputBufferedStream();
        inputStream = connection.openInputStream();
    }

    /**
     * Not supported. Only writing features available. 
     * @return one
     */
    public int available() throws IOException {
        return 1;
    }

    public void close() throws IOException {
        try {
            outputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: display alert
        }
        connection.close();
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        return inputStream.read(buffer, offset, length);
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        try {
            if (offset != 0 || length != data.length) {
                int len = length; // Copy buffer
                if ((data.length - offset) < len) len = data.length - offset;
                byte[] tmp = new byte[len];
                for( int i = offset; i < offset + len; i++) tmp[i] = data[i];
                data = tmp;
            }
            outputStream.write(data);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    public void open(String url, boolean ssl, byte sslType) throws IOException {
        try {
            connection = FilesystemFactory.getFileConnection(url, Connector.READ_WRITE);
        } catch (Exception ex) {
            throw new ConnectionNotFoundException( ex.getMessage());
        }
        outputStream = connection.openOutputBufferedStream();
        inputStream = connection.openInputStream();
    }
    
    public boolean available_bug() {
        return false;
    }

    public void flush() throws IOException {
        try {
            outputStream.flush();
        } catch (Exception e) {
            IOException ex = new IOException( e.getMessage());
            e.printStackTrace();
            throw ex;
        }
    }
}