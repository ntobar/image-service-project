# Image Platform Service

A real-time image viewing platform that displays and cycles through images using server-sent events (SSE) for live updates.

## Features

- **Real-time Image Updates**: Uses Server-Sent Events (SSE) for live image notifications without polling
- **Automatic Image Cycling**: Cycles through available images at configurable intervals
- **Multi-Frame Support**: Display multiple image frames cycling independently
- **Connection Status**: Real-time connection status monitoring with automatic reconnection
- **Responsive Design**: Adapts to different screen sizes with a modern UI
- **Multi-User Support**: Supports multiple concurrent users viewing the stream

## Requirements Fulfilled

1. **Modern UI Framework**: Built with React + TypeScript + Vite
2. **Real-time Updates**: 
   - Uses SSE (`EventSource`) instead of polling
   - Listens for `UPLOAD`, `DELETE`, and `HEARTBEAT` events
   - New images appear automatically in the cycle
   
3. **Image Cycling**:
   - Maintains strict order as defined by the backend
   - Configurable cycle intervals (slow/normal/fast)
   - Shows image metadata (name, upload time)

4. **Multi-Frame Support**:
   - Display up to 10 frames simultaneously
   - Each frame cycles independently
   - Configurable offset between frames

5. **Connection Management**:
   - Automatic reconnection on connection loss
   - Clear status indicators
   - Graceful error handling

## Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn
- Running backend service (on port 8080 by default)

### Installation

1. Clone the repository:
2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

The service will be available at `http://localhost:5173` by default.

### Configuration

The service expects the backend to be running on `http://localhost:8080`. To change this:

1. Update the proxy settings in `vite.config.ts`:
   ```typescript
   server: {
     proxy: {
       '/api': {
         target: 'http://backend-url',
         changeOrigin: true,
         secure: false,
       },
     },
   }
   ```

## Usage

### Single Frame Mode
- Displays one image at a time
- Shows full metadata
- Cycles through images at the selected speed

### Multi-Frame Mode
- Display multiple frames (1-10)
- Each frame cycles independently
- Compact view with essential metadata
- Adjustable cycle speed (slow/normal/fast)

### Controls
- Switch between single/multi-frame modes
- Add/remove frames in multi-frame mode
- Adjust cycle speed
- Monitor connection status

## Development

### Running Tests
```bash
npm test
```

### Building for Production
```bash
npm run build
```

## API Integration

The service integrates with the following backend endpoints:

- `GET /api/images`: Fetch initial image list
- `GET /api/images/stream`: SSE endpoint for real-time updates
- `GET /api/images/:id`: Fetch individual image
- `GET /api/images/:id/metadata`: Fetch image metadata

## Architecture

- **React + TypeScript**: For type-safe component development
- **Tailwind CSS**: For responsive and modern styling
- **Vite**: For fast development and optimized builds
- **Server-Sent Events**: For real-time updates
- **Custom Hooks**: For encapsulated business logic
  - `useImageStream`: Manages SSE connection and image state
  - Handles connection recovery and event processing

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
