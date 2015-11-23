//#condition MUJMAIL_SEARCH
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

package mujmail.util;

/**
 * This interface implements classes that supports saving and loading of some
 * boolean value to RMS database.
 * 
 * @author David Hauzar
 */
public interface SaveableBooleanValue {
    
    /**
     * Persistently saves given boolean value of this item so it can be
     * reminded with method {@link mujmail.util.SaveableBooleanValue#loadBoolean() }
     * later.
     * 
     * @param value the value of this item.
     */
    public void saveBoolean(boolean value);
    
    /**
     * Reminds the value of saved boolean variable..
     * @return true if saved value of boolean variable was true.
     */
    public boolean loadBoolean();

}
