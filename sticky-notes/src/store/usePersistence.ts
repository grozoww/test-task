import { useEffect } from 'react';
import { notesStore } from './notesStore';
import { saveNotes } from './persistence';

const DEBOUNCE_MS = 300;

export function usePersistence(enabled: boolean): void {
  useEffect(() => {
    if (!enabled) return;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const unsubscribe = notesStore.subscribe(() => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        const state = notesStore.getState();
        void saveNotes(state.ids.map((id) => state.byId[id]));
      }, DEBOUNCE_MS);
    });
    return () => {
      if (timer) clearTimeout(timer);
      unsubscribe();
    };
  }, [enabled]);
}
