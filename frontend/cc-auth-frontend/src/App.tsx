import { Outlet, useLocation } from 'react-router-dom';
import './App.css';

export default function App() {
  const location = useLocation();

  const steps = [
    { path: '/step1', label: 'Introdução' },
    { path: '/step2', label: 'Escolha de Documento' },
    { path: '/step3', label: 'Pré-Visualização de Documento' },
    { path: '/step4', label: 'Detalhes da Assinatura' },
    { path: '/step5', label: 'Pedido de Assinatura' },
    { path: '/step6', label: 'Confirmação de Assinatura' },
  ];

  return (
    <div className="app-container">
      {/* Header */}
      <header className="header">
        <h1 className="h1-header">Assinatura Digital com Chave Móvel Digital</h1> 
      </header>

      {/* Stepper */}
      <nav className="stepper">
        <ul className="stepper-list">
          {steps.map((step, index) => {
            const isActive = location.pathname === step.path || (step.path === '/step1' && location.pathname === '/');
            const isCompleted = steps.findIndex(s => s.path === location.pathname) > index;
            return (
              <li
                key={step.path}
                className={`stepper-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
              >
                {step.label}
              </li>
            );
          })}
        </ul>
      </nav>
      
      {/* Main content */}
      <div className="main-content">
        <Outlet />
      </div>

      {/* Footer */}
      <footer className="footer">
        <hr />
        <p className="footer-text">© 2025 Assinatura Digital com Chave Móvel Digital</p>
      </footer>
    </div>
  );
}