export interface ImageMetadata {
  id: string;
  name: string;
  size: number;
  mimeType: string;
  uploadedAt: string;
}

export interface ApiResponse<T> {
  data: T;
}

export interface ImageEvent {
  type: 'UPLOAD' | 'DELETE' | 'HEARTBEAT';
  image_id: string;
  image_name: string;
}

const API_BASE = '/api';

export const imageApi = {
  /**
   * Fetches all images from the backend
   * @returns Promise<ImageMetadata[]> Array of images
   */
  getImages: async (): Promise<ImageMetadata[]> => {
    try {
      const response = await fetch(`${API_BASE}/images`);
      console.log('API Response status:', response.status);
      
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('Image service not found. Please check if the server is running.');
        }
        throw new Error(`Server error: ${response.status}`);
      }

      const contentType = response.headers.get('content-type');
      console.log('Content-Type:', contentType);

      if (!contentType || !contentType.includes('application/json')) {
        throw new Error('Invalid response from server. Expected JSON.');
      }

      const responseText = await response.text();
      console.log('Raw response:', responseText);

      const { data } = JSON.parse(responseText) as ApiResponse<ImageMetadata[]>;
      console.log('Parsed images:', data);

      return data;
    } catch (error) {
      console.error('Error in getImages:', error);
      if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
        throw new Error('Unable to connect to the server. Please check if it is running.');
      }
      throw error;
    }
  }
}; 