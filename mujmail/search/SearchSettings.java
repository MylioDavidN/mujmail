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


package mujmail.search;

import java.util.Enumeration;
import java.util.Vector;
import mujmail.PersistentBox;
import mujmail.TheBox;

/**
 * Contains search settings necessary for search.
 * Used to store and get such settings.
 * 
 * All messages that match this search settings must be in specified DateInterval.
 * If the date interval is not specified (by default, or set DateInterval.NOT_SPECIFIED), 
 * all messages are in such interval.
 * All messages that match must also contain all search phrases. If this
 * search settings contains no search phrases (getSearchPhrases returns empty
 * enumeration), all messages match such search phrases.
 * Messages that match must also be from one of specified boxes.
 * 
 * 
 * @author David Hauzar
 */
public class SearchSettings {
    /** The vector of boxes where search. Contains objects of type TheBox */
    private Vector boxes = new Vector();
    
    /** The search phrases and information in which message parts search. 
     * Contains objects of type SearchPhrase */
    private final Vector searchPhrases = new Vector();
    
    /** Date interval in that the message must be. */
    private DateInterval dateInterval = DateInterval.NOT_SPECIFIED;
    
    /**
     * Creates the instance of class SearchSettings.
     */
    public SearchSettings() {}
    
    /**
     * Adds the box that should be searched.
     * @param box the box which should be searched.
     */
    public void addBox(TheBox box) {
        boxes.addElement(box);
    }
    
    /**
     * Adds the vector of boxes that should be searched.
     * @param boxes the vector of boxes that should  be searched
     *  the vector of instances of the class TheBox
     */
    public void addBoxes(Vector boxes) {
        for (int i = 0; i < boxes.size(); i++) {
            addBox((TheBox) boxes.elementAt(i));
        }
    }
    
    /**
     * Add all searchable boxes to the settings.
     */
    public void addAllSearchableBoxes() {
        boxes = PersistentBox.getPersistentBoxes();
    }
    
    /**
     * Removes all boxes that should be searched.
     */
    public void removeBoxes() {
        boxes.removeAllElements();
    }
    
    /**
     * Gets the enumeration of all boxes that should be searched.
     * @return the enumeration of all boxes that should be searched
     */
    public Enumeration getBoxes() {
        return boxes.elements();
    }
    
    /**
     * Adds phrase to be searched.
     * There must be found some occurrence of all search phrases in searched
     * message to not return MessageSearchResult.NO_MATCH.
     * @param searchPhrase the phrase that will be searched
     */
    public void addSearchPhrase(SearchPhrase searchPhrase) {
        searchPhrases.addElement(searchPhrase);
    }
    
    /**
     * Gets the enumeration of all search phrases.
     * @return the enumeration of all search phrases.
     */
    public Enumeration getSearchPhrases() {
        return searchPhrases.elements();
    }
    
    /**
     * Removes all search phrases.
     */
    public void removeSearchPhrases() {
        searchPhrases.removeAllElements();
    }
    
    /**
     * Sets the date interval of messages that should be searched.
     * @param dateInterval
     */
    public void setDateInterval(DateInterval dateInterval) {
        this.dateInterval = dateInterval;
    }
    
    /**
     * Gets the date interval of messages that should be searched.
     * @return the date interval of messages that should be searched.
     */
    public DateInterval getDateInterval() {
        return dateInterval;
    }
}
