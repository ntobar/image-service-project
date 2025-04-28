/**
 * Tests for the useImageStream hook
 * 
 * Tests core functionality of the real-time image stream connection:
 * - Initial connection and image loading
 * - Connection health monitoring via heartbeats
 * - Resource cleanup
 */
import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { useImageStream } from '../hooks/useImageStream';
import type { ImageMetadata, ImageEvent } from '../services/api';

/** Mock image data representing a typical server response */
const mockImages: ImageMetadata[] = [
  { id: '1', name: 'test1.jpg', size: 1024, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:00Z' },
  { id: '2', name: 'test2.jpg', size: 2048, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:01Z' },
  { id: '3', name: 'test3.jpg', size: 3072, mimeType: 'image/jpeg', uploadedAt: '2024-01-01T00:00:02Z' },
];

/**
 * Mock EventSource implementation for testing SSE connections
 * Simulates server-sent events without requiring a real server
 */
class MockEventSource {
  public static instance: MockEventSource | null = null;
  private listeners: Record<string, Function[]> = {};
  public readyState: number = 0;
  public static CONNECTING = 0;
  public static OPEN = 1;
  public static CLOSED = 2;

  constructor(public url: string) {
    MockEventSource.instance = this;
    this.readyState = MockEventSource.CONNECTING;
    setTimeout(() => this.emitOpen(), 0);
  }

  addEventListener(type: string, listener: Function) {
    this.listeners[type] = this.listeners[type] || [];
    this.listeners[type].push(listener);
  }

  removeEventListener(type: string, listener: Function) {
    if (this.listeners[type]) {
      this.listeners[type] = this.listeners[type].filter(l => l !== listener);
    }
  }

  close() {
    this.readyState = MockEventSource.CLOSED;
    MockEventSource.instance = null;
  }

  emitMessage(data: ImageEvent) {
    if (this.listeners.message) {
      this.listeners.message.forEach(listener => 
        listener({ data: JSON.stringify(data) }));
    }
  }

  emitOpen() {
    this.readyState = MockEventSource.OPEN;
    if (this.listeners.open) {
      this.listeners.open.forEach(listener => listener());
    }
  }

  emitError() {
    this.readyState = MockEventSource.CLOSED;
    if (this.listeners.error) {
      this.listeners.error.forEach(listener => listener(new Event('error')));
    }
  }

  static getInstance() {
    return MockEventSource.instance;
  }
}

/**
 * MSW server setup
 */
const server = setupServer(
  http.get('/api/images', () => {
    return HttpResponse.json({ data: mockImages });
  })
);

describe('useImageStream', () => {
  // Start MSW server once before all tests
  beforeAll(() => {
    server.listen({ onUnhandledRequest: 'error' });
  });

  // Reset MSW handlers before each test
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.stubGlobal('EventSource', MockEventSource);
    server.resetHandlers();
  });

  // Clean up after each test
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    MockEventSource.instance = null;
    server.resetHandlers();
  });

  // Stop MSW server after all tests
  afterAll(() => {
    server.close();
  });

  it('establishes connection and loads initial images', async () => {
    // Given - a new hook instance
    const { result } = renderHook(() => useImageStream());

    // Then - initial state should be loading
    expect(result.current.isLoading).toBe(true);
    expect(result.current.images).toHaveLength(0);

    // When - EventSource connects and initial load completes
    await act(async () => {
      await vi.runAllTimersAsync();
    });

    // Then - state should update with images
    expect(result.current.isLoading).toBe(false);
    expect(result.current.isConnected).toBe(true);
    expect(result.current.error).toBeNull();
    expect(result.current.images).toHaveLength(mockImages.length);
    expect(result.current.images).toEqual(mockImages);
  });

  it('maintains connection with heartbeat events', async () => {
    // Given - a hook instance with initial images loaded
    const { result } = renderHook(() => useImageStream());
    
    await act(async () => {
      await vi.runAllTimersAsync();
    });

    // When - heartbeat event is received
    await act(async () => {
      const eventSource = MockEventSource.getInstance();
      if (eventSource) {
        eventSource.emitMessage({
          type: 'HEARTBEAT',
          image_id: 'heartbeat',
          image_name: 'heartbeat'
        });
      }
      await vi.runAllTimersAsync();
    });

    // Then - connection should remain stable
    expect(result.current.isConnected).toBe(true);
    expect(result.current.error).toBeNull();
  });

  it('cleans up resources on unmount', async () => {
    // Given - a hook instance with active connection
    const { unmount } = renderHook(() => useImageStream());
    
    await act(async () => {
      await vi.runAllTimersAsync();
    });

    // When - component unmounts
    const closeSpy = vi.spyOn(MockEventSource.prototype, 'close');
    unmount();

    // Then - should clean up EventSource
    expect(closeSpy).toHaveBeenCalled();
  });
}); 