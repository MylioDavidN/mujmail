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

import mujmail.search.FulltextSearchAlgorithm.SubstringLocation;

/**
 * Enumeration class that provides search modes for fulltext searching.
 *
 * Search modes are used in method {@link mujmail.search.FulltextSearcher#searchInString(java.lang.String, mujmail.search.SearchPhrase)}
 * to check whether given location of string that was found meets given conditions.
 * For example it is whole word etc.
 * @author David Hauzar
 */
public abstract class FulltextSearchModes {
    private final String name;

    /** Private constructor ensures that it is not possible to create new
     instances outside this class. */
    private FulltextSearchModes(String name) { this.name = name; };
    
    /**
     * Returns true if given location of the searchPhrase in the 
     * searchedString match the definition of this search mode.
     * 
     * @param location the location of the searchedString in the searchPhrase.
     * @param searchedString the string that was searched.
     * @param searchPhrase the string that was found.
     * @return true if given location of the searchPhrase int the
     *  searchedString match the definition of this search mode.
     */
    protected abstract boolean matchMode(SubstringLocation location, 
        String searchedString, String searchPhrase);

    /**
     * Returns true if substring of the searchedString with given location
     * begins a word in searchedString.
     * @param location the location of substring in the searchedString.
     * @param searchedString the string in that is located substring
     * @return true if substring with given location begins a word in 
     *  searchedString
     */
    protected boolean beginsWord(SubstringLocation location, String searchedString) {
        if (location.firstLetter == 0 || (searchedString.charAt(location.firstLetter - 1) == ' ')) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if substring of the searchedString with given location
     * ends a word in searchedString.
     * @param location the location of substring in the searchedString.
     * @param searchedString the string in that is located substring
     * @return true if substring with given location ends a word in 
     *  searchedString
     */
    protected boolean endsWord(SubstringLocation location, String searchedString) {
        if (location.lastLetter == searchedString.length() - 1 || 
            (searchedString.charAt(location.lastLetter + 1) == ' ')) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return name;
    }


    /** Represents whole words search mode. Found occurences must be whole words
     * in searched text.*/
    public static final FulltextSearchModes WHOLE_WORDS = new WholeWords("Whole words");

    /** Represents not whole words search mode. Found occurences need not be
     * whole words in searched text. */
    public static final FulltextSearchModes NOT_WHOLE_WORDS = new NotWholeWords("Not whole words");


    /** Represents whole words search mode. Found occurences must be whole words
     * in searched text.*/
    private static class WholeWords extends FulltextSearchModes {
        public WholeWords(String name) {
            super(name);
        }
        
        
        protected boolean matchMode(SubstringLocation location, String searchedString, String searchPhrase) {
            if (beginsWord(location, searchedString) && endsWord(location, searchedString)) {
                return true;
            } else {
                return false;
            }      
        }

    }

    /** Represents not whole words search mode. Found occurences need not be
     * whole words in searched text. */
    private static class NotWholeWords extends FulltextSearchModes {
        public NotWholeWords(String name) {
            super(name);
        }
        
        protected boolean matchMode(SubstringLocation location, String searchedString, String searchPhrase) {
            return true;
        }

    }

}
