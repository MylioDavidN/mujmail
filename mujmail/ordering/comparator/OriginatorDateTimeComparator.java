package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Ordering;


/**
 * Comparing messages based on time when message was sent.
 */
public class OriginatorDateTimeComparator extends MessageHeaderComparator {

    /** Create comparator */
    public OriginatorDateTimeComparator(Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
          // substraction is wrong here, because result is of type long
        return (messageHeader1.getTime() < messageHeader2.getTime())? -1 : 1;
    }

}
