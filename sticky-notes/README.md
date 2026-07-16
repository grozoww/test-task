# Sticky Notes

A sticky-notes board built with **React 18 + TypeScript + Vite** and **zero runtime dependencies** beyond React itself — no state-management, drag-and-drop, or UI libraries. State, drag mechanics, and persistence are implemented from scratch (~700 lines total).

## Features

- **Create notes** — drag on an empty area of the board to draw a note at that exact spot and size, or click **+ Add note** (new notes cascade so they don't stack exactly on top of each other). Drags smaller than 24px are treated as accidental clicks and ignored.
- **Move** — drag a note by its header. On drop the position is clamped to the board bounds.
- **Resize** — pull the handle in the bottom-right corner (minimum size 120×90, clamped to the board).
- **Edit text** — type directly into the note.
- **Colors** — five colors (amber, rose, lime, sky, violet). The toolbar palette sets the color for *new* notes; the swatches in a note's header recolor an *existing* note.
- **Z-order** — clicking or dragging a note brings it to the front.
- **Delete** — drag a note onto the trash zone in the corner; it arms ("Release to delete") while you hover over it.
- **Persistence** — the board survives reloads. Saves go to `localStorage` through a small async layer that simulates backend latency, debounced at 300 ms so rapid edits don't spam writes. On startup the app hydrates from storage before enabling saves, so an empty initial render can't overwrite existing data.

## Getting started

Requires Node 18+.

```bash
npm install
npm run dev        # start dev server
```

Other scripts:

```bash
npm run build      # typecheck + production build
npm run preview    # serve the production build locally
npm run typecheck  # tsc --noEmit only
```

## Project structure

```
src/
  domain/        # pure types & constants (Note, Point, Size, colors, clamp) — no React
  store/         # custom store, notes state, subscription hooks, persistence
  hooks/         # usePointerDrag — reusable pointer-drag primitive
  interaction/   # InteractionContext — shared board/trash refs, coordinate transforms
  components/    # Board, NoteView, Toolbar, TrashZone (+ CSS Modules)
  App.tsx        # composition + hydration from storage
  main.tsx       # entry point
```

## Architecture notes

**Custom store instead of Redux/Zustand.** [store.ts](src/store/store.ts) is a minimal pub/sub store (`getState` / `setState` / `subscribe`) — essentially the core of Zustand's API, so migrating to a library later would be trivial. Updates are immutable, and `setState` skips notifying listeners when a reducer returns the same state reference (e.g. `bringToFront` on a note that's already on top), avoiding useless re-renders.

**Normalized state.** Notes are stored as `{ byId, ids, topZ }` rather than an array. Components subscribe via `useSyncExternalStore`: the board subscribes only to the `ids` list, and each `NoteView` subscribes only to its own note, so editing one note never re-renders the others.

**Drag without store churn.** [usePointerDrag](src/hooks/usePointerDrag.ts) wraps the Pointer Events API with pointer capture, working uniformly for mouse and touch. While dragging, a note applies its delta as a local `translate3d` transform — the store is only committed once, on drop. The same primitive drives moving, resizing, and drawing new notes.

**Isolated interaction context.** [InteractionContext](src/interaction/InteractionContext.tsx) shares the board and trash-zone refs, converting client coordinates to board space and hit-testing the trash zone — no DOM queries or prop drilling in the components.
