package com.assessment.hierarchy;

public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        Hierarchy input = new ArrayBasedHierarchy(
            new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
            new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
        );

        Hierarchy filtered = HierarchyFilter.filter(input, id -> id % 3 != 0);

        System.out.println("predicate: keep node ids not divisible by 3");
        System.out.println("input:     " + input.formatString());
        System.out.println("filtered:  " + filtered.formatString());
    }
}
