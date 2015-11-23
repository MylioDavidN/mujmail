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

package mujmail.ordering.comparator;

import mujmail.TheBox;
import mujmail.ordering.Comparator;

/**
 * Used for sorting loaded user mailboxes in BoxList.
 * Sorts folder by shown name.
 */
public class MailBoxNameComparator implements Comparator {

    public int compare(Object o1, Object o2) throws IllegalArgumentException {
        if ( (o1 instanceof TheBox) &&
             (o2 instanceof TheBox) ) {
            return ((TheBox)o1).getName().compareTo(((TheBox)(o2)).getName());
        }
        final String THE_BOX_CLASS_NAME = TheBox.class.getName();
        throw new IllegalArgumentException(
            "Expected " + THE_BOX_CLASS_NAME
            + " and " + THE_BOX_CLASS_NAME
            + " got " + o1.getClass().getName()
            + " and " + o2.getClass().getName()
        );
    }

}
