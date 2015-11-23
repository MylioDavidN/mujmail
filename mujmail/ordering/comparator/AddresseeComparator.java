package mujmail.ordering.comparator;

import mujmail.util.Functions;
import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/** 
 * Compare messege headers by sender. 
 * Takes into consideration only pre @ (username) parts of mail adress.
 *
 */
public class AddresseeComparator extends MessageHeaderComparator {

    /**
     * Comparator that compare message header by sender's name.
     * @param ordering Natural of Nonnatural ordering type
     */
    public AddresseeComparator(final Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        final String email1 = Functions
            .emailOnly( messageHeader1.getFrom() )
            .toLowerCase();
        final String email2 = Functions
            .emailOnly( messageHeader2.getFrom() )
            .toLowerCase();
        final String addressee1 = email1.substring( 0, email1.indexOf("@") );
        final String addressee2 = email2.substring( 0, email2.indexOf("@") );
        return addressee1.compareTo( addressee2 );
    }

}
