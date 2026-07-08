import { useEffect, useState } from 'react';
import type { NoteColor } from './domain/colors';
import { NOTE_COLORS } from './domain/colors';
import { Board } from './components/Board';
import { Toolbar } from './components/Toolbar';
import { TrashZone } from './components/TrashZone';
import { InteractionProvider } from './interaction/InteractionContext';
import { replaceAll } from './store/notesStore';
import { loadNotes } from './store/persistence';
import { usePersistence } from './store/usePersistence';

export function App() {
  const [newColor, setNewColor] = useState<NoteColor>(NOTE_COLORS[0]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    let active = true;
    void loadNotes().then((notes) => {
      if (!active) return;
      replaceAll(notes);
      setHydrated(true);
    });
    return () => {
      active = false;
    };
  }, []);

  usePersistence(hydrated);

  return (
    <InteractionProvider>
      <div className="app">
        <Toolbar newColor={newColor} onSelectColor={setNewColor} />
        <Board newColor={newColor} />
        <TrashZone />
      </div>
    </InteractionProvider>
  );
}
