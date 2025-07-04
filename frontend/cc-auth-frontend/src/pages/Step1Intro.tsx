import { useNavigate } from 'react-router-dom';
import './Step1Intro.css';

export default function Step1Intro() {
    const navigate = useNavigate();

    const handleAdvance = () => {
        navigate('/step2');
    };

    return (
            <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl w-full flex flex-col items-center">
                <h2>
                    Bem-Vindo à Assinatura Digital
                </h2>

                <p className="intro-text">
                    Assine um documento pdf (máximo de 3MB) com a sua Chave Móvel Digital.
                </p>

                <p className="intro-text">
                    Vai precisar de:
                </p>

                <div className="mb-8 flex flex-col items-center">
                    <ul className="requirements-list">
                        <li className="leading-relaxed">Chave Móvel Digital ativa</li>
                        <li className="leading-relaxed">Assinatura digital ativa da Chave Móvel Digital</li>
                        <li className="leading-relaxed">PIN de assinatura da Chave Móvel Digital</li>
                    </ul>
                </div>

                <button
                    onClick={handleAdvance}
                    className="bg-blue-700 hover:bg-blue-800 text-white 
                                font-semibold py-3 px-8 rounded-md transition-colors 
                                duration-200 text-lg">
                    AVANÇAR
                </button>
            </div>
    );
}