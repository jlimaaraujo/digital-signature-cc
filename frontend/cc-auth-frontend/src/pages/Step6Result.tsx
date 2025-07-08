import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import './Step6Result.css';

export default function Step6Result() {
    const navigate = useNavigate();
    const [setDocumentInfo] = useState<any>(null);
    const [isSuccess] = useState<boolean>(true); // Em produção, isso viria do resultado da assinatura

    const handleDownload = async () => {
        try {
            // Recuperar o PDF assinado do localStorage
            const signedPdfBase64 = localStorage.getItem('signedPdfBlob');
            const documentInfo = localStorage.getItem('documentInfo');

            if (!signedPdfBase64) {
                alert('Erro: Documento assinado não encontrado. Refaça o processo de assinatura.');
                return;
            }

            // Converter base64 para blob
            const binaryString = atob(signedPdfBase64);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            const blob = new Blob([bytes], { type: 'application/pdf' });

            // Criar URL temporário para download
            const url = window.URL.createObjectURL(blob);

            // Criar elemento de link temporário para download
            const link = document.createElement('a');
            link.href = url;

            // Definir nome do arquivo
            const fileName = documentInfo ?
                JSON.parse(documentInfo).name.replace('.pdf', '_assinado.pdf') :
                'documento_assinado.pdf';

            link.download = fileName;

            // Executar download
            document.body.appendChild(link);
            link.click();

            // Limpar
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            alert('Erro ao fazer download do documento. Tente novamente.');
        }
    };

    const handleClose = () => {
        // Limpar dados e fechar aplicação ou voltar ao início
        localStorage.removeItem('documentInfo');
        localStorage.removeItem('documentHash');
        localStorage.removeItem('signedPdfBlob');
        localStorage.removeItem('phoneNumber');
        localStorage.removeItem('userPin');
        navigate('/');
    };

    useEffect(() => {
        // Recuperar informações do documento do localStorage
        const documentData = localStorage.getItem('documentInfo');
        if (documentData) {
            try {
                const parsedData = JSON.parse(documentData);
                setDocumentInfo(parsedData);
            } catch (error) {
                // Se não conseguir fazer parse, ignorar erro
            }
        }

        // Se a assinatura foi bem-sucedida, iniciar download automaticamente
        if (isSuccess) {
            handleDownload();
        }
    }, []);

    return (
        <div className="center-content">
            <div className="rounded-lg shadow-md p-12 max-w-2xl flex flex-col items-center text-center">
                {isSuccess ? (
                    <>
                        {/* Ícone de sucesso */}
                        <div className="w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mb-8">
                        </div>

                        <h2 className="text-2xl font-bold text-green-600 mb-4">
                            Descarregar Documento Assinado
                        </h2>

                        <p className="text-gray-700 mb-8 text-lg leading-relaxed">
                            O seu documento foi assinado digitalmente com a Chave Móvel Digital. O download iniciou automaticamente.
                        </p>

                        {/* Botões de ação */}
                        <div className="flex flex-col gap-4 w-full">
                            <button
                                onClick={handleDownload}
                                className="bg-green-600 hover:bg-green-700 text-white font-semibold py-4 px-8 rounded-md transition-colors duration-200 flex items-center justify-center"
                            >
                                DESCARREGAR NOVAMENTE
                            </button>

                            <button
                                onClick={handleClose}
                                className="flex-1 bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-6 rounded-md transition-colors duration-200"
                            >
                                CONCLUIR
                            </button>
                        </div>
                    </>
                ) : (
                    <>
                        {/* Ícone de erro */}
                        <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center mb-8">
                        </div>

                        <h2 className="text-2xl font-bold text-red-600 mb-4">
                            Erro na Assinatura
                        </h2>

                        <p className="text-gray-700 mb-8 text-lg leading-relaxed">
                            Ocorreu um erro durante o processo de assinatura digital. Por favor, tente novamente.
                        </p>

                        {/* Botões para erro */}
                        <div className="flex gap-4">
                            <button
                                onClick={() => navigate('/step4')}
                                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-6 rounded-md transition-colors duration-200"
                            >
                                TENTAR NOVAMENTE
                            </button>
                            <button
                                onClick={handleClose}
                                className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-6 rounded-md transition-colors duration-200"
                            >
                                CANCELAR
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
