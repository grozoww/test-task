import kotlin.test.*

/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * Invariants on the depths array:
 *  * Depth of the first element is 0.
 *  * If the depth of a node is `D`, the depth of the next node in the array can be:
 *      * `D + 1` if the next node is a child of this node;
 *      * `D` if the next node is a sibling of this node;
 *      * `d < D` - in this case the next node is not related to this node.
 */
interface Hierarchy {
  /** The number of nodes in the hierarchy. */
  val size: Int

  /**
   * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be `depth(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun nodeId(index: Int): Int

  /**
   * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be `nodeId(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun depth(index: Int): Int

  fun formatString(): String {
    return (0 until size).joinToString(
      separator = ", ",
      prefix = "[",
      postfix = "]"
    ) { i -> "${nodeId(i)}:${depth(i)}" }
  }
}

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
  val keptNodeIds = IntArray(size)
  val keptDepths = IntArray(size)
  val ancestorKept = BooleanArray(size)
  var kept = 0
  for (i in 0 until size) {
    val d = depth(i)
    val parentKept = d == 0 || ancestorKept[d - 1]
    val keep = parentKept && nodeIdPredicate(nodeId(i))
    ancestorKept[d] = keep
    if (keep) {
      keptNodeIds[kept] = nodeId(i)
      keptDepths[kept] = d
      kept++
    }
  }
  return ArrayBasedHierarchy(keptNodeIds.copyOf(kept), keptDepths.copyOf(kept))
}

class ArrayBasedHierarchy(
  private val myNodeIds: IntArray,
  private val myDepths: IntArray,
) : Hierarchy {
  init {
    require(myNodeIds.size == myDepths.size) { "nodeIds and depths must have equal length" }
    if (myDepths.isNotEmpty()) {
      require(myDepths[0] == 0) { "the first depth must be 0" }
      for (i in myDepths.indices) {
        require(myDepths[i] >= 0) { "depths must be non-negative" }
        if (i > 0) require(myDepths[i] <= myDepths[i - 1] + 1) { "depth can increase by at most 1" }
      }
    }
  }

  override val size: Int = myDepths.size

  override fun nodeId(index: Int): Int = myNodeIds[index]

  override fun depth(index: Int): Int = myDepths[index]
}

class FilterTest {
  private fun assertFiltered(
    nodeIds: IntArray,
    depths: IntArray,
    expectedNodeIds: IntArray,
    expectedDepths: IntArray,
    predicate: (Int) -> Boolean,
  ): Hierarchy {
    val actual = ArrayBasedHierarchy(nodeIds, depths).filter(predicate)
    val expected = ArrayBasedHierarchy(expectedNodeIds, expectedDepths)
    assertEquals(expected.formatString(), actual.formatString())
    assertValidDepths(actual)
    return actual
  }

  private fun assertValidDepths(hierarchy: Hierarchy) {
    if (hierarchy.size == 0) return
    assertEquals(0, hierarchy.depth(0))
    for (i in 0 until hierarchy.size) {
      assertTrue(hierarchy.depth(i) >= 0)
      if (i > 0) assertTrue(hierarchy.depth(i) <= hierarchy.depth(i - 1) + 1)
    }
  }

  @Test
  fun testFilter() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2),
      intArrayOf(1, 2, 5, 8, 10, 11),
      intArrayOf(0, 1, 1, 0, 1, 2),
    ) { nodeId -> nodeId % 3 != 0 }
  }

  @Test
  fun testEmptyInput() {
    assertFiltered(IntArray(0), IntArray(0), IntArray(0), IntArray(0)) { true }
  }

  @Test
  fun testSingleNodePass() {
    assertFiltered(intArrayOf(7), intArrayOf(0), intArrayOf(7), intArrayOf(0)) { true }
  }

  @Test
  fun testSingleNodeFail() {
    assertFiltered(intArrayOf(7), intArrayOf(0), IntArray(0), IntArray(0)) { false }
  }

  @Test
  fun testAllPass() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2),
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2),
    ) { true }
  }

  @Test
  fun testAllFail() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2),
      IntArray(0),
      IntArray(0),
    ) { false }
  }

  @Test
  fun testFailedRootPrunesSubtree() {
    assertFiltered(
      intArrayOf(1, 2, 3),
      intArrayOf(0, 1, 0),
      intArrayOf(3),
      intArrayOf(0),
    ) { nodeId -> nodeId != 1 }
  }

  @Test
  fun testFailedIntermediatePrunesDescendants() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4),
      intArrayOf(0, 1, 2, 1),
      intArrayOf(1, 4),
      intArrayOf(0, 1),
    ) { nodeId -> nodeId != 2 }
  }

  @Test
  fun testLeafRejection() {
    assertFiltered(
      intArrayOf(1, 2, 3),
      intArrayOf(0, 1, 2),
      intArrayOf(1, 2),
      intArrayOf(0, 1),
    ) { nodeId -> nodeId != 3 }
  }

  @Test
  fun testMultipleRoots() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4),
      intArrayOf(0, 0, 0, 0),
      intArrayOf(1, 3),
      intArrayOf(0, 0),
    ) { nodeId -> nodeId % 2 != 0 }
  }

  @Test
  fun testWideTree() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4, 5, 6),
      intArrayOf(0, 1, 1, 1, 1, 1),
      intArrayOf(1, 2, 4, 6),
      intArrayOf(0, 1, 1, 1),
    ) { nodeId -> nodeId == 1 || nodeId % 2 == 0 }
  }

  @Test
  fun testDeepChain() {
    assertFiltered(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 3, 4),
      intArrayOf(1, 2),
      intArrayOf(0, 1),
    ) { nodeId -> nodeId != 3 }
  }

  @Test
  fun testInputIsNotMutated() {
    val nodeIds = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    val depths = intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2)
    val nodeIdsCopy = nodeIds.copyOf()
    val depthsCopy = depths.copyOf()
    val source = ArrayBasedHierarchy(nodeIds, depths)
    val before = source.formatString()
    source.filter { nodeId -> nodeId % 3 != 0 }
    assertEquals(before, source.formatString())
    assertTrue(nodeIds contentEquals nodeIdsCopy)
    assertTrue(depths contentEquals depthsCopy)
  }

  @Test
  fun testPredicateIsNotCalledForDescendantsOfRejectedNodes() {
    var calls = 0
    val source = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val result = source.filter { nodeId ->
      calls++
      nodeId % 3 != 0
    }
    assertEquals(9, calls)
    assertEquals("[1:0, 2:1, 5:1, 8:0, 10:1, 11:2]", result.formatString())
  }

  @Test
  fun testRejectsUnequalLengths() {
    assertFailsWith<IllegalArgumentException> {
      ArrayBasedHierarchy(intArrayOf(1, 2), intArrayOf(0))
    }
  }

  @Test
  fun testRejectsNonZeroFirstDepth() {
    assertFailsWith<IllegalArgumentException> {
      ArrayBasedHierarchy(intArrayOf(1), intArrayOf(1))
    }
  }

  @Test
  fun testRejectsNegativeDepth() {
    assertFailsWith<IllegalArgumentException> {
      ArrayBasedHierarchy(intArrayOf(1, 2), intArrayOf(0, -1))
    }
  }

  @Test
  fun testRejectsInvalidUpwardJump() {
    assertFailsWith<IllegalArgumentException> {
      ArrayBasedHierarchy(intArrayOf(1, 2), intArrayOf(0, 2))
    }
  }
}