import { ImageViewer } from './components/ImageViewer'

function App() {
  return (
    <div className="min-h-screen bg-[#0B132B] text-white">
      {/* Navigation Bar */}
      <nav className="bg-[#0B132B] border-b border-gray-800 px-4 py-4">
        <div className="container mx-auto flex items-center">
          <img 
            src="/quera-logo.png" 
            alt="QuEra Logo" 
            className="h-8 w-auto"
          />
          <h1 className="ml-4 text-xl font-semibold">
            Image Service Viewer
          </h1>
        </div>
      </nav>

      {/* Main Content */}
      <main className="container mx-auto">
        <ImageViewer />
      </main>
    </div>
  )
}

export default App