package com.assessment.hierarchy;

import java.util.Objects;

public final class ArrayBasedHierarchy implements Hierarchy {
    private final int[] nodeIds;
    private final int[] depths;

    public ArrayBasedHierarchy(int[] nodeIds, int[] depths) {
        this.nodeIds = Objects.requireNonNull(nodeIds, "nodeIds");
        this.depths = Objects.requireNonNull(depths, "depths");
        if (nodeIds.length != depths.length) {
            throw new IllegalArgumentException(
                "nodeIds and depths must have the same length: " + nodeIds.length + " != " + depths.length);
        }
    }

    @Override
    public int size() {
        return depths.length;
    }

    @Override
    public int nodeId(int index) {
        return nodeIds[index];
    }

    @Override
    public int depth(int index) {
        return depths[index];
    }
}
