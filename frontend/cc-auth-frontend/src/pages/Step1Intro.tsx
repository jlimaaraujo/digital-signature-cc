import { useNavigate } from 'react-router-dom';
import './Step1Intro.css';

export default function Step1Intro() {
    const navigate = useNavigate();

    const handleAdvance = () => {
        navigate('/step2');
    };

    return (
            <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl w-full flex flex-col items-center" role="main" aria-label="Introdução à Assinatura Digital">
                <h2 tabIndex={0} className="mb-6 text-2xl font-bold text-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-400">
                    Bem-Vindo à Assinatura Digital
                </h2>

                <p className="intro-text mb-2" tabIndex={0}>
                    Assine um documento PDF (máximo de 3MB) com a sua Chave Móvel Digital.
                </p>

                <p className="intro-text mb-2" tabIndex={0}>
                    Vai precisar de:
                </p>

                <div className="mb-8 flex flex-col items-center">
                    <ul className="requirements-list" aria-label="Requisitos para assinatura digital">
                        <li className="leading-relaxed" tabIndex={0}>Chave Móvel Digital ativa</li>
                        <li className="leading-relaxed" tabIndex={0}>Assinatura digital ativa da Chave Móvel Digital</li>
                        <li className="leading-relaxed" tabIndex={0}>PIN de assinatura da Chave Móvel Digital</li>
                    </ul>
                </div>

                <div className="w-full flex flex-col items-center">
                <p className="sr-only" aria-live="polite" id="step-feedback">Está na etapa de introdução. Clique em avançar para continuar.</p>
                    <button
                        onClick={handleAdvance}
                        className="bg-blue-700 hover:bg-blue-800 focus:bg-blue-900 focus:ring-4 focus:ring-blue-300 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200 text-lg"
                        aria-label="Avançar para o próximo passo"
                        autoFocus
                    >
                        AVANÇAR
                    </button>
                </div>
            </div>
    );
}