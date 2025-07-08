import { NavigationGuardProvider } from './contexts/NavigationGuardContext';
import App from './App';

export default function ProtectedApp() {
  return (
    <NavigationGuardProvider>
      <App />
    </NavigationGuardProvider>
  );
}
