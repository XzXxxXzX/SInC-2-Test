package sinc2.kb;

import java.util.Arrays;

/**
 * Wrapper class for an integer array, which is, a record.
 *
 * @since 2.0
 */
public class Record {

    /** The argument array */
    public final int[] args;

    public Record(int[] args) {
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return Arrays.equals(args, record.args);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(args);
    }

    @Override
    public String toString() {
        return Arrays.toString(args);
    }
}
