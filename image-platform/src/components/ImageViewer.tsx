import { useState, FC } from 'react';
import ImageCarousel from './ImageCarousel';
import { useImageStream } from '../hooks/useImageStream';

type ViewMode = 'single' | 'multi';
type CycleSpeed = 'slow' | 'normal' | 'fast';

const MIN_FRAMES = 1;
const MAX_FRAMES = 10;
const DEFAULT_FRAMES = 4;

const CYCLE_INTERVALS: Record<CycleSpeed, number> = {
  slow: 5000,    // 5 seconds
  normal: 3000,  // 3 seconds
  fast: 1000     // 1 second
};

export const ImageViewer: FC = () => {
  const [viewMode, setViewMode] = useState<ViewMode>('single');
  const [frameCount, setFrameCount] = useState(DEFAULT_FRAMES);
  const [cycleSpeed, setCycleSpeed] = useState<CycleSpeed>('normal');
  const { images, error, isLoading, isConnected } = useImageStream();

  const handleAddFrame = () => {
    if (frameCount < MAX_FRAMES) {
      setFrameCount(prev => prev + 1);
    }
  };

  const handleRemoveFrame = () => {
    if (frameCount > MIN_FRAMES) {
      setFrameCount(prev => prev - 1);
    }
  };

  const handleSpeedChange = (speed: CycleSpeed) => {
    setCycleSpeed(speed);
  };

  const getConnectionStatusText = () => {
    if (error) return 'Disconnected';
    if (!isConnected) return 'Connecting...';
    return 'Connected';
  };

  const getConnectionStatusColor = () => {
    if (error) return 'text-red-500';
    if (!isConnected) return 'text-yellow-500';
    return 'text-emerald-500';
  };

  const renderConnectionStatus = () => (
    <div className="flex items-center justify-center gap-2 mb-4">
      <div 
        data-testid="connection-status"
        className={`w-2 h-2 rounded-full ${error ? 'bg-red-500' : !isConnected ? 'bg-yellow-500' : 'bg-emerald-500'}`} 
      />
      <span className={`text-sm ${getConnectionStatusColor()}`}>
        {getConnectionStatusText()}
      </span>
    </div>
  );

  const renderFrameControls = () => (
    <div className="flex flex-col items-center gap-4">
      <div className="flex gap-4">
        <button
          onClick={handleRemoveFrame}
          disabled={frameCount <= MIN_FRAMES || (!isConnected && !error)}
          className={`px-4 py-1.5 rounded-lg font-medium transition-colors ${
            frameCount <= MIN_FRAMES || (!isConnected && !error)
              ? 'bg-gray-700 text-gray-400 cursor-not-allowed'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          Remove Frame
        </button>
        <button
          onClick={handleAddFrame}
          disabled={frameCount >= MAX_FRAMES || (!isConnected && !error)}
          className={`px-4 py-1.5 rounded-lg font-medium transition-colors ${
            frameCount >= MAX_FRAMES || (!isConnected && !error)
              ? 'bg-gray-700 text-gray-400 cursor-not-allowed'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          Add Frame
        </button>
      </div>
      <div className="flex items-center gap-6 bg-gray-800/50 rounded-lg px-6 py-3">
        <span className="text-sm text-gray-300">Cycle Speed:</span>
        <div className="flex gap-2">
          {(['slow', 'normal', 'fast'] as CycleSpeed[]).map((speed) => (
            <button
              key={speed}
              onClick={() => handleSpeedChange(speed)}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${
                cycleSpeed === speed
                  ? 'bg-[#7F5AF0] text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              {speed.charAt(0).toUpperCase() + speed.slice(1)}
            </button>
          ))}
        </div>
      </div>
    </div>
  );

  const renderModeSelector = () => (
    <div className="flex flex-col items-center gap-4 mb-8">
      {renderConnectionStatus()}
      <div className="flex justify-center gap-4">
        <button
          onClick={() => setViewMode('single')}
          disabled={!isConnected}
          className={`px-6 py-2 rounded-lg font-medium transition-colors ${
            !isConnected
              ? 'bg-gray-700 text-gray-400 cursor-not-allowed'
              : viewMode === 'single'
              ? 'bg-[#7F5AF0] text-white'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          Single Frame
        </button>
        <button
          onClick={() => setViewMode('multi')}
          disabled={!isConnected}
          className={`px-6 py-2 rounded-lg font-medium transition-colors ${
            !isConnected
              ? 'bg-gray-700 text-gray-400 cursor-not-allowed'
              : viewMode === 'multi'
              ? 'bg-[#7F5AF0] text-white'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          Multi-Frame
        </button>
      </div>
      
      {viewMode === 'multi' && renderFrameControls()}
    </div>
  );

  const renderSingleFrame = () => (
    <div className="max-w-5xl mx-auto px-4">
      <ImageCarousel
        images={images}
        error={error}
        isLoading={isLoading}
        isConnected={isConnected}
        cycleInterval={CYCLE_INTERVALS[cycleSpeed]}
      />
    </div>
  );

  const renderMultiFrame = () => {
    // Generate frames with distributed offsets
    const frames = Array.from({ length: frameCount }, (_, index) => (
      <div key={`frame-${index}`} className="aspect-video">
        <ImageCarousel
          images={images}
          error={error}
          isLoading={isLoading}
          isConnected={isConnected}
          startIndex={index}
          cycleInterval={CYCLE_INTERVALS[cycleSpeed]}
          compact
        />
      </div>
    ));

    // Determine grid columns based on frame count
    const gridCols = frameCount <= 4 ? 'md:grid-cols-2' : 'md:grid-cols-3';

    return (
      <div className="container mx-auto px-4 pb-8">
        <div className={`grid grid-cols-1 ${gridCols} gap-6`}>
          {frames}
        </div>
      </div>
    );
  };

  return (
    <div className="py-8">
      {renderModeSelector()}
      {viewMode === 'single' ? renderSingleFrame() : renderMultiFrame()}
    </div>
  );
};

export default ImageViewer; 
