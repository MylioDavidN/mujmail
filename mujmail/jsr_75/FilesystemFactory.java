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

import mujmail.Properties;

/**
 * Static class used to get dynamically loaded instances of filesystem classes.
 * If the JSR75 is be found, the class using wrapping JSR75 objects will be 
 * received, if not, it will be received the class with dummy methods.
 * For explanation of this mechanism, see documetation of package mujmail.jsr_75.
 * 
 * @author David Hauzar
 */
public class FilesystemFactory {
    
    /** Singleton static class. */
    private FilesystemFactory() {};

    /**
     * Gets file connection object. Dynamically loads either the object using
     * JSR 75 or dummy object.
     * @param FileURL the url used for the connection
     * @param flag Connector.READ, Connector.WRITE, Connector.READ_WRITE
     * @return the file connection object
     */
    public static MyFileConnection getFileConnection(String FileURL, int flag) throws Exception {
        MyFileConnection connection = getFConnection();
        
        connection.initialize(FileURL, flag);
        return connection;
    }
    
    /**
     * Gets file connection object. Dynamically loads either the object using
     * JSR 75 or dummy object.
     * @param FileURL the url used for the connection
     * @return the file connection object
     * @throws java.lang.Exception
     */
    public static MyFileConnection getFileConnection(String FileURL) throws Exception {
        MyFileConnection connection = getFConnection();
        connection.initialize(FileURL);
        return connection;
    }
    
    private static MyFileConnection getFConnection() throws Exception {
        // TODO: ?? why does not work this?
        //MyFileConnection connection = (MyFileConnection) getFileSystemObject("mujmail.jsr_75.MyFileConnectionJSR", "mujmail.jsr_75.MyFileConnectionDummy");
        
        Class cl;
        if (Properties.JSR75Available()) {
            cl = Class.forName("mujmail.jsr_75.MyFileConnectionJSR");
        } else {
            cl = Class.forName("mujmail.jsr_75.MyFileConnectionDummy");
        }
        MyFileConnection connection = (MyFileConnection) cl.newInstance();
        return connection;
    }
    
    /**
     * Gets file system registry object. Dynamically loads either the object using
     * JSR 75 or dummy object.
     * @return file system registry object
     * @throws java.lang.Exception
     */
    public static MyFileSystemRegistry getFileSystemRegistry() throws Exception {
        Class cl;
        if (Properties.JSR75Available()) {
            cl = Class.forName("mujmail.jsr_75.MyFileSystemRegistryJSR");
        } else {
            cl = Class.forName("mujmail.jsr_75.MyFileSystemRegistryDummy");
        }
        return (MyFileSystemRegistry) cl.newInstance();
        
        // TODO: ?? why does not work this:
        //return (MyFileSystemRegistry) getFileSystemObject("mujmail.jsr_75, "mujmail.jsr_75.MyFileSystemRegistryDummy");
    }
    
    /**
     * Loads object of dynamically loaded class.
     * TODO: Does not works. 
     * @param JSRClass
     * @param DummyClass
     * @return
     * @throws java.lang.Exception
     */
    private static Object getFileSystemObject(String JSRClass, String DummyClass) throws Exception {
        Class cl;
        if (Properties.JSR75Available()) {
            cl = Class.forName(JSRClass);
        } else {
            cl = Class.forName(DummyClass);
        }
        return cl.newInstance();
    }
}
