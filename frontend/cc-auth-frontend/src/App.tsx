import { Outlet } from 'react-router-dom';

export default function App() {
  return (
    <div className="min-h-screen bg-gray-100">

      
      {/* Main content */}
      <div className="p-4 max-w-6xl mx-auto">
        <Outlet />
      </div>
    </div>
  );
}