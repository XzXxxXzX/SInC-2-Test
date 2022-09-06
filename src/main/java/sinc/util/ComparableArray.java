package sinc.util;

import java.util.Arrays;

public class ComparableArray<T> {
    public final T[] arr;

    public ComparableArray(T[] arr) {
        this.arr = arr;
    }

    public ComparableArray(ComparableArray<T> another) {
        this.arr = another.arr.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparableArray<?> that = (ComparableArray<?>) o;
        return Arrays.equals(arr, that.arr);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(arr);
    }
}
