## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```kotlin
import java.util.concurrent.ConcurrentHashMap

/*
 * Issue #1: There's no way to delete a value from the cache (only put/get).
 *   Impact: if the real data changes in the DB, the cache keeps serving the old, wrong value
 *           until its timer runs out. We can't kick a known-bad value out on demand.
 *   Fix: add a remove-one-key method and a clear-everything method.
 *
 * Issue #2: There's no way to see if the cache is actually helping.
 *   Impact: we don't count how often a value was found vs not found, how often it was loaded,
 *           or how often loads failed. In production we're flying blind - we can't tell if the
 *           cache helps or if the 1-minute timer is set right.
 *   Fix: add simple counters (found / not-found / loads / load failures / size).
 */
class SimpleCache<K, V> {

    // Issue #3: The cache has no size limit and never throws anything away.
    //   Impact: it just keeps growing forever until the app runs out of memory and crashes.
    //   Fix: set a maximum size, and a rule for what to drop when it's full
    //        (for example, drop the entries that haven't been used for the longest time).
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()

    // Issue #4: The 60-second lifetime is written straight into the code.
    //   Impact: you can't change it without editing and rebuilding the app.
    //   Fix: pass it in as a setting (constructor parameter), ideally per-entry.
    private val ttlMs = 60000 // 1 minute

    // Issue #5 (minor): the inner CacheEntry<V> reuses the same letter V as the outer class.
    //   Impact: confusing to read - the inner V hides the outer one.
    //   Fix: give the inner class a different letter, e.g. CacheEntry<T>.
    data class CacheEntry<V>(val value: V, val timestamp: Long)

    fun put(key: K, value: V) {
        // Issue #6: System.currentTimeMillis() is the normal wall clock, and it can jump.
        //   Impact: when the server re-syncs its time, or on daylight-saving changes, the clock
        //           can move backward or forward. That makes entries expire too early, or live
        //           too long, or all expire at once.
        //   Fix: use System.nanoTime() - it's a clock that only ever ticks forward and never
        //        jumps, so measuring "how much time has passed" is reliable.
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }

    fun get(key: K): V? {
        val entry = cache[key]
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                return entry.value
            }
            // Issue #7: expired values are never actually deleted -> memory leak. Worst bug here.
            //   Impact: when a value is too old, get() returns null but leaves it in the map.
            //           So old junk piles up forever, and eventually the app runs out of memory.
            //   Fix: delete it right here. Use cache.remove(key, entry) with BOTH arguments - it
            //        deletes only if the map still holds THIS exact old entry. That way, if another
            //        thread just wrote a fresh value for the same key, we don't wipe out the new one.
        }

        // Issue #8: when a popular key expires, everyone reloads it at once (a "stampede").
        //   Impact: the moment a hot key expires, every request that wanted it misses at the same
        //           time and they ALL rush to the DB/backend together to reload it - a sudden flood
        //           of identical calls that can knock the backend over.
        //   Fix: make sure only ONE request does the reload while the others wait for its result.
        //        The simplest tool is cache.computeIfAbsent(key) { load(it) }, but watch out:
        //          - while one key is loading, other keys that land in the same internal slot get
        //            blocked too;
        //          - it only kicks in when the key is completely missing - if the old expired value
        //            is still sitting there, it won't help unless you remove the old one first;
        //          - if the load fails, nothing is saved and everyone tries again - a broken backend
        //            turns into a retry flood.
        //        A cleaner way: store a "someone is already loading this" placeholder so exactly one
        //        thread loads and the rest wait on the same result; limit how many loads run at once;
        //        and clear the placeholder if the load fails so it can be retried. Easiest of all:
        //        use a ready-made library like Caffeine that already handles this correctly.

        // Issue #9: returning null doesn't say WHY it's null.
        //   Impact: "never existed", "was here but expired", and even "someone stored null on
        //           purpose" all look identical to the caller.
        //   Fix: either don't allow null values, or return something that clearly says which case
        //        it is.
        return null
    }

    fun size(): Int {
        // Issue #10: size() is misleading, and not free to call.
        //   Impact: it also counts old expired entries, so it reports more items than are really
        //           usable. And ConcurrentHashMap's size is only a rough estimate while other
        //           threads are writing - fine for a dashboard number, but don't use it in logic.
        //   Fix: decide what "size" means: total stored (cheap, but includes expired) is a
        //        different number from actually-alive entries (which needs counting). Expose them
        //        as two separate values.
        return cache.size
    }

    // Issue #11: if you add a background cleaner later, mind its lifecycle and failures.
    //   Impact: whatever runs in the background to clear out old / extra entries needs care:
    //           - if it isn't stopped when the cache is thrown away, it keeps running and leaks
    //             (and can even stop the app from shutting down);
    //           - if it crashes just once, many schedulers silently stop running it forever - and
    //             then the cache quietly grows without limit again.
    //   Fix: tie the cleaner to the cache's lifecycle so it stops with it, and wrap each run in a
    //        try/catch + log so one error doesn't kill it for good.
}
```

## Priority

Thread-safety is fine — `ConcurrentHashMap` plus an entry that never changes after it's created
(a `data class` with `val` fields) means two threads reading and writing at the same time won't
corrupt anything. The real problems are about memory and how it behaves under heavy load.

Fix before going to production: add a size limit + cleanup (#3), stop the memory leak from expired
entries (#7), and stop the reload flood on a popular key (#8). Being able to delete entries (#1) and
see cache stats (#2) come right after. Honestly, instead of building all this by hand, use a proven
library like **Caffeine** — it gives you size limits, expiry, safe one-loader-at-a-time reloads,
and stats out of the box.
