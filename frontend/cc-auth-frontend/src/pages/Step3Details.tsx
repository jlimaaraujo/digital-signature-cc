import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import './Step3Details.css';

export default function Step3Details() {
    const navigate = useNavigate();
    const [showVisualSignature, setShowVisualSignature] = useState<boolean>(false);

    const handleBack = () => {
        navigate('/step3');
    };

    const handleSubmit = () => {
        // Guardar a opção no localStorage
        localStorage.setItem('showVisualSignature', JSON.stringify(showVisualSignature));
        // Navegar para o Step4
        navigate('/step5');
    };

    return (
        <div className="center-content">
            <div className="rounded-lg shadow-md p-12 max-w-2xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Detalhes da assinatura do documento
                </h2>

                <div className="w-full space-y-6">
                    {/* Opção de assinatura visual */}
                    <div className="flex items-start space-x-3">
                        <input
                            type="checkbox"
                            id="visualSignature"
                            checked={showVisualSignature}
                            onChange={(e) => setShowVisualSignature(e.target.checked)}
                            className="mt-1 w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
                        />
                        <label htmlFor="visualSignature" className="text-gray-700 text-base leading-relaxed">
                            Deseja que a assinatura esteja visível no documento?
                        </label>
                    </div>

                    {/* Informação adicional */}
                    <div className="w-full p-4 bg-blue-50 border border-blue-200 rounded-lg">
                        <div className="flex items-start">
                            <div>
                                <p className="text-blue-800 text-sm font-medium mb-1">Informação</p>
                                <p className="text-blue-700 text-xs leading-relaxed">
                                    {showVisualSignature ? (
                                        'A assinatura será visível no documento PDF como uma representação visual da sua assinatura digital.'
                                    ) : (
                                        'A assinatura será apenas digital, sem representação visual no documento PDF.'
                                    )}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Botões de navegação */}
                <div className="flex gap-4 mt-8">
                    <button
                        onClick={handleBack}
                        className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                    >
                        VOLTAR
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="bg-blue-700 hover:bg-blue-800 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                    >
                        SUBMETER
                    </button>
                </div>
            </div>
        </div>
    );
}
