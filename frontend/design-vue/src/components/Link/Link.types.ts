import type { RouteLocationRaw } from 'vue-router';
import type { LinkProps as BaseLinkProps } from '@portal/design-core';

export interface LinkProps extends BaseLinkProps {
  to?: RouteLocationRaw;
}
