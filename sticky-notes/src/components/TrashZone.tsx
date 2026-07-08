import { useInteraction } from '../interaction/InteractionContext';
import { useOverTrash } from '../store/uiStore';
import styles from './TrashZone.module.css';

export function TrashZone() {
  const { trashRef } = useInteraction();
  const armed = useOverTrash();

  return (
    <div ref={trashRef} className={styles.trash} data-armed={armed ? 'true' : undefined}>
      <svg
        className={styles.icon}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M3 6h18" />
        <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        <path d="M6 6v14a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V6" />
        <path d="M10 11v6" />
        <path d="M14 11v6" />
      </svg>
      <span>{armed ? 'Release to delete' : 'Drop here to delete'}</span>
    </div>
  );
}
