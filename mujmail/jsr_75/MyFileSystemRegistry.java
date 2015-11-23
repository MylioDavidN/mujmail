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

import java.util.Enumeration;

/**
 * Interface for file connection. When JSR 75 is available, the 
 * javax.microedition.io.file.FileSystemRegistry object will be called in methods
 * implementing this interface, if not, dummy methods will be called.
 * @author David Hauzar
 */
public interface MyFileSystemRegistry {
    /**
     * Gets enumeration of roots of the file system.
     * @return the enumeration of roots of the file system
     */
    Enumeration listRoots();
}
