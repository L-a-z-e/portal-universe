import React from 'react'
import { Button } from '@portal/design-react'

interface FilterChipsProps {
  categories: string[]
  active: string
  onChange: (category: string) => void
}

const FilterChips: React.FC<FilterChipsProps> = ({ categories, active, onChange }) => {
  const allCategories = ['All', ...categories]

  return (
    <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide pb-1">
      {allCategories.map((cat) => {
        const isActive = cat === 'All' ? !active : active === cat
        return (
          <Button
            key={cat}
            variant="outline"
            size="sm"
            onClick={() => onChange(cat === 'All' ? '' : cat)}
            className={`flex-shrink-0 rounded-full ${
              isActive
                ? 'bg-brand-primary/10 text-brand-primary ring-1 ring-brand-primary/50 border-brand-primary/50'
                : 'bg-bg-elevated text-text-meta hover:text-text-heading'
            }`}
          >
            {cat}
          </Button>
        )
      })}
    </div>
  )
}

export default FilterChips
