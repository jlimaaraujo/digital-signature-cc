import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNavigationGuard } from '../contexts/NavigationGuardContext';
import './Step2ChooseDocument.css';

export default function Step2EscolhaDocumento() {
    const navigate = useNavigate();
    const { setAllowedStep, resetFlow } = useNavigationGuard();
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isUploading, setIsUploading] = useState(false);

    const handleFileSelect = (file: File) => {
        // Verificar se é PDF
        if (file.type !== 'application/pdf') {
            alert('Por favor, selecione apenas arquivos PDF.');
            return;
        }

        // Verificar tamanho (3MB = 3 * 1024 * 1024 bytes)
        if (file.size > 3 * 1024 * 1024) {
            alert('O documento deve ter no máximo 3MB.');
            return;
        }

        setSelectedFile(file);
    };

    const uploadFileToBackend = async (file: File) => {
        try {
            setIsUploading(true);

            // Converter o ficheiro para ArrayBuffer (bytes)
            const arrayBuffer = await file.arrayBuffer();

            const response = await fetch('http://localhost:8080/api/signature/hash', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/octet-stream',
                },
                body: arrayBuffer,
            });

            if (!response.ok) {
                throw new Error('Erro ao processar documento');
            }

            // Tentar ler como texto primeiro
            const responseText = await response.text();

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (e) {
                // Se não for JSON, usar como string
                result = responseText;
            }

            // Armazenar o resultado no localStorage para usar nas próximas páginas
            const hash = typeof result === 'string' ? result : (result.hash || result);

            // Converter o ficheiro para base64 para armazenar no localStorage
            const reader = new FileReader();
            reader.onload = () => {
                const base64String = reader.result as string;

                const documentInfo = {
                    name: file.name,
                    size: file.size,
                    hash: hash,
                    fileData: base64String
                };

                localStorage.setItem('documentHash', hash);
                localStorage.setItem('documentInfo', JSON.stringify(documentInfo));
            };
            reader.readAsDataURL(file);

            return result;
        } catch (error) {
            throw error;
        } finally {
            setIsUploading(false);
        }
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleFileSelect(file);
        }
    };

    const handleNext = async () => {
        if (selectedFile) {
            try {
                await uploadFileToBackend(selectedFile);
                setAllowedStep(3); // Permitir navegação para o passo 3
                navigate('/step3');
            } catch (error) {
            }
        }
    };

    const handleBack = () => {
        resetFlow(); // Reiniciar o fluxo; volta ao passo 1
    };

    const removeFile = () => {
        setSelectedFile(null);
    };

    return (
        <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl w-full flex flex-col items-center">
            <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Escolha do Documento
                </h2>

                {!selectedFile && (
                    <p className="text-gray-700 mb-8 text-lg leading-relaxed">
                        <b>Selecione o documento PDF que deseja assinar digitalmente.</b>
                    </p>
                )}

                {/* Seleção de Documento */}
                <div className="w-full max-w-2xl mb-8 flex flex-col items-center">
                    {selectedFile ? (
                        <div className="flex flex-col items-center">
                            <h3 className="text-lg font-semibold text-green-600 mb-2">
                                Documento selecionado:
                            </h3>
                            <p className="text-gray-700">{selectedFile.name}</p>
                            <p className="text-sm text-gray-500">
                                {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                            </p>
                            <button
                                onClick={removeFile}
                                className="remove-file-button"
                            >
                                Remover documento
                            </button>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center">
                            <button
                                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-md cursor-pointer transition-colors mb-2"
                                onClick={() => document.getElementById('fileInput')?.click()}
                                type="button"
                            >
                                Selecionar documento PDF
                            </button>
                            <input
                                id="fileInput"
                                type="file"
                                accept=".pdf"
                                onChange={handleFileChange}
                                style={{ display: 'none' }}
                            />
                            <p className="text-sm text-gray-500 mt-4">
                                Apenas documentos PDF • Máximo 3MB
                            </p>
                        </div>
                    )}
                </div>

                {/* Botões de navegação */}
                <div className="flex gap-4">
                    <button
                        onClick={handleBack}
                        disabled={isUploading}
                        className={`font-semibold py-3 px-8 rounded-md transition-colors duration-200 ${isUploading
                                ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                                : 'bg-gray-300 hover:bg-gray-400 text-gray-700'
                            }`}
                    >
                        VOLTAR
                    </button>
                    {selectedFile && (
                        <button
                            onClick={handleNext}
                            disabled={isUploading}
                            className={`font-semibold py-3 px-8 rounded-md transition-colors duration-200 flex items-center gap-2 bg-blue-700 hover:bg-blue-800 text-white`}
                        >
                            {isUploading && (
                                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                            )}
                            {isUploading ? 'A ENVIAR...' : 'CONTINUAR'}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
