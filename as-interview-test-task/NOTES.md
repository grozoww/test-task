# NOTES

## Starting point

`./mvnw test` on the original code: **38 tests, 3 failures** - every `ConcurrentEmployeeDataLoadService`
test failed with an empty result. That failure is a symptom of the most serious bug in the codebase
(see below), so I treated the load pipeline as the center of the work and moved outward from there.

## Issues found and how I addressed them

### Correctness

1. **`getAllEmployees()` returned before any work finished.** The service fired
   `CompletableFuture.runAsync(...)` per source and returned immediately without joining the futures,
   so callers got an empty (or partially filled, racy) list. Rewrote the service around
   `ExecutorService.invokeAll(...)`, which blocks until all sources complete (or the timeout hits).

2. **Three of four DAOs returned `null` at runtime.** `DesignSourceDAO`, `EngineeringSourceDAO` and
   `SecuritySourceDAO` kept their payload in an uninitialized `static String DATA` that was only
   populated by `reset()`/`setData()` - which nothing ever called. In the running app only the legacy
   source produced data; the others threw inside the mapper and were silently swallowed (see 4).
   Nothing in main or test code used `reset()`/`setData()`, so I removed them from `DataSourceDAO`
   entirely: each DAO is now a stateless supplier of a `static final` payload. This deletes the whole
   class of shared-mutable-static bugs instead of patching it.

3. **The router accumulated state across calls.** `JsonEmployeeMapRouterService` stored the result in
   an instance field (`List result`) of a singleton bean: concurrent calls raced each other, and an
   unrecognized payload silently returned the *previous* call's employees. The router is now stateless;
   an unrecognized structure throws `UnsupportedJsonStructureException` instead of returning stale data.

4. **Failures were invisible.** The load service had an empty `catch (Exception e) {}`; mappers caught
   everything, printed to `System.err` and returned an empty list - so "source is broken" and "source
   is empty" were indistinguishable, and data quietly disappeared (that's exactly how bug #2 stayed
   hidden). New policy, in one place: mappers/router throw; the load service catches **per source**,
   logs a warning with the source name (SLF4J via Lombok), and continues with the remaining sources.
   One broken source no longer takes the aggregate down, but it is also no longer silent.

5. **Contradictory tests.** One load test expected duplicates to be kept
   (`containsExactlyInAnyOrder(..., thirdEmployee, fourthEmployee, ...)` with two value-equal
   employees), the other expected de-duplication (`shouldReturnUniqueEmployeesList`). Both failed
   against the actual implementation, so neither documented real behavior. I took the test *name*
   `shouldReturnUniqueEmployeesList` as the intended requirement: the aggregate de-duplicates
   value-identical `Employee` records (via `LinkedHashSet`, preserving source order). The other test
   was rewritten to cover pure aggregation with distinct employees.

6. **Mapper "invalid JSON" tests didn't test their inputs.** The `@ValueSource(strings = {"{", "{}", "["})`
   tests shadowed the parameter with a hard-coded `String json = "{"`, so two of three cases were never
   exercised. Superseded by the new routing/parse-error tests (invalid JSON now fails in the router,
   with real assertions on the exception).

### Reliability / hardening

7. **Unbounded waiting.** A real source that hangs would previously have hung the common pool forever
   (or, with the original bug, been ignored). Loading now runs on **virtual threads**
   (`Executors.newVirtualThreadPerTaskExecutor()` - the right tool for blocking I/O-shaped calls on
   Java 21, and it stops competing with `ForkJoinPool.commonPool()`) with a configurable overall
   timeout: `employees.load-timeout`, default `5s`. Sources that don't respond in time are cancelled,
   logged, and skipped. Covered by a test with a deliberately slow source.

8. **Payload logging.** Mappers printed entire payloads to stderr on failure - employee PII in logs.
   Error messages now describe the *structure* (top-level field names, node type), never field values.

### Design / maintainability

9. **Adding a source structure required editing the router.** The router hard-wired three concrete
   mappers via field injection and re-parsed the payload up to three times (`readTree` per branch).
   Now `JsonEmployeeMapService` declares `boolean supports(JsonNode)` + `List<Employee> map(JsonNode)`;
   the router parses **once** and picks the first supporting mapper from the injected
   `List<JsonEmployeeMapService>`. Part 2 validated this: the new structure needed zero router changes.
   (Contract: `supports` predicates must stay mutually exclusive - first match wins.)

10. **Interface honesty.** Mappers previously took a raw `String` and each re-parsed it. They now take
    the already-parsed `JsonNode`. Raw types (`List result`), field injection, and `@Autowired` on
    single constructors are gone.

11. **`ArrayEmployeeDTO`** mixed public mutable fields with Lombok getters/setters and used `int` for a
    boolean-ish flag. It's now an immutable `record`. Jackson 3 fails on `null` → primitive by default
    (stricter than Jackson 2, and it surfaced in tests), so the record uses boxed types with explicit
    semantics in the mapper: absent/`null` `is_active` ⇒ inactive, absent `user_id` ⇒ `null` id -
    matching the other mappers' "unknown ⇒ inactive" convention rather than a silent primitive `0`.

12. **`AppConfig` deleted.** Spring Boot 4 auto-configures a Jackson 3 `JsonMapper` (assignable to
    `ObjectMapper`); a hand-rolled `new ObjectMapper()` bean would shadow Boot's configured one and
    ignore any `spring.jackson.*` properties. Verified by the integration test.

13. **`pom.xml` cleanup.** Removed version pins for everything the Boot parent already manages
    (starters, Jackson, Lombok, Mockito, both plugins - Lombok's annotation-processor path now uses
    `${lombok.version}` from the parent) and deleted the empty `<licenses>/<developers>/<scm>`
    scaffolding. One less way to drift from the BOM on upgrades.

### Tests

- Mapper tests now parse once and call `map(JsonNode)`; each mapper has an explicit `supports(...)`
  truth table, plus targeted tests for the active-flag rules (they encode business decisions).
- Router tests are plain Mockito unit tests instead of booting the full Spring context, and now cover
  the two previously untested paths: *no mapper supports the payload* and *payload isn't JSON*.
- Load-service tests cover: aggregation across sources (with deterministic ordering),
  de-duplication, per-source failure isolation (unreachable source + unroutable payload),
  all-sources-failing, timeout, and the 100-source concurrency test (kept, minus the `println` noise).
- New **end-to-end integration test**: the real context loads, and the default data of all five
  sources flows through routing and mapping into the combined result. This single test would have
  caught bugs #1, #2 and #3.
- Test resources load from the classpath instead of `Path.of("src/test/resources/...")`, which breaks
  when the working directory isn't the module root.

Result: 74 tests, green. `./mvnw package` + running the jar verified separately.

## Part 2 - the `results` source

- `ResearchSourceDAO` returns the new payload; `ResultsJsonEmployeeMapService` supports
  `root.path("results").isArray()` and maps via a small `ResultsEmployeeDTO` record (field names match
  the JSON, so no annotations needed).
- **No existing class changed** - the router discovered the new mapper by injection, which was the
  point of the Part 1 restructuring.
- Mapping decisions: `employmentStatus` is compared **case-sensitively** against `"ACTIVE"` - the spec
  says "`ACTIVE` -> true; anything else -> false", so `"Active"`/`"active"` are "anything else". Missing
  `personal`/`work` objects yield `null` fields rather than an error, consistent with the other mappers.

## Key decisions and trade-offs

- **Skip-and-log vs. fail-fast for a broken source.** I chose partial results with warnings (an
  aggregate over independent systems shouldn't be hostage to its flakiest member). The trade-off is
  that callers can't distinguish "complete" from "partial" yet - see next steps.
- **De-duplication by full value equality**, not by id. It only removes byte-identical records, so it
  can't destroy information. Merging *conflicting* records for the same person (same id, different
  role across systems) is a business decision that needs an owner, not something to invent silently.
- **One overall timeout** (`invokeAll` semantics) rather than per-source deadlines: simpler, and it
  bounds the caller's total wait, which is usually the requirement. Per-source budgets are easy to add
  if sources get real SLAs.
- **Strictness moved to the edges.** Structure errors throw (visible per source in logs); missing
  *fields* inside a recognized structure map to `null`/inactive. That mirrors reality: absent data is
  normal, unrecognized shapes mean something changed upstream and must not be guessed at.
- **Kept** the `DAO`/`Service` naming, Lombok (`@Builder` on `Employee`, `@Slf4j`), and the `webmvc`
  starter, to keep the diff focused on behavior. Each is defensible to change; none felt like the
  highest-value use of the time box.

## Deliberately left out / what I'd do next

1. **Expose the result.** Nothing consumes `EmployeeDataLoadService` yet - a `GET /employees`
   controller (or a scheduled export) is the obvious next slice, and would define the API contract
   (ordering, pagination, error shape).
2. **Completeness metadata.** Return sources-succeeded/failed alongside the list (or at least metrics)
   so "partial result" is observable by callers, not only in logs.
3. **Real DAO implementations.** The in-memory constants stand in for HTTP clients; retries/backoff
   and per-source timeouts belong there once they exist.
4. **Schema validation per source** (fail loudly on contract drift instead of `null`-ing fields), if
   the business prefers strictness to tolerance.
