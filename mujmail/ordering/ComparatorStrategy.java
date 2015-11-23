package mujmail.ordering;

import mujmail.ordering.comparator.AddresseeComparator;
import mujmail.ordering.comparator.MessageIDComparator;
import mujmail.ordering.comparator.OriginatorDateTimeComparator;
import mujmail.ordering.comparator.RecipientComparator;
import mujmail.ordering.comparator.RecordIDComparator;
import mujmail.ordering.comparator.SizeComparator;
import mujmail.ordering.comparator.SubjectComparator;
import mujmail.ordering.comparator.ThreadingMessageIDComparator;

/**
 * Is used for obtaining comparators usable for sorting mails. 
 */
public class ComparatorStrategy {
    /** Instance of this singleton */
    private static final ComparatorStrategy strategy = new ComparatorStrategy();

    /**
     * Gets singleton ComparatorStrategy object.
     * @return Instance of ComparatorStrategy object
     */
    public static ComparatorStrategy getStrategy() {
        return strategy;
    }

    /** Private constructor to disable instance creation. */
    private ComparatorStrategy() {}

    /**
     * Gets proper Comparator for given sorting and ordering type.
     * 
     * @param ordering Specify if ordering is decreasing or increasing
     * @param criterion Spefify mail etry that is used as key for sorting
     * @return Comparator class which compare mail according given sorting requirements.
     */
    public Comparator getComparator(final Ordering ordering, final Criterion criterion) {
        if ( criterion == Criterion.TIME ) {
            return new OriginatorDateTimeComparator( ordering );
        } else if ( criterion == Criterion.SUBJECT ) {
            return new SubjectComparator( ordering );
        } else if ( criterion == Criterion.TO ) {
            return new RecipientComparator( ordering );
        } else if ( criterion == Criterion.FROM ) {
            return new AddresseeComparator( ordering );
        } else if ( criterion == Criterion.SIZE ) {
            return new SizeComparator( ordering );
        } else if ( criterion == Criterion.RECORD_ID ) {
            return new RecordIDComparator( ordering );
        } else if ( criterion == Criterion.MESSAGE_ID ) {
            return new MessageIDComparator( ordering );
        } else if ( criterion == Criterion.THREADING_MESSAGE_ID ) {
            return new ThreadingMessageIDComparator( ordering );
        } else {
            //#ifdef MUJMAIL_DEVELOPMENT
//#                 System.out.println("ERROR - returning null comparator for ordering: " + ordering + " and criterion: " + criterion);
            //#endif
            return new OriginatorDateTimeComparator( ordering ); // TODO: return null
        }
            
    }
    
}
