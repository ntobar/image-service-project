import { useState, useEffect, FC } from 'react';
import { ImageMetadata } from '../services/api';

interface ImageCarouselProps {
  images: ImageMetadata[];
  error: string | null;
  isLoading: boolean;
  isConnected: boolean;
  startIndex?: number;
  compact?: boolean;
  cycleInterval?: number;
}

export const ImageCarousel: FC<ImageCarouselProps> = ({
  images,
  error,
  isLoading,
  isConnected,
  startIndex = 0,
  compact = false,
  cycleInterval = 3000
}) => {
  // Initialize currentIndex with startIndex
  const [currentIndex, setCurrentIndex] = useState(startIndex);

  // Update currentIndex when images array changes
  useEffect(() => {
    if (images.length === 0) {
      setCurrentIndex(0);
    } else {
      // When images change, maintain the offset pattern by using startIndex
      setCurrentIndex(startIndex % images.length);
    }
  }, [images.length, startIndex]);

  // Handle automatic image cycling while maintaining the offset
  useEffect(() => {
    if (images.length <= 1 || !isConnected) return;

    const interval = setInterval(() => {
      setCurrentIndex(current => {
        // Calculate next index while maintaining the same relative position
        const nextIndex = (current + 1) % images.length;
        return nextIndex;
      });
    }, cycleInterval);

    return () => clearInterval(interval);
  }, [images.length, isConnected, cycleInterval]);

  const containerClasses = compact
    ? "w-full h-full"
    : "w-full";

  const getConnectionStatusText = () => {
    if (error) return 'Disconnected';
    if (!isConnected) return 'Connecting...';
    return 'Connected';
  };

  const getConnectionStatusColor = () => {
    if (error) return 'text-red-300/80';
    if (!isConnected) return 'text-yellow-300/80';
    return 'text-emerald-300/80';
  };

  if (error) {
    return (
      <div className={containerClasses}>
        <div className="bg-red-900/20 border-l-4 border-red-500 p-4 rounded">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-red-400">{error}</p>
              <p className={`text-xs mt-1 ${getConnectionStatusColor()}`}>
                {getConnectionStatusText()}
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!isConnected) {
    return (
      <div className={containerClasses}>
        <div className="bg-yellow-900/20 border-l-4 border-yellow-500 p-4 rounded">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-400 animate-spin" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-yellow-400">Attempting to connect to server...</p>
              <p className={`text-xs mt-1 ${getConnectionStatusColor()}`}>
                {getConnectionStatusText()}
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className={containerClasses}>
        <div className="bg-gray-800/50 rounded-lg p-8 text-center h-full flex flex-col items-center justify-center">
          <div role="status" className="animate-spin rounded-full h-8 w-8 border-2 border-[#7F5AF0] mb-4"></div>
          <p className="text-gray-300">Loading images...</p>
        </div>
      </div>
    );
  }

  if (images.length === 0) {
    return (
      <div className={containerClasses}>
        <div className="bg-gray-800/30 rounded-lg p-8 text-center border-2 border-dashed border-gray-700 h-full flex flex-col items-center justify-center">
          <svg className="mx-auto h-12 w-12 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-300">No images available</h3>
          <p className="mt-1 text-sm text-gray-400">Wait for images to be added or check back later.</p>
        </div>
      </div>
    );
  }

  // Ensure currentIndex is valid
  const safeCurrentIndex = currentIndex % images.length;
  const currentImage = images[safeCurrentIndex];
  const imageUrl = `/api/images/${currentImage.id}`;

  return (
    <div className={containerClasses}>
      <div className="relative aspect-video bg-gray-800/30 rounded-lg overflow-hidden shadow-lg h-full">
        <img
          src={imageUrl}
          alt={currentImage.name}
          className="w-full h-full object-cover"
          onError={(e) => {
            e.currentTarget.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
            e.currentTarget.className = "w-full h-full object-contain p-8 text-gray-600";
          }}
        />
        <div className="absolute bottom-0 left-0 right-0 bg-black/70 backdrop-blur-sm text-white p-4">
          <h3 className={`${compact ? 'text-sm' : 'text-lg'} font-semibold`}>{currentImage.name}</h3>
          <p className={`${compact ? 'text-xs' : 'text-sm'} text-gray-300`}>
            Frame {safeCurrentIndex + 1} of {images.length}
          </p>
          {!compact && (
            <p className="text-xs text-gray-400">
              Captured at {new Date(currentImage.uploadedAt).toLocaleString()}
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default ImageCarousel; 