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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * Wrapper for object of class javax.microedition.io.file.FileConnection.
 * Used when JSR 75 is available.
 * @author David Hauzar
 */
class MyFileConnectionJSR extends  MyFileConnection.MyFileConnectionAdapter {
    private FileConnection fileConnection;
    
    MyFileConnectionJSR() {
    }
    
    public void initialize(String fileURL, int flag) throws IOException {
        fileConnection = (FileConnection) Connector.open(fileURL, flag);
    }
    
    
    public void initialize(String fileURL) throws IOException {
        fileConnection = (FileConnection) Connector.open(fileURL);
    }

    public void close() throws IOException {
        fileConnection.close();
    }

    public long fileSize() throws IOException {
        return fileConnection.fileSize();
    }

    public Enumeration list() throws IOException {
        return fileConnection.list();
    }

    public InputStream openInputStream() throws IOException {
        return fileConnection.openInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return fileConnection.openOutputStream();
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return fileConnection.openDataOutputStream();
    }

    public void create() throws IOException {
        fileConnection.create();
    }

    public boolean exist() {
        return fileConnection.exists();
    }

    public void mkdir() throws IOException {
        fileConnection.mkdir();
    }

	public DataInputStream openDataInputStream() throws IOException {
		return fileConnection.openDataInputStream();
	}
}
