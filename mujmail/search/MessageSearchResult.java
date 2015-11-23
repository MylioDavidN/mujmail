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

import java.util.Vector;

/**
 * Represent the result of searching in one message. That means occurences
 * of specified search phrases.
 *
 * Each message - instance of class {@link mujmail.MessageHeader} has instance
 * of this class inside.
 * 
 * @author David Hauzar
 */
public class MessageSearchResult {
    /** The vector of occurences of searched phrases in the message. */
    private final Vector occurrences;
    
    /** When nothing was found in the message. */
    public static final MessageSearchResult NO_MATCH = new MessageSearchResult();

    /**
     * Creates the instace of this class containing no occurences.
     */
    public MessageSearchResult() {
        occurrences = new Vector();
    }
    
    /**
     * Creates the instance of this class with containing the same occurences
     * as given messageSearchResult.
     * 
     * @param messageSearchResult message search result that occurences will be copyed
     *  note that vector of occurences will be directly used and not copyed.
     */
    public MessageSearchResult(MessageSearchResult messageSearchResult) {
        this.occurrences = messageSearchResult.occurrences;
    }

    /**
     * Creates the instance of this class with containing given occurences.
     *
     * @param occurrences occurences that this message search results contains.
     *  note that this vector will be directly used and not copyed.
     */
    MessageSearchResult(Vector occurrences) {
        this.occurrences = occurrences;
    }

    /**
     * Gets the occurences of search phrases that were found in message that
     * search results this instace stores.
     * @return the occurences of search phrases that were found in message that
     * search results this instace stores.
     */
    Vector getOccurrences() {
        return occurrences;
    }

    /**
     * Represents one occurrence of search phrase in message.
     */
     static class Occurrence {
        /** The number of letters in neigbourhood before the search phrase. */
        private static final int NEIGHBOURHOOD_BEFORE_PHRASE = 10;
        /** The number of letters in neigbourhood after the search phrase. */
        private static final int NEIGHBOURHOOD_AFTER_PHRASE = 10;
        
        /** Represents no occurrence of the phrase in the message. */
        public static final Occurrence NO_OCCURRENCE = new Occurrence();
        
        /** The phrase that matches this occurrence. */
        private final SearchPhrase searchPhrase;
     
        /** Contains the phrase plus the neighbourhood of the phrase. */
        private final String neighbourhood;
        /** The starting letter of the phrase in string neighbourhood */
        private int firstLetterInNeighbourhood;
        /** The last letter of the phrase in string neighbourhood */
        private int lastLetterInNeighbourhood;
        
        
        /**
         * Extract the neighbourhood of the phrase from given text.
         * 
         * @param text the text that contains the phrase.
         * @param startPhrase the number of letter in text where the phrase starts
         * @param endPhrase the number of letter in text where the phrase ends
         * @return the neighbourhood of the phrase. This means the substring of text
         *  that contains the phrase plus given number of letters of the text before 
         *  the phrase and after the phrase.
         */
        private String extractNeighbourhood(String text, int startPhrase, int endPhrase) {
            firstLetterInNeighbourhood = startPhrase - NEIGHBOURHOOD_BEFORE_PHRASE;
            firstLetterInNeighbourhood = (firstLetterInNeighbourhood >= 0) ? firstLetterInNeighbourhood : 0;

            lastLetterInNeighbourhood = endPhrase + NEIGHBOURHOOD_AFTER_PHRASE;
            lastLetterInNeighbourhood = (lastLetterInNeighbourhood < text.length()) ? lastLetterInNeighbourhood : text.length()-1;

            return text.substring(firstLetterInNeighbourhood, lastLetterInNeighbourhood);
        }
        
        /** Used only to construct instance NO_MATCH. */
        private Occurrence() {
            this.searchPhrase = null;
            this.neighbourhood = "";
            firstLetterInNeighbourhood = 0;
            lastLetterInNeighbourhood = -1;
        }

        /**
         * Creates new instance of occurrence.
         * @param searchPhrase the phrase that was found in searchedText
         * @param searchedText the text that was searched and that containins 
         *  search phrase
         * @param firstLetter the letter in searchedText where the phrase begins
         * @param lastLetter the last letter of the phrase in searchedText
         */
         Occurrence(SearchPhrase searchPhrase, String searchedText, int firstLetter, int lastLetter) {
            this.searchPhrase = searchPhrase;
            this.neighbourhood = extractNeighbourhood(searchedText, firstLetter, lastLetter);
            extractNeighbourhood(searchedText, firstLetter, lastLetter);
        }
        
        
        
        
    }
}
