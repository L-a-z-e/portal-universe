/**
 * Admin Product List Page
 * 상품 목록 관리 페이지
 */
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminProducts, useDeleteProduct } from '@/hooks/useAdminProducts'
import { Button, Pagination, Modal, Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'
import type { ProductFilters } from '@/types'
import type { Product } from '@/types'

export const AdminProductListPage: React.FC = () => {
  const navigate = useNavigate()

  // 필터 상태
  const [filters, setFilters] = useState<ProductFilters>({
    page: 1,
    size: 10,
    keyword: '',
    sortBy: 'createdAt',
    sortOrder: 'desc'
  })

  // 삭제 모달 상태
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null)

  // 데이터 조회
  const { data, isLoading, error, refetch } = useAdminProducts(filters)
  const deleteMutation = useDeleteProduct()

  // 삭제 확인
  const handleDelete = async () => {
    if (!deleteTarget) return

    try {
      await deleteMutation.mutateAsync(deleteTarget.id)
      setDeleteTarget(null)
      // 삭제 후 목록 갱신
      refetch()
    } catch (error) {
      console.error('Failed to delete product:', error)
    }
  }

  // 페이지 변경
  const handlePageChange = (page: number) => {
    setFilters({ ...filters, page })
  }

  if (error) {
    return (
      <div className="bg-status-error-bg border border-status-error/20 rounded-lg p-8 text-center">
        <p className="text-status-error">Failed to load products</p>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Products</h1>
        <Button
          variant="primary"
          onClick={() => navigate('/admin/products/new')}
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Product
        </Button>
      </div>

      {/* Table */}
      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        <>
            <Table<Product>
              columns={[
                { key: 'id', label: 'ID' },
                { key: 'name', label: 'Name' },
                {
                  key: 'price',
                  label: 'Price',
                  render: (_, row) => `$${row.price.toLocaleString()}`,
                },
                {
                  key: 'category',
                  label: 'Category',
                  render: (_, row) => row.category || '-',
                },
                {
                  key: 'id' as keyof Product,
                  label: 'Actions',
                  align: 'right',
                  render: (_, row) => (
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e: React.MouseEvent) => {
                          e.stopPropagation()
                          navigate(`/admin/products/${row.id}`)
                        }}
                        className="p-2 text-brand-primary hover:bg-brand-primary/10"
                        title="Edit"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e: React.MouseEvent) => {
                          e.stopPropagation()
                          setDeleteTarget(row)
                        }}
                        className="p-2 text-status-error hover:bg-status-error-bg"
                        title="Delete"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </Button>
                    </div>
                  ),
                } as TableColumn<Product>,
              ]}
              data={data?.data.items ?? []}
              loading={isLoading}
              hoverable
              onRowClick={(row) => navigate(`/admin/products/${row.id}`)}
              emptyText="No products found"
            />

            {/* Pagination */}
            {data?.data.totalPages && data.data.totalPages > 1 && (
              <Pagination
                page={filters.page}
                totalPages={data.data.totalPages}
                onChange={(p: number) => handlePageChange(p)}
              />
            )}
        </>
      </div>

      {/* Delete Confirmation Modal */}
      <Modal
        open={!!deleteTarget}
        title="Delete Product?"
        onClose={() => setDeleteTarget(null)}
        size="sm"
      >
        <p className="mb-6">
          Are you sure you want to delete &quot;{deleteTarget?.name}&quot;? This action cannot be undone.
        </p>
        <div className="flex justify-end gap-3 pt-4 border-t border-border-default">
          <Button variant="ghost" onClick={() => setDeleteTarget(null)} disabled={deleteMutation.isPending}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDelete} loading={deleteMutation.isPending}>
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  )
}

export default AdminProductListPage
