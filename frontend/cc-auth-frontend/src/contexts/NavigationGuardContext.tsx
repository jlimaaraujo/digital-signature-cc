import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

interface NavigationGuardContextType {
    allowedStep: number;
    setAllowedStep: (step: number) => void;
    canNavigateToStep: (step: number) => boolean;
    resetFlow: () => void;
}

const NavigationGuardContext = createContext<NavigationGuardContextType | undefined>(undefined);

interface NavigationGuardProviderProps {
    children: ReactNode;
}

const stepRoutes = [
    '/',       // Step 1
    '/step2',  // Step 2
    '/step3',  // Step 3
    '/step4',  // Step 4
    '/step5',  // Step 5
    '/step6',  // Step 6
    '/step7'   // Step 7
];

export function NavigationGuardProvider({ children }: NavigationGuardProviderProps) {
    const [allowedStep, setAllowedStepState] = useState<number>(1);
    const navigate = useNavigate();
    const location = useLocation();

    // Prevenir navegação do navegador (botões voltar/avançar)
    useEffect(() => {
        const preventNavigation = (e: PopStateEvent) => {
            e.preventDefault();
            // Redirecionar para a rota do passo permitido
            const currentPath = stepRoutes[allowedStep - 1];
            if (location.pathname !== currentPath) {
                navigate(currentPath, { replace: true });
            }
        };

        // Adicionar listener para eventos de popstate
        window.addEventListener('popstate', preventNavigation);

        // Colocar um estado para prevenir navegação para trás
        window.history.pushState(null, '', window.location.href);

        return () => {
            window.removeEventListener('popstate', preventNavigation);
        };
    }, [allowedStep, navigate, location.pathname]);

    // Verificar se o user está a tentar aceder um passo não permitido
    useEffect(() => {
        const currentStepIndex = stepRoutes.findIndex(route => route === location.pathname);
        const currentStep = currentStepIndex + 1;

        if (currentStep > allowedStep || currentStepIndex === -1) {
            // Redirecionar para o passo permitido mais próximo
            const allowedPath = stepRoutes[allowedStep - 1];
            navigate(allowedPath, { replace: true });
        }
    }, [location.pathname, allowedStep, navigate]);

    const setAllowedStep = (step: number) => {
        if (step >= 1 && step <= 7) {
            setAllowedStepState(step);
            // Armazenar o passo permitido no localStorage
            localStorage.setItem('allowedStep', step.toString());
        }
    };

    const canNavigateToStep = (step: number) => {
        return step <= allowedStep;
    };

    const resetFlow = () => {
        setAllowedStepState(1);
        localStorage.removeItem('allowedStep');
        navigate('/', { replace: true });
    };

    // Carregar o passo permitido do localStorage ao iniciar
    useEffect(() => {
        const savedStep = localStorage.getItem('allowedStep');
        if (savedStep) {
            const step = parseInt(savedStep);
            if (step >= 1 && step <= 7) {
                setAllowedStepState(step);
            }
        }
    }, []);

    const contextValue: NavigationGuardContextType = {
        allowedStep,
        setAllowedStep,
        canNavigateToStep,
        resetFlow
    };

    return (
        <NavigationGuardContext.Provider value={contextValue}>
            {children}
        </NavigationGuardContext.Provider>
    );
}

export function useNavigationGuard() {
    const context = useContext(NavigationGuardContext);
    if (context === undefined) {
        throw new Error('useNavigationGuard must be used within a NavigationGuardProvider');
    }
    return context;
}
