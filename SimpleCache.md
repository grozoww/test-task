## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```java
import java.util.concurrent.ConcurrentHashMap;

public class SimpleCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long ttlMs = 60000; // 1 minute

    public static class CacheEntry<V> {
        private final V value;
        private final long timestamp;

        public CacheEntry(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public V getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.getTimestamp() < ttlMs) {
                return entry.getValue();
            }
        }
        return null;
    }

    public int size() {
        return cache.size();
    }
}
```

## Review

`ConcurrentHashMap` plus an immutable `CacheEntry` (both fields `final`) makes the code thread-safe
— no torn reads or corruption. The real problems are about memory and behaviour under load, roughly
in order of how much they'd hurt:

1. **Expired entries are never removed — memory leak.**
   `get()` returns `null` for stale entries but never deletes them, and `put()` only overwrites the
   same key. Any key written once and not re-read stays forever, so the map grows unbounded and
   eventually causes long GC pauses and an `OutOfMemoryError`. For a cache, this is the headline bug.

2. **No maximum size and no eviction policy.**
   Nothing caps the cache. It needs a size bound plus a strategy like LRU to drop the least-useful
   entries once full.

3. **A hot key expiring causes a stampede (thundering herd).**
   When a popular key hits the TTL, every reader gets `null` at the same instant and they all reload
   from the backend together — a spike of duplicate work that can take the backend down. There's no
   atomic "get-or-load" (e.g. `computeIfAbsent` with a loader) to collapse those into one load.

4. **`System.currentTimeMillis()` is wall-clock, not monotonic.**
   Clock jumps (NTP, DST, leap seconds) can make entries expire early or live too long. Use
   `System.nanoTime()` to measure elapsed time.

5. **`size()` is misleading and not free.**
   It counts expired-but-not-removed entries, so it overreports live items; and
   `ConcurrentHashMap.size()` is only an estimate under concurrent writes — avoid it on hot paths.

6. **No lazy cleanup on read.**
   Cheap win until there's real eviction: when `get()` finds an expired entry, remove it with the
   two-arg `cache.remove(key, entry)` so you don't drop a fresher value another thread just wrote.

7. **TTL is hardcoded and global.**
   `ttlMs` is a fixed 60s constant — make it a constructor parameter, ideally allowing per-entry TTL.

8. **Returning `null` is ambiguous.**
   It means "missing", "expired", or "stored null" all at once. Disallow null values or return
   `Optional<V>`.

9. **Minor: `CacheEntry<V>` shadows the outer `V`.**
   The nested class redeclares `V`, hiding `SimpleCache`'s — confusing to read. Rename it to
   `CacheEntry<T>`.

**Bottom line:** thread-safety is fine, but this isn't yet a production cache — it never frees
memory. The must-fixes are bounded size + eviction (1–2) and stampede protection (3). For a
high-throughput service, a proven library like **Caffeine** gives all of this out of the box.

