export const NOTE_COLORS = ['amber', 'rose', 'lime', 'sky', 'violet'] as const;

export type NoteColor = (typeof NOTE_COLORS)[number];

export const COLOR_VALUES: Record<NoteColor, string> = {
  amber: '#ffd54a',
  rose: '#ff9aa2',
  lime: '#c3f06b',
  sky: '#9ad8ff',
  violet: '#c9b0ff',
};
