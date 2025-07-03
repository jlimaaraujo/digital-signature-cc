import { useNavigate } from 'react-router-dom';
import './Step1Intro.css';

export default function Step1Intro() {
    const navigate = useNavigate();

    const handleAdvance = () => {
        navigate('/step2');
    };

    return (
        <div className="center-content">
            <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl w-full flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Bem-Vindo à Assinatura Digital
                </h2>

                <p className="text-gray-700 mb-8 text-lg leading-relaxed text-center">
                    Assine um documento pdf (máximo de 5MB) com a sua Chave Móvel Digital.
                </p>

                <div className="mb-8 flex flex-col items-center">
                    <p className="text-gray-700 font-medium mb-4">Vai precisar de:</p>
                    <ul className="list-none space-y-3 text-gray-700 max-w-md text-center">
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
        </div>
    );
}