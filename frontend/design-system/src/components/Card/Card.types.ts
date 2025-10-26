export interface CardProps {
  /**
   * 카드의 시각적 스타일을 결정합니다.
   * - `elevated`: 그림자가 있는 기본 스타일
   * - `outlined`: 외곽선이 있는 스타일
   * - `flat`: 배경색만 있는 평평한 스타일
   * @default 'elevated'
   */
  variant?: 'elevated' | 'outlined' | 'flat';

  /**
   * 마우스 호버 시 확대 효과 및 커서 변경 여부를 결정합니다.
   * @default false
   */
  hoverable?: boolean;

  /**
   * 카드 내부의 패딩(padding) 크기를 결정합니다.
   * @default 'md'
   */
  padding?: 'none' | 'sm' | 'md' | 'lg';
}
