package sinc.util;

public class DisjointSet {
    private final int[] sets;

    public DisjointSet(int capacity) {
        sets = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            sets[i] = i;
        }
    }

    public int findSet(int idx) {
        while (sets[idx] != idx) {
            idx = sets[idx];
        }
        return idx;
    }

    public void unionSets(int idx1, int idx2) {
        int set1 = findSet(idx1);
        int set2 = findSet(idx2);
        if (set1 != set2) {
            sets[set1] = set2;
        }
    }

    public int totalSets() {
        int cnt = 0;
        for (int i = 0; i < sets.length; i++) {
            if (i == sets[i]) {
                cnt++;
            }
        }
        return cnt;
    }
}
