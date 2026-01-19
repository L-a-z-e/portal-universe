/**
 * Dropdown menu item
 */
export interface DropdownItem {
  /**
   * Item label
   */
  label: string;

  /**
   * Item value (returned on select)
   */
  value?: string | number;

  /**
   * Icon name or component
   */
  icon?: string;

  /**
   * Disabled state
   */
  disabled?: boolean;

  /**
   * Render as divider instead of item
   */
  divider?: boolean;
}

/**
 * Dropdown component props
 */
export interface DropdownProps {
  /**
   * Menu items
   */
  items: DropdownItem[];

  /**
   * Trigger behavior
   * @default 'click'
   */
  trigger?: 'click' | 'hover';

  /**
   * Menu placement
   * @default 'bottom-start'
   */
  placement?: 'bottom' | 'bottom-start' | 'bottom-end' | 'top' | 'top-start' | 'top-end';

  /**
   * Disabled state
   * @default false
   */
  disabled?: boolean;

  /**
   * Close on item click
   * @default true
   */
  closeOnSelect?: boolean;

  /**
   * Custom width
   * @default 'auto'
   */
  width?: 'auto' | 'trigger' | string;
}

/**
 * Dropdown component emits
 */
export interface DropdownEmits {
  (e: 'select', item: DropdownItem): void;
  (e: 'open'): void;
  (e: 'close'): void;
}
