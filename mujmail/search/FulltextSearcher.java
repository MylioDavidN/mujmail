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
 * Used to do fulltext search. Provides additional functionality above
 * FulltextSearchAlgorithm such as checking whether found substring matches
 * search mode defined in given search phrase.
 *
 * @author David Hauzar
 */
class FulltextSearcher {
    private final FulltextSearchAlgorithm searchAlgorithm;

    /**
     * Creates new instance of this class.
     * @param searchAlgorithm search algorithm used for searching.
     */
    public FulltextSearcher(FulltextSearchAlgorithm searchAlgorithm) {
        this.searchAlgorithm = searchAlgorithm;
    }

    /**
     * Searches in given string the occurence of given searchPhrase.
     *
     * Uses instance of class {@link mujmail.search.FulltextSearchAlgorithm} for searching
     * the string and {@link mujmail.search.FulltextSearchModes} stored in
     * {@link mujmail.search.SearchPhrase} to check whether the location of the
     * string matches the {@link mujmail.search.FulltextSearchModes}.
     * 
     * @param searchedString the string to be searched in.
     * @param searchPhrase the phrase to be searched in <code>searchedSring</code>
     *  contains the string to be searched and search mode used for searching
     * @return found occurence in searchedString of searchPhrase
     */
    public MessageSearchResult.Occurrence searchInString(String searchedString, 
        SearchPhrase searchPhrase) {
        FulltextSearchModes searchMode = searchPhrase.getSearchMode();
        while (true) {
            SubstringLocation location = 
                searchAlgorithm.findInStringFirstMatch(searchedString, searchPhrase.getPhrase());
            if (location == SubstringLocation.NO_LOCATION) {
                return MessageSearchResult.Occurrence.NO_OCCURRENCE;
            }
            
            if ( searchMode.matchMode(location, searchedString, searchPhrase.getPhrase()) ) {
                return new MessageSearchResult.Occurrence(searchPhrase, searchedString, location.firstLetter, location.lastLetter);
            }
            
            searchedString = searchedString.substring(location.firstLetter + 1);
        }
    }

}
