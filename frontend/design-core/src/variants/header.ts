export const headerBase = [
  'h-16 flex items-center justify-between px-6',
  'bg-bg-card/80 backdrop-blur-md',
  'border-b border-border-default',
  'sticky top-0 z-30',
].join(' ');

export const headerSearch = [
  'flex items-center gap-2 px-3 py-1.5 rounded-lg',
  'text-sm text-text-meta',
  'bg-bg-muted border border-border-default',
  'hover:border-border-hover hover:text-text-body',
  'transition-colors duration-fast cursor-pointer',
].join(' ');

export const headerAvatar = [
  'w-8 h-8 rounded-full',
  'bg-gradient-to-br from-nightfall-300 to-nightfall-500',
  'flex items-center justify-center',
  'text-white text-xs font-semibold',
  'ring-2 ring-transparent hover:ring-nightfall-300/30',
  'transition-all duration-fast cursor-pointer',
].join(' ');
