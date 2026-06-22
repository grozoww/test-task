import { createContext, useContext, useMemo, useRef } from 'react';
import type { ReactNode, RefObject } from 'react';
import type { Point } from '../domain/types';

interface InteractionValue {
  boardRef: RefObject<HTMLDivElement>;
  trashRef: RefObject<HTMLDivElement>;
  toBoardPoint: (clientX: number, clientY: number) => Point;
  isOverTrash: (clientX: number, clientY: number) => boolean;
}

const InteractionContext = createContext<InteractionValue | null>(null);

export function InteractionProvider({ children }: { children: ReactNode }) {
  const boardRef = useRef<HTMLDivElement>(null);
  const trashRef = useRef<HTMLDivElement>(null);

  const value = useMemo<InteractionValue>(
    () => ({
      boardRef,
      trashRef,
      toBoardPoint: (clientX, clientY) => {
        const rect = boardRef.current?.getBoundingClientRect();
        if (!rect) return { x: clientX, y: clientY };
        return { x: clientX - rect.left, y: clientY - rect.top };
      },
      isOverTrash: (clientX, clientY) => {
        const rect = trashRef.current?.getBoundingClientRect();
        if (!rect) return false;
        return (
          clientX >= rect.left &&
          clientX <= rect.right &&
          clientY >= rect.top &&
          clientY <= rect.bottom
        );
      },
    }),
    [],
  );

  return <InteractionContext.Provider value={value}>{children}</InteractionContext.Provider>;
}

export function useInteraction(): InteractionValue {
  const value = useContext(InteractionContext);
  if (!value) throw new Error('useInteraction must be used within an InteractionProvider');
  return value;
}
