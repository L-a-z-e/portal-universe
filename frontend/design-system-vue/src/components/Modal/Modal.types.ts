export interface ModalProps {
  /**
   * Modal 표시 여부
   */
  modelValue: boolean;

  /**
   * Modal 제목
   */
  title?: string;

  /**
   * Modal 크기
   */
  size?: 'sm' | 'md' | 'lg' | 'xl';

  /**
   * 닫기 버튼 표시 여부
   */
  showClose?: boolean;

  /**
   * 배경 클릭 시 닫기
   */
  closeOnBackdrop?: boolean;
}