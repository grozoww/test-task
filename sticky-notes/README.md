# Sticky Notes

A single-page sticky-notes board built with React + TypeScript, no UI/component or
drag-and-drop libraries. Everything — the store, the drag gestures, the components — is
hand-written.

## Features

Required (all four implemented):

- **Create** — drag on an empty area of the board to rubber-band a new note at that position and
  size. There is also a *+ Add note* button for a default-sized note.
- **Move** — drag a note by its header bar.
- **Resize** — drag the bottom-right corner handle.
- **Delete** — drag a note onto the trash zone (bottom-right); it arms while you hover and removes
  the note on release.

Bonus features included:

- Editing note text (type directly into a note).
- Bring-to-front on interaction (handles overlapping notes).
- Several note colours, selectable for new notes and per existing note.
- Persistence through an **asynchronous** storage layer (Promise-based, backed by `localStorage`);
  notes are restored on load.

## Architecture

The app is split into a framework-agnostic **domain/store core** and a thin **React view layer**.
The domain layer (`src/domain`) holds plain types (`Note`, `Point`, `Size`, colours) and pure
helpers. State lives in a tiny observable store (`src/store/store.ts`, ~20 lines): immutable
state, a `setState` reducer, and a subscriber set. `notesStore` builds the note actions
(`createNote`, `moveNote`, `resizeNote`, `removeNote`, `bringToFront`, …) on top of it, and React
reads from it through `useSyncExternalStore` selectors (`useNote`, `useNoteIds`, `useNoteCount`).
Because each `NoteView` subscribes to *its own* note object, editing or moving one note re-renders
only that note — not the whole board.

Pointer interaction is isolated in a single `usePointerDrag` hook that wraps the Pointer Events
API (capture + `pointermove`/`pointerup`), so every gesture — create, move, resize — is expressed
as the same `onStart`/`onMove`/`onEnd` contract. To keep dragging smooth, the *in-flight* gesture
is kept as transient local state on the dragged note (applied as a CSS `transform` for moves and a
live width/height for resizes) and is **committed to the store only on drop**. This means a drag
produces no global state churn and no re-render of other notes. The trash interaction uses a
separate one-field UI store so that arming the trash highlight re-renders only the trash zone, and
hit-testing is plain geometry against the trash element's bounding rect. An `InteractionContext`
exposes stable refs/helpers (board coordinate conversion, trash hit-test) without ever changing
identity, so it never causes re-renders itself.

Persistence is deliberately behind an async, Promise-based boundary (`src/store/persistence.ts`) to
mimic a REST API: loads are awaited on startup, and writes are debounced and fired in the
background as the store changes. Swapping `localStorage` for a real `fetch`-based backend would
touch only that one file.

## Requirements covered

- TypeScript throughout, `strict` mode, `verbatimModuleSyntax`, no `any`.
- React without any stock/third-party components.
- Targets desktop (≥ 1024×768), latest Chrome / Firefox / Edge (Pointer Events are supported
  everywhere there).

## Running

```bash
npm install
npm run dev        # start the dev server (Vite prints the local URL)
npm run build      # type-check (tsc --noEmit) + production build into dist/
npm run preview    # serve the production build locally
```

Requires Node 18+.
