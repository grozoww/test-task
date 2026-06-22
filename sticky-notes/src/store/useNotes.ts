import { useCallback, useSyncExternalStore } from 'react';
import type { Note, NoteId } from '../domain/types';
import { notesStore } from './notesStore';

export function useNoteIds(): NoteId[] {
  return useSyncExternalStore(notesStore.subscribe, () => notesStore.getState().ids);
}

export function useNoteCount(): number {
  return useSyncExternalStore(notesStore.subscribe, () => notesStore.getState().ids.length);
}

export function useNote(id: NoteId): Note | undefined {
  const getSnapshot = useCallback(() => notesStore.getState().byId[id], [id]);
  return useSyncExternalStore(notesStore.subscribe, getSnapshot);
}
