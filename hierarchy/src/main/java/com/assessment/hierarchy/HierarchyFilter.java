package com.assessment.hierarchy;

import java.util.Arrays;
import java.util.function.IntPredicate;

public final class HierarchyFilter {

    private HierarchyFilter() {
    }

    public static Hierarchy filter(Hierarchy hierarchy, IntPredicate nodeIdPredicate) {
        int size = hierarchy.size();
        int[] keptIds = new int[size];
        int[] keptDepths = new int[size];
        int kept = 0;

        boolean[] keptOnPath = new boolean[size];

        for (int i = 0; i < size; i++) {
            int depth = hierarchy.depth(i);
            boolean parentKept = depth == 0 || keptOnPath[depth - 1];
            boolean keep = parentKept && nodeIdPredicate.test(hierarchy.nodeId(i));
            keptOnPath[depth] = keep;
            if (keep) {
                keptIds[kept] = hierarchy.nodeId(i);
                keptDepths[kept] = depth;
                kept++;
            }
        }

        return new ArrayBasedHierarchy(Arrays.copyOf(keptIds, kept), Arrays.copyOf(keptDepths, kept));
    }
}
