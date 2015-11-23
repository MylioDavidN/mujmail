/*
MujMail - Simple mail client for J2ME
Copyright (C) 2009 David Hauzar <david.hauzar.mujmail@gmail.com>

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

package mujmail.tasks;

/**
 *
 * Represents events that sends BackgroundTask and it's descendatnts to
 * Observers registered to it.
 * 
 * @author David Hauzar
 */
public class TaskEvents {
    private TaskEvents() {};

    /** The progress was updated. This means that method {@link BackgroundTask#updateProgress} was called. */
    public static final TaskEvents UPDATE_PROGRESS = new TaskEvents();
    /** Actual progress was incremented. */
    public static final TaskEvents INC_ACTUAL = new TaskEvents();
    /** New title was setted. */
    public static final TaskEvents SET_TITLE = new TaskEvents();

}
