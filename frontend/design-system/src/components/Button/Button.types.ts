export interface ButtonProps {
  /**
   * 버튼의 시각적 스타일을 결정합니다.
   * - `primary`: 주요 액션 버튼
   * - `secondary`: 보조 액션 버튼
   * - `outline`: 배경이 투명한 외곽선 버튼
   * @default 'primary'
   */
  variant?: 'primary' | 'secondary' | 'outline';

  /**
   * 버튼의 크기를 결정합니다.
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * 버튼을 비활성화 상태로 만듭니다.
   * @default false
   */
  disabled?: boolean;
}
