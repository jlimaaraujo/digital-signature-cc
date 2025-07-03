import { useLocation } from 'react-router-dom';

const steps = [
    'Introdução',
    'Escolha de Documento',
    'Pré-Visualização de Documento',
    'Detalhes da Assinatura',
    'Pedido de Assinatura',
    'Confirmação de Assinatura',
    'Download do Documento',
];

export default function Stepper() {
    const location = useLocation();
    const activeIndex = [
        '/',
        '/step2',
        '/step3',
        '/step4',
        '/step5',
        '/step6',
        '/step7',
    ].indexOf(location.pathname);

    return (
        <div className="bg-white py-6 border-b">
            <div className="max-w-6xl mx-auto px-4">
                <div className="flex justify-center">
                    <ol className="flex flex-wrap justify-center items-center space-x-2 sm:space-x-4 text-sm">
                        {steps.map((label, idx) => (
                            <li key={idx} className={`flex items-center ${
                                idx <= activeIndex 
                                    ? 'text-blue-700 font-semibold' 
                                    : 'text-gray-400'
                            }`}>
                                <span className={`w-8 h-8 flex items-center justify-center rounded-full border-2 mr-2 text-sm font-semibold ${
                                    idx <= activeIndex 
                                        ? idx === activeIndex 
                                            ? 'bg-blue-700 text-white border-blue-700'
                                            : 'bg-blue-100 text-blue-700 border-blue-700'
                                        : 'border-gray-300 text-gray-400'
                                }`}>
                                    {idx + 1}
                                </span>
                                <span className="text-xs sm:text-sm">{label}</span>
                                {idx < steps.length - 1 && (
                                    <span className="mx-2 text-gray-300">&rarr;</span>
                                )}
                            </li>
                        ))}
                    </ol>
                </div>
            </div>
        </div>
    );
}
