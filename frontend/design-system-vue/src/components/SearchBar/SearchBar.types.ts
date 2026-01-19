export interface SearchBarProps {
  /**
   * 검색어 (v-model)
   */
  modelValue: string;

  /**
   * Placeholder 텍스트
   * @default "검색..."
   */
  placeholder?: string;

  /**
   * 검색 중 상태
   */
  loading?: boolean;

  /**
   * 비활성화 상태
   */
  disabled?: boolean;

  /**
   * 자동 포커스
   */
  autofocus?: boolean;
}