/**
 * Tests for the ImageViewer component
 * 
 * Verifies the main viewer functionality:
 * - Single/multi-frame mode switching
 * - Frame addition and removal with limits
 * - Cycle speed adjustments
 * - Connection status display
 * - Control state management
 */
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ImageViewer } from '../components/ImageViewer';
import { useImageStream } from '../hooks/useImageStream';
import type { ImageMetadata } from '../services/api';

/** Mock the useImageStream hook */
vi.mock('../hooks/useImageStream', () => ({
  useImageStream: vi.fn()
}));

/** Mock image data for testing viewer states */
const mockImages: ImageMetadata[] = [
  { id: '1', name: 'test1.jpg', size: 1024, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:00Z' },
  { id: '2', name: 'test2.jpg', size: 2048, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:01Z' },
  { id: '3', name: 'test3.jpg', size: 3072, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:02Z' },
];

describe('ImageViewer', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    // Default mock implementation
    (useImageStream as ReturnType<typeof vi.fn>).mockReturnValue({
      images: mockImages,
      error: null,
      isLoading: false,
      isConnected: true
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it('renders single frame mode by default', () => {
    render(<ImageViewer />);
    expect(screen.getByText('Single Frame')).toHaveClass('bg-[#7F5AF0]');
    expect(screen.queryByText('Add Frame')).not.toBeInTheDocument();
  });

  it('switches to multi-frame mode', async () => {
    render(<ImageViewer />);
    fireEvent.click(screen.getByText('Multi-Frame'));
    expect(screen.getByText('Multi-Frame')).toHaveClass('bg-[#7F5AF0]');
    expect(screen.getByText('Add Frame')).toBeInTheDocument();
    expect(screen.getByText('Remove Frame')).toBeInTheDocument();
  });

  it('handles frame addition and removal', () => {
    render(<ImageViewer />);
    fireEvent.click(screen.getByText('Multi-Frame'));

    // When - add frames
    const addButton = screen.getByText('Add Frame');
    fireEvent.click(addButton);
    fireEvent.click(addButton);

    // Then
    const images = screen.getAllByRole('img');
    expect(images).toHaveLength(6); // Default 4 + 2 added

    // When - remove frames
    const removeButton = screen.getByText('Remove Frame');
    fireEvent.click(removeButton);

    // Then
    expect(screen.getAllByRole('img')).toHaveLength(5);
  });

  it('respects frame limits', () => {
    render(<ImageViewer />);
    fireEvent.click(screen.getByText('Multi-Frame'));

    // When - try to remove below minimum
    const removeButton = screen.getByText('Remove Frame');
    for (let i = 0; i < 5; i++) {
      fireEvent.click(removeButton);
    }

    // Then
    expect(screen.getAllByRole('img')).toHaveLength(1); // Minimum 1 frame
    expect(removeButton).toBeDisabled();

    // When - try to add above maximum
    const addButton = screen.getByText('Add Frame');
    for (let i = 0; i < 15; i++) {
      fireEvent.click(addButton);
    }

    // Then
    expect(screen.getAllByRole('img')).toHaveLength(10); // Maximum 10 frames
    expect(addButton).toBeDisabled();
  });

  it('adjusts cycle speed', () => {
    render(<ImageViewer />);
    fireEvent.click(screen.getByText('Multi-Frame'));

    const speedButtons = screen.getAllByRole('button', { name: /Slow|Normal|Fast/i });
    const fastButton = speedButtons.find(button => button.textContent === 'Fast');
    const slowButton = speedButtons.find(button => button.textContent === 'Slow');

    if (fastButton) fireEvent.click(fastButton);
    expect(fastButton).toHaveClass('bg-[#7F5AF0]');

    if (slowButton) fireEvent.click(slowButton);
    expect(slowButton).toHaveClass('bg-[#7F5AF0]');
  });

  it('displays connection status correctly', () => {
    // Given - disconnected state
    (useImageStream as ReturnType<typeof vi.fn>).mockReturnValue({
      images: [],
      error: 'Connection lost',
      isLoading: false,
      isConnected: false
    });

    // When
    render(<ImageViewer />);

    // Then - check the status indicator
    const statusIndicator = screen.getByTestId('connection-status');
    expect(statusIndicator).toHaveClass('bg-red-500');
    
    // Find the status text in the connection indicator section specifically
    const statusText = screen.getByText('Disconnected', { 
      selector: 'span.text-sm' // This targets the status indicator text specifically
    });
    expect(statusText).toHaveClass('text-red-500');
  });

  it('handles loading state', () => {
    // Given
    (useImageStream as ReturnType<typeof vi.fn>).mockReturnValue({
      images: [],
      error: null,
      isLoading: true,
      isConnected: false
    });

    // When
    render(<ImageViewer />);

    // Then
    // Check for the loading spinner SVG
    const spinnerSvg = screen.getByText('Attempting to connect to server...').parentElement?.previousElementSibling?.querySelector('.animate-spin');
    expect(spinnerSvg).toBeTruthy();
    expect(spinnerSvg).toBeVisible();
    
    // Check for loading messages
    expect(screen.getByText('Attempting to connect to server...')).toBeInTheDocument();
    // Check for the status indicator text specifically
    expect(screen.getByText('Connecting...', { selector: 'span.text-sm' })).toBeInTheDocument();
  });

  it('disables controls when disconnected', () => {
    // Given - mock the useImageStream hook to return disconnected state
    (useImageStream as ReturnType<typeof vi.fn>).mockReturnValue({
      images: [],
      error: 'Connection lost',
      isLoading: false,
      isConnected: false
    });

    // When
    render(<ImageViewer />);

    // Then - get buttons by their text content
    const singleFrameButton = screen.getByRole('button', { name: /Single Frame/i });
    const multiFrameButton = screen.getByRole('button', { name: /Multi-Frame/i });

    // Check disabled state
    expect(singleFrameButton).toBeDisabled();
    expect(multiFrameButton).toBeDisabled();

    // Check disabled styling
    expect(singleFrameButton).toHaveClass('bg-gray-700', 'text-gray-400', 'cursor-not-allowed');
    expect(multiFrameButton).toHaveClass('bg-gray-700', 'text-gray-400', 'cursor-not-allowed');
  });

  it('maintains frame offsets in multi-frame mode', () => {
    render(<ImageViewer />);
    fireEvent.click(screen.getByText('Multi-Frame'));

    const images = screen.getAllByRole('img');
    images.forEach((image, index) => {
      const expectedImage = mockImages[index % mockImages.length];
      expect(image).toHaveAttribute('alt', expectedImage.name);
    });
  });
}); 
