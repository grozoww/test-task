import type { NoteColor } from './colors';

export type NoteId = string;

export interface Point {
  x: number;
  y: number;
}

export interface Size {
  width: number;
  height: number;
}

export interface Note {
  id: NoteId;
  position: Point;
  size: Size;
  color: NoteColor;
  text: string;
  z: number;
}
