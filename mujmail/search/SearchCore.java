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
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.TheBox;
import mujmail.tasks.Progress;
import mujmail.tasks.StoppableProgress;

/**
 * The search engine core.
 * Provides search capability.
 * 
 * @author David Hauzar
 */
public class SearchCore {
    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;
    
    private static final FulltextSearcher searcher = new FulltextSearcher( 
        new FulltextSearchAlgorithm.DefaultFulltextSearchAlgorithm() );
    
    /**
     * Gets the object that offers fulltext search.
     * @return the object that offers fulltext search.
     */
    static FulltextSearcher getFulltextSearcher() {
        return searcher;
    }
    
    
    
    /**
     * Gets vector of messages (instances of the class MessageHeader) that match
     * given search settings. See search settings for details.
     * 
     * @param settings the settings of the search.
     * @param searchBox the box that displays results of searching.
     * @param progress the object to that the progress will be displayed and that
     *  notifies the algorithm whether it should stop it's work.
     * @return Vector of instances of MessageHeader - messages that match given
     *  search settings. 
     * @see SearchSettings
     */
    public static Vector search(SearchSettings settings, SearchBox searchBox, StoppableProgress progress) {
        
        Vector matchedMessages = new Vector();
        // for all boxes
        for (Enumeration boxes = settings.getBoxes(); boxes.hasMoreElements(); ) {
            TheBox actBox = (TheBox) boxes.nextElement();
              if (DEBUG) { System.out.println("Scanning box: " + actBox); }
            // for all messages in each box
            int numMessage = 1;
            for (Enumeration messages = actBox.getMessages(); messages.hasMoreElements(); numMessage++) {
                // do search
                MessageHeader message = (MessageHeader) messages.nextElement();
                  if (DEBUG) { System.out.println("Scanning message: " + message.getSubject()); }
                MessageSearchResult result = search(settings, message);
                if (result != MessageSearchResult.NO_MATCH) {
                    // add to matched messages
                    message.setSearchResult(result);
                    matchedMessages.addElement(message);
                    
                    // add to box
                      if (DEBUG) { System.out.println("Adding message to the box"); }
                    searchBox.storeMail(message);
                    if ( progress.isDisplayed() ) {
                        MujMail.mujmail.getDisplay().setCurrent(searchBox);
                    }
                }
                setProgress(progress, actBox, numMessage);

                // interrupt the search if the box is not busy
                if (progress.stopped()) {
                    //reportBox.searchInterrupted();
                    return matchedMessages;
                }
                //Functions.sleep(100);
                
            }
            
        }
        
        return matchedMessages;
    }
    
    /**
     * Search the message with given settings and return the result.
     * @param settings the search settings - contains information where search etc.
     * @param message the message to be searched.
     * @return the search result. MessageSearchResult.NO_MATCH if the message
     *  does not match this search settings.
     * @see SearchSettings
     */
    private static MessageSearchResult search(SearchSettings settings, MessageHeader message) {
        if (!verifyDate(settings.getDateInterval(), message)) {
            // the message is not if specified date interval
            return MessageSearchResult.NO_MATCH;
        }
        
        return findSearchPhrases(settings.getSearchPhrases(), message);
    }

    /**
     * Set the progress bar in report box while searching.
     * @param progress the progressBar to which set the progress
     * @param actBox actually scanned box
     * @param numMessage the number of actually scanned message in actBox
     */
    private static void setProgress(Progress progress, TheBox actBox, int numMessage) {
          if (DEBUG) { System.out.println("before setting title"); }
        progress.setTitle(Lang.get(Lang.SEA_SEARCHING_IN_BOX) + actBox.getName());
          if (DEBUG) { System.out.println("update progress"); }
        progress.updateProgress(actBox.getMessageCount(), numMessage);
          if (DEBUG) { System.out.println("completed");}
    }
    
    /**
     * Verify whether the message is in specified date interval.
     * @param dateInterval the date interval specified in the search settings
     * @param message the message to be searched.
     * @return true if the message is in specified date interval.
     */
    private static boolean verifyDate(DateInterval dateInterval, MessageHeader message) {
        if (dateInterval != DateInterval.NOT_SPECIFIED) {
            //if (!dateInterval.contains(new Date(message.getTime()))) {
            System.out.println(dateInterval);
            if (!dateInterval.contains(message.getTime())) {
                if (DEBUG) { System.out.println("Does not contain"); }
                // the message is not if specified date interval
               return false;
            }
            
            if (DEBUG) { System.out.println("Contains"); }
        }
        
        // the message is in specified date interval or date interval is not specified
        return true;
    }
    
    /**
     * Tries to find search phrases specified in settings.
     * @param searchPhrases the phrases to be searched in the message.
     * @param message the message to be searched.
     * @return the instance of MessageSearchResult with no occurrences if 
     *  searchPhrases is empty.
     *  MessageSearchResult.NO_MATCH if searchPhrases is not empty and there was
     *  not found any occurrence of some required search phrase in the message or
     *  there was not found any occurrence of any search phrase and all search
     *  phrases are optional.
     *  the instance of MessageSearchResult with occurrences of search phrases
     *  if at least one occurrence of some search phrases was found and occurrences
     *  of all required search phrases was found..
     */
    private static MessageSearchResult findSearchPhrases(Enumeration searchPhrases, MessageHeader message) {
        if (!searchPhrases.hasMoreElements()) {
            // if there are no search phrases, the message matches
            return new MessageSearchResult();
        } else {
            // search all phrases
            Vector occurrences = new Vector();
            while (searchPhrases.hasMoreElements()) {
                SearchPhrase phrase = (SearchPhrase) searchPhrases.nextElement();
                MessageSearchResult.Occurrence occurrence = phrase.findFirstMatch(message);
                if (occurrence == MessageSearchResult.Occurrence.NO_OCCURRENCE && 
                    phrase.getImportance() == SearchPhrase.PhraseImportance.REQUIRED) {
                    // it was not found any occurrence of required phrase in the message
                    return MessageSearchResult.NO_MATCH;
                } else if (occurrence != MessageSearchResult.Occurrence.NO_OCCURRENCE) {
                    // it was found some occurrence
                    occurrences.addElement(occurrence);
                }
            }
            
            if (occurrences.isEmpty()) {
                // all phrases were optional and no occurrence was found
                return MessageSearchResult.NO_MATCH;
            } else {
                // at least one occurrence of some search phrase was found, occurrences
                // of all required phrases was found
                return new MessageSearchResult(occurrences);
            }
        }
    }
}
