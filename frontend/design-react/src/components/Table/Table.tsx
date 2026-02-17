import { forwardRef, type TableHTMLAttributes, type ReactNode } from 'react';
import type { TableProps, TableColumn } from '@portal/design-core';
import { cn } from '@portal/design-core';
import { Spinner } from '../Spinner';

export interface TableComponentProps<T = unknown>
  extends TableProps<T>,
    Omit<TableHTMLAttributes<HTMLTableElement>, 'children'> {}

export const Table = forwardRef<HTMLTableElement, TableComponentProps>(
  (
    {
      columns,
      data,
      loading = false,
      emptyText = 'No data available',
      striped = false,
      hoverable = true,
      onRowClick,
      className,
      ...props
    },
    ref
  ) => {
    const getAlignClass = (align?: 'left' | 'center' | 'right') => {
      switch (align) {
        case 'center':
          return 'text-center';
        case 'right':
          return 'text-right';
        default:
          return 'text-left';
      }
    };

    return (
      <div className="relative w-full overflow-auto">
        <table
          ref={ref}
          className={cn('w-full border-collapse', className)}
          {...props}
        >
          <thead>
            <tr className="border-b border-border-default bg-bg-muted">
              {columns.map((column: TableColumn) => (
                <th
                  key={column.key}
                  className={cn(
                    'px-4 py-3 text-sm font-semibold text-text-heading',
                    getAlignClass(column.align)
                  )}
                  style={{ width: column.width }}
                >
                  {column.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-8 text-center">
                  <Spinner size="md" />
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-8 text-center text-text-muted"
                >
                  {emptyText}
                </td>
              </tr>
            ) : (
              data.map((row, rowIndex) => (
                <tr
                  key={rowIndex}
                  className={cn(
                    'border-b border-border-default transition-colors',
                    striped && rowIndex % 2 === 1 && 'bg-bg-muted/50',
                    hoverable && 'hover:bg-bg-hover',
                    onRowClick && 'cursor-pointer'
                  )}
                  onClick={() => onRowClick?.(row, rowIndex)}
                >
                  {columns.map((column: TableColumn) => {
                    const value = (row as Record<string, unknown>)[column.key];
                    const rendered = column.render
                      ? (column.render(value, row) as ReactNode)
                      : (value as ReactNode);

                    return (
                      <td
                        key={column.key}
                        className={cn(
                          'px-4 py-3 text-sm text-text-body',
                          getAlignClass(column.align)
                        )}
                      >
                        {rendered}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    );
  }
) as <T>(props: TableComponentProps<T> & { ref?: React.Ref<HTMLTableElement> }) => React.ReactElement;

(Table as { displayName?: string }).displayName = 'Table';

export default Table;
