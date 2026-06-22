import { useSyncExternalStore } from 'react';
import { createStore } from './store';

interface UiState {
  overTrash: boolean;
}

const uiStore = createStore<UiState>({ overTrash: false });

export function setOverTrash(overTrash: boolean): void {
  uiStore.setState((state) => (state.overTrash === overTrash ? state : { overTrash }));
}

export function useOverTrash(): boolean {
  return useSyncExternalStore(uiStore.subscribe, () => uiStore.getState().overTrash);
}
