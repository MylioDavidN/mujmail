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

import mujmail.util.SaveableBooleanValue;
import mujmail.util.PersistentValueReminder;
import java.util.Vector;
import mujmail.BodyPart;
import mujmail.ContentStorage;
import mujmail.util.Functions;
import mujmail.MessageHeader;
import mujmail.MyException;
import mujmail.search.MessageSearchResult.Occurrence;

/**
 * Enumeration class which represents the message parts that can be used in search.
 * @author David Hauzar
 */
public abstract class SearchMessagePart implements SaveableBooleanValue {
    
    /** Vector of all instances of search message parts. */
    private static final Vector searchMessageParts = new Vector();
    
    private final PersistentValueReminder.PersistentBooleanValueReminder wasSelectedReminder;
    
    /**
     * Creates the instance of search message part.
     */
    private SearchMessagePart() {
        searchMessageParts.addElement(this);
        wasSelectedReminder = new PersistentValueReminder.PersistentBooleanValueReminder(PersistentValueReminder.DB_PREFIX + toString());
    }

    abstract public String toString();
    
    
    
    /**
     * Gets the vector of all possible search message parts.
     * @return all possible search message parts
     */
    public static Vector getAllMessageParts() {
        return Functions.copyVector(searchMessageParts);
    }
    
    public boolean loadBoolean() {
        return wasSelectedReminder.loadBoolean();
    }
    
    public void saveBoolean(boolean isSelected) {
        wasSelectedReminder.saveBoolean(isSelected);
    }
    
    
    /**
     * Finds first occurrence of the phrase in this part of given message and returns
     * this occurrence.
     * 
     * @param message the message in that will be searched.
     * @param phrase the phrase that will be searched
     * @return the first occurrence of the phrase in this part of the message.
     *  MessageSearchResult.Occurrence.NO_OCCURRENCE if the phrase is not in this
     *  the part of the message
     */
    abstract Occurrence findFirstMatch(MessageHeader message, SearchPhrase phrase);
    
    /**
     * Represents the message part "To".
     */
    private static class To extends SearchMessagePart {
        public String toString() {
            return "To";
        }

        MessageSearchResult.Occurrence findFirstMatch(MessageHeader message, SearchPhrase phrase) {
            return SearchCore.getFulltextSearcher().
                searchInString(message.getRecipients(), phrase);
        }
    }
    /**
     * Represents the message part "From".
     */
    private static class From extends SearchMessagePart {
        public String toString() {
            return "From";
        }

        Occurrence findFirstMatch(MessageHeader message, SearchPhrase phrase) {
           return SearchCore.getFulltextSearcher().
                searchInString(message.getSender(), phrase);
        }
    }
    /**
     * Represents the message part "Subject".
     */
    private static class Subject extends SearchMessagePart {
        public String toString() {
            return "Subject";
        }

        Occurrence findFirstMatch(MessageHeader message, SearchPhrase phrase) {
            return SearchCore.getFulltextSearcher().
                searchInString(message.getSubject(), phrase);
        }
    }
    /**
     * Represents the message part "BODY".
     */
    private static class Body extends SearchMessagePart {
        public String toString() {
            return "Body";
        }
        
        /**
         * Return true if given body part should be searched.
         * @param bp the body part about which is decided whether should be searched
         * @return true if body part bp should be searched
         */
        private boolean searchInBodyPart(BodyPart bp) {
            // search in html od text bodyparts stored in RMS storage
            byte contentType = bp.getHeader().getBodyPartContentType();
            if (contentType == BodyPart.TYPE_HTML || contentType == BodyPart.TYPE_TEXT && 
                bp.getStorage().getStorageType() == ContentStorage.StorageTypes.RMS_STORAGE) {
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * Searches the occurrence of the phrase in given body part.
         * @param bp the bodypart in that search
         * @param phrase the phrase that is searched
         * @return the occurrence of the phrase in the body part bp
         * @throws mujmail.MyException
         */
        private Occurrence findInBodyPart(BodyPart bp, SearchPhrase phrase) throws Throwable {
            if (searchInBodyPart(bp)) {
                String content = bp.getStorage().getContent();
                return SearchCore.getFulltextSearcher().searchInString(content, phrase);
            } else {
                // it should be not searched in this bodypart
                return Occurrence.NO_OCCURRENCE;
            }
        }

        Occurrence findFirstMatch(MessageHeader message, SearchPhrase phrase) {
            // searches the occurrence of the phrase in all bodyparts
            int numBodyParts = message.getBodyPartCount();
            for (int i = 0; i < numBodyParts; i++) {
                
                Occurrence occurrence;
                try {
                    occurrence = findInBodyPart(message.getBodyPart((byte) i), phrase);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    continue;
                }
                // the search phrase was found in this body part, return this occurrence
                if (occurrence != Occurrence.NO_OCCURRENCE) return occurrence;
            }
            
            // nothing was found
            return Occurrence.NO_OCCURRENCE;
        }
    }
    
    public static SearchMessagePart TO = new To();
    public static SearchMessagePart FROM = new From();
    public static SearchMessagePart SUBJECT = new Subject();;
    public static SearchMessagePart BODY = new Body();
}
