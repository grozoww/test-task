import { useCallback, useRef } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import type { Point } from '../domain/types';

export interface DragState {
  start: Point;
  current: Point;
  delta: Point;
}

export interface DragHandlers {
  onStart?: (event: PointerEvent, state: DragState) => void;
  onMove?: (state: DragState, event: PointerEvent) => void;
  onEnd?: (state: DragState, event: PointerEvent) => void;
}

export function usePointerDrag(
  handlers: DragHandlers,
): (event: ReactPointerEvent<HTMLElement>) => void {
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  return useCallback((event: ReactPointerEvent<HTMLElement>) => {
    if (event.button !== 0) return;

    const target = event.currentTarget;
    target.setPointerCapture(event.pointerId);

    const start: Point = { x: event.clientX, y: event.clientY };
    let state: DragState = { start, current: start, delta: { x: 0, y: 0 } };
    handlersRef.current.onStart?.(event.nativeEvent, state);

    const handleMove = (moveEvent: PointerEvent) => {
      const current: Point = { x: moveEvent.clientX, y: moveEvent.clientY };
      state = { start, current, delta: { x: current.x - start.x, y: current.y - start.y } };
      handlersRef.current.onMove?.(state, moveEvent);
    };

    const finish = (endEvent: PointerEvent) => {
      target.removeEventListener('pointermove', handleMove);
      target.removeEventListener('pointerup', finish);
      target.removeEventListener('pointercancel', finish);
      if (target.hasPointerCapture(endEvent.pointerId)) {
        target.releasePointerCapture(endEvent.pointerId);
      }
      handlersRef.current.onEnd?.(state, endEvent);
    };

    target.addEventListener('pointermove', handleMove);
    target.addEventListener('pointerup', finish);
    target.addEventListener('pointercancel', finish);
  }, []);
}
