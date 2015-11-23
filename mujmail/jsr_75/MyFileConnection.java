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


package mujmail.jsr_75;

import java.io.IOException;
import java.util.Enumeration;
import javax.microedition.io.StreamConnection;
import mujmail.util.OutputBuffer;
import mujmail.util.OutputBuffer.OutputStreamBuffer;

/**
 * Interface for file connection. When JSR 75 is available, the 
 * javax.microedition.io.file.FileConnection object will be called in methods
 * implementing this interface, if not, dummy methods will be called.
 * 
 * @author David Hauzar
 */
public interface MyFileConnection extends StreamConnection {
    /**
     * Initializes the file connection.
     *
     * @param fileURL
     * @param flag Connector.READ, Connector.WRITE, Connector.READ_WRITE
     */
    void initialize(String fileURL, int flag) throws IOException;
    
    void initialize(String fileURL) throws IOException;
    
    /**
     * Determines the size of a file on the file system. The size of a file 
     * always represents the number of bytes contained in the file; there is no 
     * pre-allocated but empty space in a file. Users should perform an explicit 
     * flush()  on any open output streams to the file prior to invoking this 
     * method to ensure accurate results.
     * 
     * @return The size in bytes of the selected file, or -1 if the file does 
     *  not exist or is not accessible.
     *  -1 if the this is a dummy connection.
     * 
     * @throws java.io.IOException - if the method is invoked on a directory.
     * @throws java.lang.SecurityException - if the security of the application 
     *  does not have read access for the file.
     * @throws IllegalModeException  - if the application does have read access 
     *  to the file but has opened the connection in Connector.WRITE mode.
     * @throws ConnectionClosedException  - if the connection is closed.
     */
    long fileSize() throws IOException;
    
    /**
     * Gets a list of all visible files and directories contained in a directory. 
     * The directory is the connection's target as specified in Connector.open().
     * @return An Enumeration of strings, denoting the files and directories 
     * in the directory. The string returned contain only the file or directory 
     * name and does not contain any path prefix (to get a complete path for each 
     * file or directory, prepend  getPath()). Directories are denoted with 
     * a trailing slash "/" in their returned name. The Enumeration has zero 
     * length if the directory is empty. Any hidden files and directories in the 
     * directory are not included in the returned list. Any current directory 
     * indication (".") and any parent directory indication ("..") is not included 
     * in the list of files and directories returned.
     * 
     * @throws java.io.IOException - if invoked on a file, the directory does 
     *  not exist, the directory is not accessible, or an I/O error occurs.
     * @throws ConnectionClosedException  - if the connection is closed.
     * @throws java.lang.SecurityException - if the security of the application 
     *  does not have read access for the directory.
     * @throws IllegalModeException  - if the application does have read access 
     *  to the directory but has opened the connection in Connector.WRITE mode.
     */
    Enumeration list() throws IOException;
    

    /**
     * Opens buffered output stream. See documentation of openOutputStream and
     * OutputBuffer.OutputStreamBuffer.
     * @see #openOutputStream
     * @see mujmail.util.OutputBuffer.OutputStreamBuffer
     * @return buffered output stream.
     * @throws java.io.IOException
     */
    OutputBuffer.OutputStreamBuffer openOutputBufferedStream() throws IOException;
    
    /**
     * Checks if the file or directory specified in the URL passed to the 
     * Connector.open() method exists.
     * 
     * @return true if the connnection's target exists and is accessible, 
     *  otherwise false.;
     *  false if it is dummy connection
     * @throws java.lang.SecurityException if the security of the application 
     *  does not have read access for the connection's target.
     * @throws IllegalModeException if the application does have read access to 
     *  the connection's target but has opened the connection in Connector.WRITE mode.
     * @throws ConnectionClosedException if the connection is closed.
     */
    public boolean exist();
    
    /**
     * Creates a file corresponding to the file string provided in the 
     * Connector.open() method for this FileConnection. The file is created 
     * immediately on the actual file system upon invocation of this method. 
     * Files are created with zero length and data can be put into the file 
     * through output streams opened on the file. This method does not create 
     * any directories specified in the file's path.
     * 
     * @throws java.lang.SecurityException - if the security of the application 
     *  does not have write access for the file.
     * @throws IllegalModeException  - if the application does have write access 
     *  to the file but has opened the connection in Connector.READ mode.
     * @throws java.io.IOException - if invoked on an existing file or on any 
     * directory (mkdir() is used to create directories), the connection's target 
     * has a trailing "/" to denote a directory, the target file system is not 
     * accessible, or an unspecified error occurs preventing creation of the file.
     * @throws ConnectionClosedException  - if the connection is closed.
     */
    public void create() throws java.io.IOException;
    
    /**
     * Creates a directory corresponding to the directory string provided in the 
     * Connector.open() method. The directory is created immediately on the 
     * actual file system upon invocation of this method. Directories in the 
     * specified path are not recursively created and must be explicitly created 
     * before subdirectories can be created.
     * 
     * @throws java.lang.SecurityException if the security of the application 
     *  does not have write access to the directory.
     * @throws IllegalModeException if the application does have write access to 
     *  the directory but has opened the connection in Connector.READ mode.
     * @throws java.io.IOException if invoked on an existing directory or on any 
     *  file (create() is used to create files), the target file sytem is not 
     *  accessible, or an unspecified error occurs preventing creation of the 
     *  directory.
     * @throws ConnectionClosedException if the connection is closed.
     */
    public void mkdir() throws java.io.IOException;
    
    /**
     * Class with default implementations of methods from interface 
     * MyFileConnectionAdapter.
     */
    public static abstract class MyFileConnectionAdapter implements MyFileConnection {

        public OutputStreamBuffer openOutputBufferedStream() throws IOException {
            return new OutputStreamBuffer(openOutputStream());
        }
        
    }
}
