import type { Note, NoteId, Point, Size } from '../domain/types';
import type { NoteColor } from '../domain/colors';
import { createStore } from './store';

export interface NotesState {
  byId: Record<NoteId, Note>;
  ids: NoteId[];
  topZ: number;
}

const INITIAL: NotesState = { byId: {}, ids: [], topZ: 0 };

export const notesStore = createStore<NotesState>(INITIAL);

export interface CreateNoteInput {
  position: Point;
  size: Size;
  color: NoteColor;
}

export function createNote(input: CreateNoteInput): NoteId {
  const id = crypto.randomUUID();
  notesStore.setState((state) => {
    const z = state.topZ + 1;
    const note: Note = {
      id,
      position: input.position,
      size: input.size,
      color: input.color,
      text: '',
      z,
    };
    return { byId: { ...state.byId, [id]: note }, ids: [...state.ids, id], topZ: z };
  });
  return id;
}

function patch(id: NoteId, change: Partial<Note>): void {
  notesStore.setState((state) => {
    const current = state.byId[id];
    if (!current) return state;
    return { ...state, byId: { ...state.byId, [id]: { ...current, ...change } } };
  });
}

export const moveNote = (id: NoteId, position: Point): void => patch(id, { position });
export const resizeNote = (id: NoteId, size: Size): void => patch(id, { size });
export const setNoteText = (id: NoteId, text: string): void => patch(id, { text });
export const setNoteColor = (id: NoteId, color: NoteColor): void => patch(id, { color });

export function bringToFront(id: NoteId): void {
  notesStore.setState((state) => {
    const current = state.byId[id];
    if (!current || current.z === state.topZ) return state;
    const z = state.topZ + 1;
    return { ...state, byId: { ...state.byId, [id]: { ...current, z } }, topZ: z };
  });
}

export function removeNote(id: NoteId): void {
  notesStore.setState((state) => {
    if (!state.byId[id]) return state;
    const byId = { ...state.byId };
    delete byId[id];
    return { ...state, byId, ids: state.ids.filter((existing) => existing !== id) };
  });
}

export function replaceAll(notes: Note[]): void {
  notesStore.setState(() => {
    const byId: Record<NoteId, Note> = {};
    const ids: NoteId[] = [];
    let topZ = 0;
    for (const note of notes) {
      byId[note.id] = note;
      ids.push(note.id);
      topZ = Math.max(topZ, note.z);
    }
    return { byId, ids, topZ };
  });
}
