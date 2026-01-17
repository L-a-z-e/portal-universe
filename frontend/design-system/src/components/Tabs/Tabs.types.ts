/**
 * Tab item
 */
export interface TabItem {
  /**
   * Tab label
   */
  label: string;

  /**
   * Tab value (unique identifier)
   */
  value: string;

  /**
   * Disabled state
   */
  disabled?: boolean;

  /**
   * Icon name or component
   */
  icon?: string;
}

/**
 * Tabs component props
 */
export interface TabsProps {
  /**
   * Active tab value (v-model)
   */
  modelValue: string;

  /**
   * Tab items
   */
  items: TabItem[];

  /**
   * Visual variant
   * @default 'default'
   */
  variant?: 'default' | 'pills' | 'underline';

  /**
   * Size
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Full width tabs
   * @default false
   */
  fullWidth?: boolean;
}

/**
 * Tabs component emits
 */
export interface TabsEmits {
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}
