package sinc2.exp;

import org.junit.jupiter.api.Test;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.kb.NumerationMap;
import sinc2.rule.RuleParseException;
import sinc2.rule.SpecOprCase2;
import sinc2.rule.SpecOprCase3;
import sinc2.rule.SpecOprCase4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HintTest {
    @Test
    void testConstruction1() throws RuleParseException {
        /* p(X,X):-;[] */
        NumerationMap num_map = new NumerationMap();
        int p = num_map.mapName("p") - 1;
        Hint hint = new Hint("p(X,X):-;[]", num_map);
        assertEquals(
                new ArrayList<>(List.of(
                        new Predicate(p, new int[]{Argument.variable(0), Argument.variable(0)})
                )), hint.template
        );
        assertTrue(hint.restrictions.isEmpty());
        assertEquals(
                new ArrayList<>(List.of(
                        new SpecOprCase3(0, 0, 0, 1)
                )), hint.operations
        );
        assertArrayEquals(new int[]{2}, hint.functorArities);
        int[][] expected_counter_links = new int[1][];
        expected_counter_links[p] = new int[0];
        assertArrayEquals(expected_counter_links, hint.functorRestrictionCounterLink);
        assertEquals(0, hint.restrictionCounterBounds.length);
    }

    @Test
    void testConstruction2() throws RuleParseException {
        /* p(X,Y):-q(X,Y);[(p,q)] */
        NumerationMap num_map = new NumerationMap();
        int p = num_map.mapName("p") - 1;
        int q = num_map.mapName("q") - 1;
        Hint hint = new Hint("p(X,Y):-q(X,Y);[(p,q)]", num_map);
        assertEquals(
                new ArrayList<>(List.of(
                        new Predicate(p, new int[]{Argument.variable(0), Argument.variable(1)}),
                        new Predicate(q, new int[]{Argument.variable(0), Argument.variable(1)})
                )), hint.template
        );
        checkRestrictions(new ArrayList<>(List.of(new int[]{p, q})), hint.restrictions);
        assertEquals(
                new ArrayList<>(List.of(
                        new SpecOprCase4(q, 2, 0, 0, 0),
                        new SpecOprCase3(0, 1, 1, 1)
                )), hint.operations
        );
        assertArrayEquals(new int[]{2, 2}, hint.functorArities);
        int[][] expected_counter_links = new int[2][];
        expected_counter_links[p] = new int[]{0};
        expected_counter_links[q] = new int[]{0};
        assertArrayEquals(expected_counter_links, hint.functorRestrictionCounterLink);
        int[] expected_counter_bounds = new int[]{2};
        assertArrayEquals(expected_counter_bounds, hint.restrictionCounterBounds);
    }

    @Test
    void testConstruction3() throws RuleParseException {
        /* p(X,Y):-q(X,Z),r(Z,Y);[(p,q),(p,r)] */
        NumerationMap num_map = new NumerationMap();
        int p = num_map.mapName("p") - 1;
        int q = num_map.mapName("q") - 1;
        int r = num_map.mapName("r") - 1;
        Hint hint = new Hint("p(X,Y):-q(X,Z),r(Z,Y);[(p,q),(p,r)]", num_map);
        assertEquals(
                new ArrayList<>(List.of(
                        new Predicate(p, new int[]{Argument.variable(0), Argument.variable(1)}),
                        new Predicate(q, new int[]{Argument.variable(0), Argument.variable(2)}),
                        new Predicate(r, new int[]{Argument.variable(2), Argument.variable(1)})
                )), hint.template
        );
        checkRestrictions(new ArrayList<>(List.of(new int[]{p, q}, new int[]{p, r})), hint.restrictions);
        assertEquals(
                new ArrayList<>(List.of(
                        new SpecOprCase4(q, 2, 0, 0, 0),
                        new SpecOprCase4(r, 2, 1, 0, 1),
                        new SpecOprCase3(1, 1, 2, 0)
                )), hint.operations
        );
        assertArrayEquals(new int[]{2, 2, 2}, hint.functorArities);
        int[][] expected_counter_links = new int[3][];
        expected_counter_links[p] = new int[]{0, 1};
        expected_counter_links[q] = new int[]{0};
        expected_counter_links[r] = new int[]{1};
        assertArrayEquals(expected_counter_links, hint.functorRestrictionCounterLink);
        int[] expected_counter_bounds = new int[]{2, 2};
        assertArrayEquals(expected_counter_bounds, hint.restrictionCounterBounds);
    }

    @Test
    void testConstruction4() throws RuleParseException {
        /* p(X,Y):-q(X,Y,Z),r(Z,Y);[(p,q),(p,r,q)] */
        NumerationMap num_map = new NumerationMap();
        int p = num_map.mapName("p") - 1;
        int q = num_map.mapName("q") - 1;
        int r = num_map.mapName("r") - 1;
        Hint hint = new Hint("p(X,Y):-q(X,Y,Z),r(Z,Y);[(p,q),(p,r,q)]", num_map);
        assertEquals(
                new ArrayList<>(List.of(
                        new Predicate(p, new int[]{Argument.variable(0), Argument.variable(1)}),
                        new Predicate(q, new int[]{Argument.variable(0), Argument.variable(1), Argument.variable(2)}),
                        new Predicate(r, new int[]{Argument.variable(2), Argument.variable(1)})
                )), hint.template
        );
        checkRestrictions(new ArrayList<>(List.of(new int[]{p, q}, new int[]{p, r, q})), hint.restrictions);
        assertEquals(
                new ArrayList<>(List.of(
                        new SpecOprCase4(q, 3, 0, 0, 0),
                        new SpecOprCase3(0, 1, 1, 1),
                        new SpecOprCase2(r, 2, 1, 1),
                        new SpecOprCase3(1, 2, 2, 0)
                )), hint.operations
        );
        assertArrayEquals(new int[]{2, 3, 2}, hint.functorArities);
        int[][] expected_counter_links = new int[3][];
        expected_counter_links[p] = new int[]{0, 1};
        expected_counter_links[q] = new int[]{0, 1};
        expected_counter_links[r] = new int[]{1};
        assertArrayEquals(expected_counter_links, hint.functorRestrictionCounterLink);
        int[] expected_counter_bounds = new int[]{2, 3};
        assertArrayEquals(expected_counter_bounds, hint.restrictionCounterBounds);
    }

    void checkRestrictions(List<int[]> expected, List<int[]> actual) {
        for (int i = 0; i < expected.size(); i++) {
            HashSet<Integer> expected_set = new HashSet<>();
            HashSet<Integer> actual_set = new HashSet<>();
            for (int ie: expected.get(i)) {
                expected_set.add(ie);
            }
            for (int ia: actual.get(i)) {
                actual_set.add(ia);
            }
            assertEquals(expected_set, actual_set, String.format("Different @%d", i));
        }
    }
}