package mujmail.ordering.comparator;

import mujmail.MessageHeader;
import mujmail.ordering.Ordering;

/** 
 * Comparing messages based on position in persistent database
 */
public class RecordIDComparator extends MessageHeaderComparator {

    /** Creates comparator */
    public RecordIDComparator(Ordering ordering) {
        super(ordering);
    }

    protected int compare(MessageHeader messageHeader1, MessageHeader messageHeader2) {
        return messageHeader1.getRecordID() - messageHeader2.getRecordID();
    }

}
