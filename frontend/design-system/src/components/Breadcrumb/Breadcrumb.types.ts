import type { RouteLocationRaw } from 'vue-router';

/**
 * Breadcrumb item
 */
export interface BreadcrumbItem {
  /**
   * Item label
   */
  label: string;

  /**
   * URL for external links
   */
  href?: string;

  /**
   * Vue Router to prop
   */
  to?: RouteLocationRaw;

  /**
   * Icon name or component
   */
  icon?: string;
}

/**
 * Breadcrumb component props
 */
export interface BreadcrumbProps {
  /**
   * Breadcrumb items
   */
  items: BreadcrumbItem[];

  /**
   * Separator character
   * @default '/'
   */
  separator?: string;

  /**
   * Max items before collapse
   */
  maxItems?: number;

  /**
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';
}
