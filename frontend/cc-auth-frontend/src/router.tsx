import { createBrowserRouter } from 'react-router-dom';
import App from './App';
import Step1Intro from './pages/Step1Intro';
import Step2EscolhaDocumento from './pages/Step2ChooseDocument';
import Step3Preview from './pages/Step3Preview';
import Step3Details from './pages/Step3Details';
import Step4Signature from './pages/Step4Signature';
import Step5OTP from './pages/Step5OTP';
import Step6Result from './pages/Step6Result';

export const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { path: '/', element: <Step1Intro /> },
            { path: '/step2', element: <Step2EscolhaDocumento /> },
            { path: '/step3', element: <Step3Preview /> },
            { path: '/step3details', element: <Step3Details /> },
            { path: '/step4', element: <Step4Signature /> },
            { path: '/step5', element: <Step5OTP /> },
            { path: '/step6', element: <Step6Result /> },
        ],
    },
]);