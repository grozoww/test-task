import { useRef, useState } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import type { Point } from '../domain/types';
import type { NoteColor } from '../domain/colors';
import { COLOR_VALUES } from '../domain/colors';
import { MIN_DRAW_SIZE } from '../domain/constants';
import { createNote } from '../store/notesStore';
import { useNoteIds } from '../store/useNotes';
import { usePointerDrag } from '../hooks/usePointerDrag';
import { useInteraction } from '../interaction/InteractionContext';
import { NoteView } from './NoteView';
import styles from './Board.module.css';

interface Draft {
  origin: Point;
  current: Point;
}

interface BoardProps {
  newColor: NoteColor;
}

export function Board({ newColor }: BoardProps) {
  const ids = useNoteIds();
  const { boardRef, toBoardPoint } = useInteraction();
  const [draft, setDraft] = useState<Draft | null>(null);
  const originRef = useRef<Point | null>(null);

  const startDraw = usePointerDrag({
    onStart: (event) => {
      const point = toBoardPoint(event.clientX, event.clientY);
      originRef.current = point;
      setDraft({ origin: point, current: point });
    },
    onMove: (state) => {
      const origin = originRef.current;
      if (!origin) return;
      setDraft({ origin, current: toBoardPoint(state.current.x, state.current.y) });
    },
    onEnd: (state) => {
      const origin = originRef.current;
      originRef.current = null;
      setDraft(null);
      if (!origin) return;
      const end = toBoardPoint(state.current.x, state.current.y);
      const position = { x: Math.min(origin.x, end.x), y: Math.min(origin.y, end.y) };
      const size = { width: Math.abs(end.x - origin.x), height: Math.abs(end.y - origin.y) };
      if (size.width < MIN_DRAW_SIZE || size.height < MIN_DRAW_SIZE) return;
      createNote({ position, size, color: newColor });
    },
  });

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.target !== event.currentTarget) return;
    startDraw(event);
  };

  return (
    <div ref={boardRef} className={styles.board} onPointerDown={handlePointerDown}>
      {ids.map((id) => (
        <NoteView key={id} id={id} />
      ))}
      {draft ? <DraftRect draft={draft} color={newColor} /> : null}
      {ids.length === 0 && !draft ? (
        <div className={styles.empty}>Drag anywhere to create your first note</div>
      ) : null}
    </div>
  );
}

function DraftRect({ draft, color }: { draft: Draft; color: NoteColor }) {
  const left = Math.min(draft.origin.x, draft.current.x);
  const top = Math.min(draft.origin.y, draft.current.y);
  const width = Math.abs(draft.current.x - draft.origin.x);
  const height = Math.abs(draft.current.y - draft.origin.y);
  return (
    <div
      className={styles.draft}
      style={{ left, top, width, height, background: COLOR_VALUES[color] }}
    />
  );
}
