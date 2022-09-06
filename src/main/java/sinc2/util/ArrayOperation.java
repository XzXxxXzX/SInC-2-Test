package sinc2.util;

import java.util.List;
import java.util.Set;

/**
 * Helper functions for array operations.
 *
 * @since 2.0
 */
public class ArrayOperation {

    /**
     * Convert an "Integer" list to an "int" array.
     */
    public static int[] toArray(List<Integer> list){
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * Convert an "Integer" set to an "int" array.
     */
    public static int[] toArray(Set<Integer> set) {
        int[] array = new int[set.size()];
        int idx = 0;
        for (int i: set) {
            array[idx] = i;
            idx++;
        }
        return array;
    }

    /**
     * Initialize an array with initial value.
     */
    public static int[] initArrayWithValue(int length, int initValue) {
        int[] arr = new int[length];
        for (int i = 0; i < length; i++) {
            arr[i] = initValue;
        }
        return arr;
    }
}
