import React, { useState } from 'react'

interface ImageGalleryProps {
  images: string[]
  alt: string
  badges?: { label: string; color: string }[]
}

const ImageGallery: React.FC<ImageGalleryProps> = ({ images, alt, badges }) => {
  const [activeIndex, setActiveIndex] = useState(0)
  const [imgErrors, setImgErrors] = useState<Set<number>>(new Set())

  const handleImgError = (index: number) => {
    setImgErrors(prev => new Set(prev).add(index))
  }

  const hasImages = images.length > 0
  const currentImage = hasImages && !imgErrors.has(activeIndex) ? images[activeIndex] : null

  const PlaceholderSvg = ({ className }: { className: string }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
        d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  )

  return (
    <div className="space-y-3">
      {/* Main Image */}
      <div className="relative aspect-[4/3] bg-bg-subtle rounded-2xl overflow-hidden group">
        {currentImage ? (
          <img
            src={currentImage}
            alt={alt}
            className="w-full h-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
            onError={() => handleImgError(activeIndex)}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-text-placeholder">
            <PlaceholderSvg className="w-24 h-24" />
          </div>
        )}

        {/* Badges */}
        {badges && badges.length > 0 && (
          <div className="absolute top-4 left-4 flex flex-col gap-2">
            {badges.map((badge, i) => (
              <span
                key={i}
                className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${badge.color}`}
              >
                {badge.label}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Thumbnails */}
      {images.length > 1 && (
        <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide">
          {images.map((img, i) => (
            <button
              key={i}
              onClick={() => setActiveIndex(i)}
              className={`flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden border-2 transition-all ${
                i === activeIndex
                  ? 'border-brand-primary ring-1 ring-brand-primary/30'
                  : 'border-transparent hover:border-border-hover'
              }`}
            >
              {!imgErrors.has(i) ? (
                <img
                  src={img}
                  alt={`${alt} ${i + 1}`}
                  className="w-full h-full object-cover"
                  onError={() => handleImgError(i)}
                />
              ) : (
                <div className="w-full h-full bg-bg-subtle flex items-center justify-center text-text-placeholder">
                  <PlaceholderSvg className="w-6 h-6" />
                </div>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default ImageGallery
