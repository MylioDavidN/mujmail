//#condition MUJMAIL_COMPRESSED_CONNECTION
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

package mujmail.connections;

//#ifdef MUJMAIL_DEBUG_CONSOLE
import mujmail.debug.DebugConsole;
//#endif
import java.io.IOException;
import mujmail.connections.gzip.GZipInputStream;
import mujmail.Properties;
import mujmail.connections.gzip.PreBufferInputStream;

/** 
 * Special connector which decompress incomming communication. 
 * <p>
 * It work as filter. Is suppose that communication is compressed with GZip 
 *  (deflate) compression.
 */

public class ConnectorGZip implements ConnectorInterface {
    private GZipInputStream is = null; /// Decompressing stream

    private ConnectorInterface communicator; /// Connector where resieve compressed data
    
    private static StringBuffer tmp_buf = null; /// Buffer for debugging output

    /**
     * Creates new docompressing connector.
     * <p>
     * Outgouing data are passed directly into undelying connector ci.
     * Incomming data from connector ci are decompressed 
     * before reading by {@link #read} method.
     *
     * @param ci Underlying connector used for sending and retrieving data by this filer.
     * @param ioBuff Buffer with some portion of data. Data from this buffer are decompressed before any reding from underlying connetor ci occure. 
     */
    public ConnectorGZip( ConnectorInterface ci, InOutBuffer ioBuff ) throws IOException {
        if ( Properties.debugConnections )
            tmp_buf = new StringBuffer(512);

        communicator = ci;
        if ( ci != null ) {
            is = new GZipInputStream( new PreBufferInputStream( ci, ioBuff), GZipInputStream.TYPE_GZIP, false);
        }
    }
    
    public void write(byte[] data, int off, int len) throws IOException {
        communicator.write(data, off, len);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (Properties.debugConnections) {
                //#ifdef MUJMAIL_DEBUG_CONSOLE
            DebugConsole.printPersistent("Debug protocol in GZIP: ");
                //#else
//#             System.out.print("Debug protocol in : ");
                //#endif
                }

        int result = is.read(buffer, offset, length);

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

    public int available() throws IOException {
        return is.available();
    }

    public boolean available_bug() {
        return false;
    }

    public void close() throws IOException {
        is.close();
        communicator.close();
    }

    public void open(String url, boolean ssl, byte sslType) throws IOException {
        communicator.open( url, ssl, sslType);
        is = new GZipInputStream( new PreBufferInputStream(communicator, null), GZipInputStream.TYPE_GZIP, false);
    }

    public void flush() throws IOException {
        communicator.flush();
    }

}
