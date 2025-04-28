import { useState, useEffect, useCallback, useRef } from 'react';
import { ImageMetadata, ImageEvent, imageApi } from '../services/api';

// Configuration constants for connection management
const MAX_RETRIES = 3;
const RETRY_DELAY = 2000;
const RECONNECT_INTERVAL = 5000;

// Heartbeat and polling configuration
const HEARTBEAT_CHECK_INTERVAL = 1000;  // Check heartbeat health every 1s
const HEARTBEAT_TIMEOUT = 7000;         // Consider dead after 7s without heartbeat
const FALLBACK_POLL_INTERVAL = 5000;    // Fallback poll every 5s
const MAX_FAILED_POLLS = 3;             // Give up after 3 failed polls

/**
 * Hook for managing real-time image stream connection with the backend.
 * Handles connection state, automatic reconnection, and image updates.
 */
export const useImageStream = () => {
  // State management for images and connection status
  const [images, setImages] = useState<ImageMetadata[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [isConnected, setIsConnected] = useState(false);
  const [initialLoadComplete, setInitialLoadComplete] = useState(false);

  // Refs for managing connection instances and intervals
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectIntervalRef = useRef<number | null>(null);
  const isCleaningUpRef = useRef(false);
  
  // Refs for heartbeat and polling management
  const lastHeartbeatRef = useRef<number>(Date.now());
  const heartbeatCheckerRef = useRef<number | null>(null);
  const fallbackPollingRef = useRef<number | null>(null);
  const failedPollsRef = useRef<number>(0);

  /**
   * Cleans up all active connections and intervals
   */
  const cleanupConnections = useCallback(() => {
    isCleaningUpRef.current = true;
    
    if (eventSourceRef.current) {
      console.log('Cleaning up existing EventSource connection');
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    if (reconnectIntervalRef.current) {
      clearInterval(reconnectIntervalRef.current);
      reconnectIntervalRef.current = null;
    }
    if (heartbeatCheckerRef.current) {
      clearInterval(heartbeatCheckerRef.current);
      heartbeatCheckerRef.current = null;
    }
    if (fallbackPollingRef.current) {
      clearInterval(fallbackPollingRef.current);
      fallbackPollingRef.current = null;
    }
    
    isCleaningUpRef.current = false;
  }, []);

  /**
   * Performs a lightweight health check poll to the backend
   */
  const performHealthCheck = useCallback(async () => {
    try {
      const response = await fetch('/api/images');
      if (!response.ok) {
        throw new Error('Health check failed');
      }
      // Reset failed polls count on successful check
      failedPollsRef.current = 0;
      setIsConnected(true);
      setError(null);
      return true;
    } catch (err) {
      failedPollsRef.current++;
      console.log(`Health check failed (${failedPollsRef.current}/${MAX_FAILED_POLLS})`);
      
      if (failedPollsRef.current >= MAX_FAILED_POLLS) {
        setIsConnected(false);
        setError('Connection lost. Please check if the server is running.');
        // Stop polling after max failures
        if (fallbackPollingRef.current) {
          clearInterval(fallbackPollingRef.current);
          fallbackPollingRef.current = null;
        }
        // Trigger reconnection if not already trying
        if (!reconnectIntervalRef.current && !isCleaningUpRef.current) {
          reconnectIntervalRef.current = window.setInterval(attemptReconnect, RECONNECT_INTERVAL);
        }
      }
      return false;
    }
  }, []);

  /**
   * Starts fallback polling when heartbeats are missing
   */
  const startFallbackPolling = useCallback(() => {
    if (fallbackPollingRef.current) return; // Already polling
    
    console.log('Starting fallback polling');
    failedPollsRef.current = 0;
    fallbackPollingRef.current = window.setInterval(performHealthCheck, FALLBACK_POLL_INTERVAL);
    // Perform immediate health check
    performHealthCheck();
  }, [performHealthCheck]);

  /**
   * Stops fallback polling (called when heartbeats resume)
   */
  const stopFallbackPolling = useCallback(() => {
    if (fallbackPollingRef.current) {
      console.log('Stopping fallback polling');
      clearInterval(fallbackPollingRef.current);
      fallbackPollingRef.current = null;
    }
    failedPollsRef.current = 0;
  }, []);

  /**
   * Checks for missing heartbeats and manages fallback polling
   */
  const checkHeartbeat = useCallback(() => {
    if (isCleaningUpRef.current) return;
    
    const timeSinceLastHeartbeat = Date.now() - lastHeartbeatRef.current;
    if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT) {
      // Start fallback polling if heartbeat is missing
      startFallbackPolling();
    }
  }, [startFallbackPolling]);

  /**
   * Handles incoming image events (upload/delete/heartbeat) from the server
   */
  const handleImageEvent = useCallback(async (event: ImageEvent) => {
    try {
      if (event.type === 'HEARTBEAT') {
        lastHeartbeatRef.current = Date.now();
        stopFallbackPolling();
        return;
      }
      
      if (event.type === 'UPLOAD') {
        try {
          // Fetch full metadata for newly uploaded image
          const response = await fetch(`/api/images/${event.image_id}/metadata`);
          if (!response.ok) {
            throw new Error(`Failed to fetch metadata: ${response.statusText}`);
          }
          const jsonResponse = await response.json();
          const newImage = jsonResponse.data as ImageMetadata;
          
          // Add new image if it doesn't already exist
          setImages(prevImages => {
            const exists = prevImages.some(img => img.id === newImage.id);
            if (exists) {
              return prevImages;
            }
            return [...prevImages, newImage];
          });
        } catch (err) {
          console.error('Error fetching image metadata:', err);
        }
      } else if (event.type === 'DELETE') {
        // Remove deleted image from state
        setImages(prevImages => prevImages.filter(img => img.id !== event.image_id));
      }
    } catch (err) {
      console.error('Error handling image event:', err);
    }
  }, [stopFallbackPolling]);

  /**
   * Sets up EventSource connection for real-time updates
   * Includes automatic retry logic for connection failures
   */
  const setupEventSource = useCallback(() => {
    if (isCleaningUpRef.current) return null;
    console.log('Setting up EventSource...');
    
    try {
      // Return existing connection if it's still open
      const currentEventSource = eventSourceRef.current;
      if (currentEventSource?.readyState === EventSource.OPEN) {
        console.log('EventSource connection already open');
        setIsConnected(true);
        setError(null);
        return currentEventSource;
      }
      
      cleanupConnections();
      
      // Establish new EventSource connection
      const eventSource = new EventSource('/api/images/stream');
      eventSourceRef.current = eventSource;
      
      // Handle incoming messages
      eventSource.onmessage = (event) => {
        if (isCleaningUpRef.current) return;
        try {
          const imageEvent: ImageEvent = JSON.parse(event.data);
          // Update connection status immediately for any valid message
          setIsConnected(true);
          setError(null);
          setRetryCount(0);
          
          // Process the event
          handleImageEvent(imageEvent);
        } catch (e) {
          console.error('Failed to parse SSE message:', e);
        }
      };

      // Handle successful connection
      eventSource.onopen = () => {
        if (isCleaningUpRef.current) return;
        console.log('SSE connection opened successfully');
        setIsConnected(true);
        setError(null);
        setRetryCount(0);
        
        // Start heartbeat checking
        if (heartbeatCheckerRef.current) {
          clearInterval(heartbeatCheckerRef.current);
        }
        heartbeatCheckerRef.current = window.setInterval(checkHeartbeat, HEARTBEAT_CHECK_INTERVAL);
      };

      // Handle connection errors
      eventSource.onerror = (e) => {
        if (isCleaningUpRef.current) return;
        
        console.error('SSE connection error:', e);
        if (eventSource.readyState === EventSource.CLOSED) {
          eventSource.close();
          eventSourceRef.current = null;
          setIsConnected(false);
          
          // Attempt immediate retries before switching to interval
          if (retryCount < MAX_RETRIES) {
            const nextRetry = retryCount + 1;
            console.log(`Attempting retry ${nextRetry}/${MAX_RETRIES} in ${RETRY_DELAY}ms`);
            setError(`Connection lost. Retrying... (${nextRetry}/${MAX_RETRIES})`);
            setTimeout(() => {
              if (!isCleaningUpRef.current) {
                setRetryCount(nextRetry);
                setupEventSource();
              }
            }, RETRY_DELAY);
          } else {
            console.error('Max retries reached, switching to reconnect interval');
            setError('Unable to connect to image stream. Please check if the server is running.');
            if (!reconnectIntervalRef.current && !isCleaningUpRef.current) {
              reconnectIntervalRef.current = window.setInterval(attemptReconnect, RECONNECT_INTERVAL);
            }
          }
        }
      };

      return eventSource;
    } catch (e) {
      console.error('Failed to create EventSource:', e);
      setError('Unable to connect to image stream. Please check if the server is running.');
      setIsConnected(false);
      return null;
    }
  }, [retryCount, handleImageEvent, cleanupConnections, checkHeartbeat]);

  /**
   * Attempts to reconnect to the backend after connection loss
   * First verifies backend availability, then re-establishes EventSource
   */
  const attemptReconnect = useCallback(async () => {
    if (isCleaningUpRef.current) return;
    console.log('Attempting to reconnect...');
    
    try {
      const fetchedImages = await imageApi.getImages();
      console.log('Successfully fetched images, attempting to restore connection');
      
      setImages(fetchedImages);
      setRetryCount(0);
      setupEventSource();
    } catch (err) {
      console.log('Backend still unavailable');
      setIsConnected(false);
      setError('Unable to connect to server. Retrying...');
    }
  }, [setupEventSource]);

  // Initialize connection and handle cleanup
  useEffect(() => {
    console.log('Initial useEffect running...');
    isCleaningUpRef.current = false;
    lastHeartbeatRef.current = Date.now(); // Initialize last heartbeat time
    
    const loadInitialImages = async () => {
      try {
        console.log('Fetching initial images...');
        const fetchedImages = await imageApi.getImages();
        console.log('Initial images fetched:', fetchedImages);
        setImages(fetchedImages);
        setInitialLoadComplete(true);
        setIsConnected(true);
        setError(null);
      } catch (err) {
        console.error('Failed to fetch initial images:', err);
        setError('Unable to load images. Please check if the server is running.');
        setInitialLoadComplete(true);
        setIsConnected(false);
        if (!reconnectIntervalRef.current) {
          reconnectIntervalRef.current = window.setInterval(attemptReconnect, RECONNECT_INTERVAL);
        }
      }
    };

    loadInitialImages().then(() => {
      if (!isCleaningUpRef.current) {
        setupEventSource();
      }
    });

    // Cleanup on unmount
    return () => {
      isCleaningUpRef.current = true;
      cleanupConnections();
    };
  }, [setupEventSource, attemptReconnect, cleanupConnections]);

  return { 
    images, 
    error, 
    isLoading: !initialLoadComplete && !error,
    isConnected 
  };
}; 