package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Comparator;
import mujmail.ordering.Ordering;

/**
 * Comparing messages based on messageID entry.
 */
public class MessageIDComparator extends MessageHeaderComparator implements Comparator {

    /** Creates comparator */
    public MessageIDComparator(Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        return messageHeader1.getMessageID().compareTo( messageHeader2.getMessageID() );
    }

}
