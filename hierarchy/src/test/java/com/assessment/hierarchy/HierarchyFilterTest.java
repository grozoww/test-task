package com.assessment.hierarchy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchyFilterTest {

    private static final IntPredicate ALWAYS = id -> true;
    private static final IntPredicate NEVER = id -> false;
    private static final IntPredicate NOT_DIVISIBLE_BY_3 = id -> id % 3 != 0;

    @Nested
    @DisplayName("Provided example")
    class ProvidedExample {

        @Test
        @DisplayName("drops nodes divisible by 3 together with their subtrees")
        void filtersTheReferenceForest() {
            Hierarchy unfiltered = hierarchy(
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
            );

            Hierarchy actual = HierarchyFilter.filter(unfiltered, NOT_DIVISIBLE_BY_3);

            assertHierarchy(
                new int[]{1, 2, 5, 8, 10, 11},
                new int[]{0, 1, 1, 0, 1, 2},
                actual
            );
        }
    }

    @Nested
    @DisplayName("Degenerate inputs")
    class DegenerateInputs {

        @Test
        @DisplayName("empty hierarchy stays empty")
        void emptyHierarchy() {
            Hierarchy empty = hierarchy(new int[]{}, new int[]{});
            assertHierarchy(new int[]{}, new int[]{}, HierarchyFilter.filter(empty, ALWAYS));
            assertHierarchy(new int[]{}, new int[]{}, HierarchyFilter.filter(empty, NEVER));
        }

        @Test
        @DisplayName("single root that passes is kept")
        void singleNodeKept() {
            Hierarchy single = hierarchy(new int[]{42}, new int[]{0});
            assertHierarchy(new int[]{42}, new int[]{0}, HierarchyFilter.filter(single, ALWAYS));
        }

        @Test
        @DisplayName("single root that fails is dropped")
        void singleNodeDropped() {
            Hierarchy single = hierarchy(new int[]{42}, new int[]{0});
            assertHierarchy(new int[]{}, new int[]{}, HierarchyFilter.filter(single, NEVER));
        }
    }

    @Nested
    @DisplayName("Whole-tree predicates")
    class WholeTreePredicates {

        @Test
        @DisplayName("always-true predicate returns an identical hierarchy")
        void identityFilter() {
            int[] ids = {1, 2, 3, 4, 5};
            int[] depths = {0, 1, 2, 1, 0};
            Hierarchy actual = HierarchyFilter.filter(hierarchy(ids, depths), ALWAYS);
            assertHierarchy(ids, depths, actual);
        }

        @Test
        @DisplayName("always-false predicate empties the hierarchy")
        void everythingFiltered() {
            Hierarchy input = hierarchy(new int[]{1, 2, 3, 4, 5}, new int[]{0, 1, 2, 1, 0});
            assertHierarchy(new int[]{}, new int[]{}, HierarchyFilter.filter(input, NEVER));
        }
    }

    @Nested
    @DisplayName("Ancestry rules")
    class AncestryRules {

        @Test
        @DisplayName("a failing root removes its entire subtree")
        void failingRootRemovesSubtree() {
            // 1(root) -> 2 -> 3 ; 10(root) -> 11
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 10, 11},
                new int[]{0, 1, 2, 0, 1}
            );
            // drop the first root (id 1); its descendants 2 and 3 must disappear too
            Hierarchy actual = HierarchyFilter.filter(input, id -> id != 1);
            assertHierarchy(new int[]{10, 11}, new int[]{0, 1}, actual);
        }

        @Test
        @DisplayName("a failing intermediate node removes its subtree but keeps its siblings")
        void failingIntermediateRemovesOnlyItsSubtree() {
            // 1 -> {2 -> {3, 4}, 5 -> 6}
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4, 5, 6},
                new int[]{0, 1, 2, 2, 1, 2}
            );
            // drop node 2: 3 and 4 go with it, but sibling subtree 5 -> 6 stays
            Hierarchy actual = HierarchyFilter.filter(input, id -> id != 2);
            assertHierarchy(new int[]{1, 5, 6}, new int[]{0, 1, 2}, actual);
        }

        @Test
        @DisplayName("a passing node is still dropped when one of its ancestors fails")
        void passingNodeDroppedWhenAncestorFails() {
            // chain 1 -> 2 -> 3 -> 4, fail only node 2
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4},
                new int[]{0, 1, 2, 3}
            );
            Hierarchy actual = HierarchyFilter.filter(input, id -> id != 2);
            // 3 and 4 pass the predicate themselves but lose their ancestor 2
            assertHierarchy(new int[]{1}, new int[]{0}, actual);
        }

        @Test
        @DisplayName("dropping a leaf does not affect anything else")
        void droppingLeafKeepsRest() {
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4},
                new int[]{0, 1, 2, 1}
            );
            Hierarchy actual = HierarchyFilter.filter(input, id -> id != 3);
            assertHierarchy(new int[]{1, 2, 4}, new int[]{0, 1, 1}, actual);
        }
    }

    @Nested
    @DisplayName("Forests and shapes")
    class ForestsAndShapes {

        @Test
        @DisplayName("multiple independent roots are filtered independently")
        void multipleRoots() {
            // three single-node roots
            Hierarchy input = hierarchy(new int[]{1, 2, 3}, new int[]{0, 0, 0});
            Hierarchy actual = HierarchyFilter.filter(input, id -> id != 2);
            assertHierarchy(new int[]{1, 3}, new int[]{0, 0}, actual);
        }

        @Test
        @DisplayName("a deep chain is cut at the first failing node")
        void deepChainCutInTheMiddle() {
            int n = 1000;
            int[] ids = new int[n];
            int[] depths = new int[n];
            for (int i = 0; i < n; i++) {
                ids[i] = i + 1;
                depths[i] = i;
            }
            // fail node with id 500 -> keep ids 1..499 (depths 0..498)
            Hierarchy actual = HierarchyFilter.filter(hierarchy(ids, depths), id -> id != 500);
            assertEquals(499, actual.size());
            for (int i = 0; i < actual.size(); i++) {
                assertEquals(i + 1, actual.nodeId(i));
                assertEquals(i, actual.depth(i));
            }
            assertValidHierarchy(actual);
        }

        @Test
        @DisplayName("a wide single-level tree keeps only the passing children")
        void wideTree() {
            // root 100 with children 1..6
            Hierarchy input = hierarchy(
                new int[]{100, 1, 2, 3, 4, 5, 6},
                new int[]{0, 1, 1, 1, 1, 1, 1}
            );
            Hierarchy actual = HierarchyFilter.filter(input, NOT_DIVISIBLE_BY_3);
            assertHierarchy(new int[]{100, 1, 2, 4, 5}, new int[]{0, 1, 1, 1, 1}, actual);
        }
    }

    @Nested
    @DisplayName("General guarantees")
    class GeneralGuarantees {

        @Test
        @DisplayName("the input hierarchy is not mutated")
        void inputIsNotMutated() {
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
            );
            String before = input.formatString();
            HierarchyFilter.filter(input, NOT_DIVISIBLE_BY_3);
            assertEquals(before, input.formatString(), "filter() must not change its input");
        }

        @Test
        @DisplayName("the predicate is evaluated exactly once per node")
        void predicateEvaluatedOncePerNode() {
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4, 5},
                new int[]{0, 1, 2, 1, 0}
            );
            int[] calls = {0};
            HierarchyFilter.filter(input, id -> {
                calls[0]++;
                return true;
            });
            assertEquals(input.size(), calls[0]);
        }

        @Test
        @DisplayName("the filtered result is itself a valid hierarchy")
        void resultIsValidHierarchy() {
            Hierarchy input = hierarchy(
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
            );
            assertValidHierarchy(HierarchyFilter.filter(input, NOT_DIVISIBLE_BY_3));
        }
    }

    // --- helpers ---------------------------------------------------------

    private static Hierarchy hierarchy(int[] ids, int[] depths) {
        return new ArrayBasedHierarchy(ids, depths);
    }

    private static void assertHierarchy(int[] expectedIds, int[] expectedDepths, Hierarchy actual) {
        Hierarchy expected = new ArrayBasedHierarchy(expectedIds, expectedDepths);
        assertEquals(expected.formatString(), actual.formatString());
    }

    /** Asserts the depth-array invariants described on {@link Hierarchy}. */
    private static void assertValidHierarchy(Hierarchy h) {
        if (h.size() == 0) {
            return;
        }
        assertEquals(0, h.depth(0), "the first node must be a root (depth 0)");
        for (int i = 1; i < h.size(); i++) {
            int prev = h.depth(i - 1);
            int cur = h.depth(i);
            assertTrue(cur >= 0, "depth must never be negative");
            assertTrue(cur <= prev + 1,
                "depth may grow by at most one (from " + prev + " to " + cur + " at index " + i + ")");
        }
    }
}
