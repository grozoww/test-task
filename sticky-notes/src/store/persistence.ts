import type { Note } from '../domain/types';

const STORAGE_KEY = 'sticky-notes/v1';
const LATENCY_MS = 120;

export function loadNotes(): Promise<Note[]> {
  return new Promise((resolve) => {
    setTimeout(() => {
      try {
        const raw = localStorage.getItem(STORAGE_KEY);
        resolve(raw ? (JSON.parse(raw) as Note[]) : []);
      } catch {
        resolve([]);
      }
    }, LATENCY_MS);
  });
}

export function saveNotes(notes: Note[]): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
      } catch {
        resolve();
        return;
      }
      resolve();
    }, LATENCY_MS);
  });
}
