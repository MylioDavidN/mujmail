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

/**
 * Represents search algorithm that do search of substring in string.
 *
 * The instance of object implementing this interface is used in {@link mujmail.search.FulltextSearcher}
 * that instance returned by method {@link mujmail.search.SearchCore#getFulltextSearcher}
 * is used in all fulltext searching.
 * search.
 *
 * To implement new algorithm for fulltext searching, implement this interface.
 * To make new search algorithm used while searching, create instance of class
 * {@link mujmail.search.FulltextSearcher} with new search algorithm as the
 * parameter and make method {@link mujmail.search.SearchCore#getFulltextSearcher}
 * to return this instance.
 * 
 * @author David Hauzar
 */
interface FulltextSearchAlgorithm {    
    /**
     * Finds first occurrence of the searchPhrase in the string searchedString.
     *
     * Classes implementing this interface can implement searching with various
     * wildcards, etc.
     * 
     * @param searchedString the stirng that will be searched 
     * @param searchPhrase the phrase to be searched.
     * @return the first location of the searchPhrase in the searchedString.
     *  SubstringLocation.NO_LOCATION if the searchPhrase is not in the
     *  searchedString
     */
    SubstringLocation findInStringFirstMatch(String searchedString, String searchPhrase);
    
    /**
     * Represents location of some substring in some string.
     */
    static class SubstringLocation {
        /** If the substring is not located in given string. */
        public static final SubstringLocation NO_LOCATION = new SubstringLocation(-1, -1);
        
        /** The first letter of the substring in string. */
        final int firstLetter;
        /** The last letter of the substring in string. */
        final int lastLetter;
        
        SubstringLocation(int firstLetter, int lastLetter) {
            this.firstLetter = firstLetter;
            this.lastLetter = lastLetter;
        }
    }
    
    /**
     * Implementation of FulltextSearchAlgorithm that uses standard Java
     * String.indexOf method.
     * Does not use any wildcards.
     */
    class DefaultFulltextSearchAlgorithm implements FulltextSearchAlgorithm {
        public SubstringLocation findInStringFirstMatch(String searchedString, String searchPhrase) {
            // ignore case
            searchedString = searchedString.toLowerCase();
            searchPhrase = searchPhrase.toLowerCase();
            
            // find the match
            int startLetter = searchedString.indexOf(searchPhrase);
            if (startLetter == -1) {
                return SubstringLocation.NO_LOCATION;
            }
            int lastLetter = startLetter + searchPhrase.length() - 1;

            // return the occurrence
            return new SubstringLocation(startLetter, lastLetter);
        }
        
    }
}
