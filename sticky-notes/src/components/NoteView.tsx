import { useState } from 'react';
import type { CSSProperties, PointerEvent as ReactPointerEvent } from 'react';
import type { NoteId, Point, Size } from '../domain/types';
import { NOTE_COLORS, COLOR_VALUES } from '../domain/colors';
import { MIN_NOTE_SIZE } from '../domain/constants';
import { clamp } from '../domain/geometry';
import {
  bringToFront,
  moveNote,
  removeNote,
  resizeNote,
  setNoteColor,
  setNoteText,
} from '../store/notesStore';
import { setOverTrash } from '../store/uiStore';
import { useNote } from '../store/useNotes';
import { usePointerDrag } from '../hooks/usePointerDrag';
import { useInteraction } from '../interaction/InteractionContext';
import styles from './NoteView.module.css';

interface NoteViewProps {
  id: NoteId;
}

export function NoteView({ id }: NoteViewProps) {
  const note = useNote(id);
  const { boardRef, isOverTrash } = useInteraction();
  const [moveDelta, setMoveDelta] = useState<Point | null>(null);
  const [sizeDelta, setSizeDelta] = useState<Size | null>(null);

  const startMove = usePointerDrag({
    onStart: () => bringToFront(id),
    onMove: (state) => {
      setMoveDelta(state.delta);
      setOverTrash(isOverTrash(state.current.x, state.current.y));
    },
    onEnd: (state) => {
      setOverTrash(false);
      if (isOverTrash(state.current.x, state.current.y)) {
        removeNote(id);
        return;
      }
      setMoveDelta(null);
      if (!note) return;
      const rect = boardRef.current?.getBoundingClientRect();
      const maxX = rect ? Math.max(0, rect.width - note.size.width) : Number.MAX_SAFE_INTEGER;
      const maxY = rect ? Math.max(0, rect.height - note.size.height) : Number.MAX_SAFE_INTEGER;
      moveNote(id, {
        x: clamp(note.position.x + state.delta.x, 0, maxX),
        y: clamp(note.position.y + state.delta.y, 0, maxY),
      });
    },
  });

  const startResize = usePointerDrag({
    onStart: () => bringToFront(id),
    onMove: (state) => setSizeDelta({ width: state.delta.x, height: state.delta.y }),
    onEnd: (state) => {
      setSizeDelta(null);
      if (!note) return;
      const rect = boardRef.current?.getBoundingClientRect();
      const maxW = rect
        ? Math.max(MIN_NOTE_SIZE.width, rect.width - note.position.x)
        : Number.MAX_SAFE_INTEGER;
      const maxH = rect
        ? Math.max(MIN_NOTE_SIZE.height, rect.height - note.position.y)
        : Number.MAX_SAFE_INTEGER;
      resizeNote(id, {
        width: clamp(note.size.width + state.delta.x, MIN_NOTE_SIZE.width, maxW),
        height: clamp(note.size.height + state.delta.y, MIN_NOTE_SIZE.height, maxH),
      });
    },
  });

  if (!note) return null;

  const width = Math.max(MIN_NOTE_SIZE.width, note.size.width + (sizeDelta?.width ?? 0));
  const height = Math.max(MIN_NOTE_SIZE.height, note.size.height + (sizeDelta?.height ?? 0));

  const style: CSSProperties = {
    left: note.position.x,
    top: note.position.y,
    width,
    height,
    zIndex: note.z,
    background: COLOR_VALUES[note.color],
    transform: moveDelta ? `translate3d(${moveDelta.x}px, ${moveDelta.y}px, 0)` : undefined,
  };

  return (
    <div
      className={styles.note}
      style={style}
      data-dragging={moveDelta ? 'true' : undefined}
      onPointerDown={() => bringToFront(id)}
    >
      <div className={styles.header} onPointerDown={startMove}>
        <div className={styles.swatches}>
          {NOTE_COLORS.map((color) => (
            <button
              key={color}
              type="button"
              className={styles.swatch}
              style={{ background: COLOR_VALUES[color] }}
              data-active={color === note.color ? 'true' : undefined}
              aria-label={`Set color ${color}`}
              onPointerDown={(event: ReactPointerEvent) => event.stopPropagation()}
              onClick={() => setNoteColor(id, color)}
            />
          ))}
        </div>
      </div>
      <textarea
        className={styles.text}
        value={note.text}
        placeholder="Write something…"
        onChange={(event) => setNoteText(id, event.target.value)}
      />
      <div
        className={styles.resize}
        onPointerDown={startResize}
        role="separator"
        aria-label="Resize note"
      />
    </div>
  );
}
