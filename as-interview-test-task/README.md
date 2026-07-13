# Employee JSON Converter — Home Assignment

Thank you for taking the time to work on this assignment. The goal isn't to catch you out — it's to
give us a concrete piece of code to discuss together afterwards. We care far more about **how you
reason** about an unfamiliar codebase, the trade-offs you weigh, and the quality bar you hold than
about how many boxes you tick.

## The project

This is a small Spring Boot service (`json-converter`). Its job: load employee data from several
**data sources**, each of which returns JSON in a **different shape**, normalize every record into a
single common `Employee` model, and combine the results.

The moving parts:

- **`DataSourceDAO`** implementations — each represents one external source and returns a raw JSON
  string. Different sources use different JSON structures (a flat array, a keyed dictionary, a nested
  object…).
- **`JsonEmployeeMapService`** implementations — each knows how to parse one structure into `Employee`s.
- **`MapRouterService`** — inspects an incoming JSON payload and routes it to the right mapper.
- **`EmployeeDataLoadService`** — pulls from all sources and aggregates the normalized employees.

Take some time to read the code and the tests until you're comfortable with how it fits together.

## Getting started

Requirements: **JDK 21** and the bundled Maven wrapper (no global Maven needed).

```bash
# Run the test suite
./mvnw test

# Run the application
./mvnw spring-boot:run
```

## What we'd like you to do

### Part 1 — Refactor & harden

Bring this code up to the standard you'd expect of production code you own. We've intentionally left
it rough in places. We won't list what to change — finding that is part of the exercise — but in
general we care about **correctness, reliability, readability, and maintainability**.

> A good first step is to build the project and run the tests, and treat what you find as your starting point.

### Part 2 — Add a new data source structure

A new source needs to be integrated. It returns this structure:

```json
{
  "results": [
    {
      "employeeNumber": "W-2001",
      "personal": { "fullName": "Grace Hopper" },
      "work": { "emailAddress": "ghopper@navy.mil", "position": "Rear Admiral" },
      "employmentStatus": "ACTIVE"
    },
    {
      "employeeNumber": "W-2002",
      "personal": { "fullName": "Katherine Johnson" },
      "work": { "emailAddress": "kjohnson@navy.mil", "position": "Mathematician" },
      "employmentStatus": "TERMINATED"
    }
  ]
}
```

Mapping rules:

| Source field          | `Employee` field | Notes                                   |
|-----------------------|------------------|-----------------------------------------|
| `employeeNumber`      | `id`             |                                         |
| `personal.fullName`   | `fullName`       |                                         |
| `work.emailAddress`   | `email`          |                                         |
| `work.position`       | `role`           |                                         |
| `employmentStatus`    | `active`         | `ACTIVE` ⇒ `true`; anything else ⇒ `false` |

Integrate this end to end so it flows through the existing pipeline alongside the current sources,
the way a real new source would. How easy or hard this is should tell you something about Part 1.

## Ground rules

- **Java 21**, Maven, keep the project building and the test suite green.
- Add or change tests as you see fit — we value tests.
- You may restructure code, rename things, and adjust interfaces if you can justify it. Avoid pulling
  in heavy new dependencies without a good reason.
- **Time-box it to roughly 3–5 hours.** This is deliberately more than you may finish perfectly — we'd
  rather see a few things done well, with clear notes on the rest, than everything done hastily.

## What to submit

1. Your code — as a fork/branch with a pull request, or a zipped repository.
2. A short **`NOTES.md`** containing:
   - the issues you found and how you addressed them (or would, with more time);
   - the key decisions and trade-offs you made, and why;
   - anything you deliberately left out and what you'd do next.

The `NOTES.md` matters as much as the code — it's the basis for our follow-up conversation, where we'll
ask you to walk us through your reasoning.

## A note on AI tools

Use whatever tools you'd normally use, AI assistants included. The only thing we ask is that you fully
understand and can **defend every decision and every line** in the follow-up interview — we'll go deep.
