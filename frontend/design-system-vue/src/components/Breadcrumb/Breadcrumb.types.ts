import type { RouteLocationRaw } from 'vue-router';
import type {
  BreadcrumbItem as BaseBreadcrumbItem,
  BreadcrumbProps as BaseBreadcrumbProps,
} from '@portal/design-types';

export interface BreadcrumbItem extends BaseBreadcrumbItem {
  to?: RouteLocationRaw;
}

export interface BreadcrumbProps extends Omit<BaseBreadcrumbProps, 'items'> {
  items: BreadcrumbItem[];
}
