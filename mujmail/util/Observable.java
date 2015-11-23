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

package mujmail.util;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Analogous to java.util.Observable from JSE. Javadoc comments are copyed from
 * original implementation in JSE.
 * 
 * This class represents an observable object, or "data" in the model-view
 * paradigm. It can be subclassed to represent an object that the application
 * wants to have observed.
 *
 * An observable object can have one or more observers. An observer may be any
 * object that implements interface Observer. After an observable instance
 * changes, an application calling the Observable's notifyObservers method
 * causes all of its observers to be notified of the change by a call to their
 * update method.
 *
 * The order in which notifications will be delivered is unspecified. The
 * default implementation provided in the Observable class will notify Observers
 * in the order in which they registered interest, but subclasses may change
 * this order, use no guaranteed order, deliver notifications on separate
 * threads, or may guarantee that their subclass follows this order, as they
 * choose.
 *
 * Note that this notification mechanism is has nothing to do with threads and
 * is completely separate from the wait and notify mechanism of class Object.
 *
 * When an observable object is newly created, its set of observers is empty.
 * Two observers are considered the same if and only if the equals method
 * returns true for them.
 *
 * 
 * 
 * @author David Hauzar
 */
public class Observable {
    private final Hashtable observers = new Hashtable(1);
    private boolean hasChanged = false;

    public String toString() {
        return "Number of observers: " + observers.size();
    }



    /**
     * Adds an observer to the set of observers for this object, provided that
     * it is not the same as some observer already in the set. The order in
     * which notifications will be delivered to multiple observers is not
     * specified. See the class comment.
     *
     * @param o an observer to be added.
     * @throws NullPointerException - if the parameter o is null.
     */
    public void addObserver(Observer o) {
        observers.put(o, o);
        
    }

    /**
     * Deletes an observer from the set of observers of this object. Passing
     * null to this method will have no effect. 
     * @param o the observer to be deleted.
     */
    public void deleteObserver(Observer o) {
        observers.remove(o);

    }

    /**
     * If this object has changed, as indicated by the hasChanged method, then
     * notify all of its observers and then call the clearChanged method to
     * indicate that this object has no longer changed.
     *
     * Each observer has its update method called with two arguments: this
     * observable object and null. In other words, this method is equivalent to:
     * <code>notifyObservers(null)</code>
     *
     * @see #clearChanged()
     * @see #hasChanged()
     * @see Observer#update(Observable, Object)
     */
    public void notifyObservers() {
        notifyObservers(null);

    }

    /**
     * If this object has changed, as indicated by the hasChanged method, then 
     * notify all of its observers and then call the clearChanged method to
     * indicate that this object has no longer changed.
     *
     *  Each observer has its <code>update</code> method called with two
     * arguments: this observable object and the <code>arg</code> argument.
     * @param arg any object.
     */
    public synchronized void notifyObservers(Object arg) {
        if (!hasChanged()) {
            return;
        }
        clearChanged();

        Enumeration observersE = observers.elements();
        while (observersE.hasMoreElements()) {
            Observer observer = (Observer) observersE.nextElement();
            observer.update(this, arg);
        }

    }

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    public void deleteObservers() {
        observers.clear();

    }

    /**
     * Marks this <code>Observable</code> object as having been changed; the
     * <code>hasChanged</code> method will now return <code>true</code>.
     */
    protected void setChanged() {
        hasChanged = true;
    }

    /**
     * Indicates that this object has no longer changed, or that it has already
     * notified all of its observers of its most recent change, so that the
     * hasChanged method will now return false. This method is called
     * automatically by the notifyObservers methods.
     *
     * @see #notifyObservers()
     * @see #notifyObservers(java.lang.Object) 
     */
    protected void clearChanged() {
        hasChanged = false;
    }

    /**
     * Tests if this object has changed.
     *
     * @return true if and only if the setChanged method has been called
     * more recently than the clearChanged method on this object; false
     * otherwise.
     *
     * @see #clearChanged()
     * @see #setChanged() 
     */
    public boolean hasChanged() {
        return hasChanged;
    }

    /**
     * Returns the number of observers of this Observable object.
     *
     * @return the number of observers of this object.
     */
    public int countObservers() {
        return observers.size();
    }
}
