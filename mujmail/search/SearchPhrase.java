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
import mujmail.util.Functions;
import mujmail.MessageHeader;

/**
 * Represents the search phrase - that is the string that is searched and the 
 * part of the message in that the string will be searched.
 * @author David Hauzar
 */
public class SearchPhrase {
    private final String phrase;
    private final Vector messageParts;
    private final FulltextSearchModes searchMode;
    private final PhraseImportance importance;

    /**
     * Creates the instance of search phrase.
     * @param phrase the string that is searched
     * @param searchMode the fulltext search mode used when searching this phrase
     * @param importance the importance of the phrase in searching
     * @param messageParts the vector with parts of the message in that the 
     *  phrase will be searched.
     *  Vector of objects of type SearchMessagePart.
     * searched
     */
    public SearchPhrase(String phrase, FulltextSearchModes searchMode, PhraseImportance importance, Vector messageParts) {
        this.phrase = phrase;
        this.messageParts = Functions.copyVector(messageParts);
        this.searchMode = searchMode;
        this.importance = importance;
    }
    
    /**
     * Creates the instance of search phrase and sets default set of message
     * parts that will be searched.
     * @param phrase the string that is searched
     * @param searchMode the fulltext search mode used when searching this phrase
     * @param importance the importance of the phrase in searching
     * searched
     */
    public SearchPhrase(String phrase, FulltextSearchModes searchMode, PhraseImportance importance) {
        this.phrase = phrase;
        this.searchMode = searchMode;
        this.importance = importance;
        messageParts = new Vector();
        messageParts.addElement(SearchMessagePart.SUBJECT);
        //messageParts.addElement(SearchMessagePart.BODY);
        //messageParts.addElement(SearchMessagePart.FROM);
        //messageParts.addElement(SearchMessagePart.TO);
    }
    
    /**
     * Gets the string that is searched.
     * @return the string that is searched.
     */
    String getPhrase() {
        return phrase;
    }
    
    /**
     * Gets fulltext search mode.
     * @return the fulltext search mode.
     */
    FulltextSearchModes getSearchMode() {
        return searchMode;
    }
    
    /**
     * Gets the importance of this phrase.
     * @return the importance of this phrase.
     */
    PhraseImportance getImportance() {
        return importance;
    }
    
    /**
     * Gets the enumeration of parts of the message in that the phrase is searched.
     * @return the enumeration of parts of the message in that the phrase is 
     *  searched. Enumeration of objects of type SearchMessagePart
     */
    Enumeration getMessageParts() {
        return messageParts.elements();
    }
    
    /**
     * Finds first occurrence of this search phrase in given message and returns
     * this occurrence.
     * 
     * @param message the message in that will be searched.
     * @return the first occurrence of this search phrase in the message
     *  MessageSearchResult.Occurrence.NO_OCCURRENCE if this phrase is not in the
     *  message
     */
     MessageSearchResult.Occurrence findFirstMatch(MessageHeader message) {
         for (int i = 0; i < messageParts.size(); i++) {
             SearchMessagePart messagePart = (SearchMessagePart)messageParts.elementAt(i);
             System.out.println("Scanning message part");
             MessageSearchResult.Occurrence occurence = messagePart.findFirstMatch(message, this);
             if (occurence != MessageSearchResult.Occurrence.NO_OCCURRENCE) {
                 System.out.println("OCCURRENCE FOUND");
                 return occurence;
             }
         }
         
         // no occurence was found
         return MessageSearchResult.Occurrence.NO_OCCURRENCE;
    }
     
     /**
      * Represents the importance of the phrase while searching.
      */
     public static class PhraseImportance {
         private PhraseImportance() {};
         
         /** The phrase is required. The phrase must be found in matching message. */
         public static final PhraseImportance REQUIRED = new PhraseImportance();
         /** The phrase is optional. At least one (optional or required) phrase must be found in matching message. */
         public static final PhraseImportance OPTIONAL = new PhraseImportance();
     }

}
