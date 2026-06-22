import type { NoteColor } from '../domain/colors';
import { NOTE_COLORS, COLOR_VALUES } from '../domain/colors';
import { DEFAULT_NOTE_SIZE } from '../domain/constants';
import { createNote } from '../store/notesStore';
import { useNoteCount } from '../store/useNotes';
import styles from './Toolbar.module.css';

interface ToolbarProps {
  newColor: NoteColor;
  onSelectColor: (color: NoteColor) => void;
}

export function Toolbar({ newColor, onSelectColor }: ToolbarProps) {
  const count = useNoteCount();

  const addNote = () => {
    const step = (count % 6) * 28;
    createNote({
      position: { x: 48 + step, y: 32 + step },
      size: DEFAULT_NOTE_SIZE,
      color: newColor,
    });
  };

  return (
    <header className={styles.toolbar}>
      <div className={styles.brand}>Sticky Notes</div>
      <div className={styles.palette}>
        <span className={styles.label}>New note</span>
        {NOTE_COLORS.map((color) => (
          <button
            key={color}
            type="button"
            className={styles.swatch}
            style={{ background: COLOR_VALUES[color] }}
            data-active={color === newColor ? 'true' : undefined}
            aria-label={`New note color ${color}`}
            onClick={() => onSelectColor(color)}
          />
        ))}
      </div>
      <button type="button" className={styles.add} onClick={addNote}>
        + Add note
      </button>
      <div className={styles.hint}>
        Drag on the board to draw a note · drag the header to move · pull the corner to resize · drop
        on the trash to delete
      </div>
      <div className={styles.count}>
        {count} {count === 1 ? 'note' : 'notes'}
      </div>
    </header>
  );
}
