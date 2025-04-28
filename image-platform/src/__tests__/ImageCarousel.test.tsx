/**
 * Tests for the ImageCarousel component
 * 
 * Verifies the component's ability to:
 * - Handle loading, error, and empty states
 * - Display single and multiple images with metadata
 * - Cycle through images at specified intervals
 * - Adapt to connection status changes
 * - Render in both standard and compact modes
 */
import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ImageCarousel } from '../components/ImageCarousel';
import type { ImageMetadata } from '../services/api';

/** Mock image data for testing various carousel states */
const mockImages: ImageMetadata[] = [
  { 
    id: '1', 
    name: 'image1.jpg',
    size: 1024,
    mimeType: 'image/jpeg',
    uploadedAt: new Date().toISOString()
  },
  { 
    id: '2', 
    name: 'image2.jpg',
    size: 2048,
    mimeType: 'image/jpeg',
    uploadedAt: new Date().toISOString()
  },
  { 
    id: '3', 
    name: 'image3.jpg',
    size: 3072,
    mimeType: 'image/jpeg',
    uploadedAt: new Date().toISOString()
  }
];

describe('ImageCarousel', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('shows loading spinner when loading', () => {
    render(<ImageCarousel images={[]} isLoading={true} isConnected={true} error={null} />);
    expect(screen.getByText('Loading images...')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveClass('animate-spin');
  });

  it('shows error message when error occurs', () => {
    const error = 'Test error';
    render(<ImageCarousel images={[]} isLoading={false} isConnected={false} error={error} />);
    expect(screen.getByText('Disconnected')).toBeInTheDocument();
  });

  it('shows empty state when no images and not loading', () => {
    render(<ImageCarousel images={[]} isLoading={false} isConnected={true} error={null} />);
    expect(screen.getByText('No images available')).toBeInTheDocument();
  });

  it('cycles through images at specified interval when connected', async () => {
    const CYCLE_INTERVAL = 5000; // 5 seconds
    
    render(
      <ImageCarousel 
        images={mockImages} 
        isLoading={false} 
        isConnected={true} 
        error={null} 
        cycleInterval={CYCLE_INTERVAL}
      />
    );

    // Initial render should show first image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[0].id}`);
    expect(screen.getByText(`Captured at ${new Date(mockImages[0].uploadedAt).toLocaleString()}`)).toBeInTheDocument();

    // Advance timer by cycle interval
    await act(async () => {
      vi.advanceTimersByTime(CYCLE_INTERVAL);
    });

    // Should show second image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[1].id}`);
    expect(screen.getByText(`Captured at ${new Date(mockImages[1].uploadedAt).toLocaleString()}`)).toBeInTheDocument();

    // Advance timer again
    await act(async () => {
      vi.advanceTimersByTime(CYCLE_INTERVAL);
    });

    // Should show third image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[2].id}`);
    expect(screen.getByText(`Captured at ${new Date(mockImages[2].uploadedAt).toLocaleString()}`)).toBeInTheDocument();

    // Advance timer one more time to wrap around
    await act(async () => {
      vi.advanceTimersByTime(CYCLE_INTERVAL);
    });

    // Should be back to first image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[0].id}`);
    expect(screen.getByText(`Captured at ${new Date(mockImages[0].uploadedAt).toLocaleString()}`)).toBeInTheDocument();
  });

  it('stops cycling when connection is lost', async () => {
    const CYCLE_INTERVAL = 5000;
    const { rerender } = render(
      <ImageCarousel 
        images={mockImages} 
        isLoading={false} 
        isConnected={true}
        error={null} 
        cycleInterval={CYCLE_INTERVAL}
      />
    );

    // Initial render shows first image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[0].id}`);

    // Advance timer and verify cycling works
    await act(async () => {
      vi.advanceTimersByTime(CYCLE_INTERVAL);
    });
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[1].id}`);

    // Disconnect and verify connection status message
    rerender(
      <ImageCarousel 
        images={mockImages} 
        isLoading={false} 
        isConnected={false} 
        error={null} 
        cycleInterval={CYCLE_INTERVAL}
      />
    );

    // Should show connecting message
    expect(screen.getByText('Attempting to connect to server...')).toBeInTheDocument();
    expect(screen.getByText('Connecting...')).toBeInTheDocument();

    // Advance timer
    await act(async () => {
      vi.advanceTimersByTime(CYCLE_INTERVAL);
    });

    // Should still show connecting message
    expect(screen.getByText('Attempting to connect to server...')).toBeInTheDocument();
    expect(screen.getByText('Connecting...')).toBeInTheDocument();
  });

  it('maintains start index offset when cycling through images', async () => {
    render(<ImageCarousel images={mockImages} isLoading={false} isConnected={true} error={null} startIndex={1} />);

    // Should start with second image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[1].id}`);
    expect(screen.getByText(mockImages[1].name)).toBeInTheDocument();

    // Advance timer to trigger image cycle
    await act(async () => {
      vi.advanceTimersByTime(3000); // Default cycle interval
    });

    // Should show third image
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[2].id}`);
    expect(screen.getByText(mockImages[2].name)).toBeInTheDocument();
  });

  it('should render single image with metadata', () => {
    render(<ImageCarousel isLoading={false} images={[mockImages[0]]} isConnected={true} error={null} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', `/api/images/${mockImages[0].id}`);
    expect(screen.getByRole('img')).toHaveAttribute('alt', mockImages[0].name);
    expect(screen.getByText(mockImages[0].name)).toBeInTheDocument();
  });

  it('should render in compact mode', () => {
    render(<ImageCarousel isLoading={false} images={mockImages} isConnected={true} error={null} compact={true} />);
    const container = screen.getByRole('img').closest('div');
    expect(container?.parentElement).toHaveClass('w-full h-full');
  });
});
