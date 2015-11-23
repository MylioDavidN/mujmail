package mujmail.ordering.comparator;

import mujmail.util.Functions;
import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/** 
 * Comparing messages based on receiver. Usefull if retrieving more accounts in one box.
*/
public class RecipientComparator extends MessageHeaderComparator {

    /** Creates comparator */
    public RecipientComparator(final Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        final String email1 = Functions
            .emailOnly( messageHeader1.getRecipients() )
            .toLowerCase();
        final String email2 = Functions
            .emailOnly( messageHeader2.getRecipients() )
            .toLowerCase();
        final String recipient1 = email1.substring( 0, email1.indexOf("@") );
        final String recipient2 = email2.substring( 0, email2.indexOf("@") );
        return recipient1.compareTo( recipient2 );
    }

}
