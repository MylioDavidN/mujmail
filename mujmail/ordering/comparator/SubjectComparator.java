package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/**
 * Comparing messages based on mails subjects
 */
public class SubjectComparator extends MessageHeaderComparator {

    /** Creates comparator */
    public SubjectComparator(final Ordering ordering) {
        super(ordering);
    }

    /**
     * Compares subjects in messages case-insensitively.
     * 
     * @param messageHeader1 message where first subject have to be gotten
     * @param messageHeader2 message where second subject have to be gotten
     */
    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        final String firstSubject = removeREsAndFWDs( messageHeader1.getSubject().toLowerCase() );
        final String secondSubject = removeREsAndFWDs( messageHeader2.getSubject().toLowerCase() );
        return firstSubject.compareTo( secondSubject );
    }

    /**
     * Removes from lower case subject leading "re:" and "fwd:" substrings.
     * 
     * @param subject lower case subject
     * @return subject without leading 're:'s and "fwd:"s
     */
    private String removeREsAndFWDs( final String subject ) {
          //System.out.println( "subject='" + subject + "'" );
        String result = subject.trim();
        while ( true ) {
            if ( result.startsWith("re:") ) {
                result = result.substring(3).trim();
            } else if ( result.startsWith("fwd:") ) {
                result = result.substring(4).trim();
            } else {
                break;
            }
        }
          //System.out.println( "result='" + result + "'" );
        return result;
    }
}
