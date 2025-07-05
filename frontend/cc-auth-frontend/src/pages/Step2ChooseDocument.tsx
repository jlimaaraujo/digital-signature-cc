import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Step2ChooseDocument.css';

export default function Step2EscolhaDocumento() {
    const navigate = useNavigate();
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [dragActive, setDragActive] = useState(false);
    const [isUploading, setIsUploading] = useState(false);

    const handleFileSelect = (file: File) => {
        // Verificar se é PDF
        if (file.type !== 'application/pdf') {
            alert('Por favor, selecione apenas arquivos PDF.');
            return;
        }

        // Verificar tamanho (5MB = 5 * 1024 * 1024 bytes)
        if (file.size > 5 * 1024 * 1024) {
            alert('O arquivo deve ter no máximo 5MB.');
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
                const errorText = await response.text();
                throw new Error(`Erro no servidor: ${response.status} - ${errorText}`);
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
            console.error('Erro ao enviar o arquivo:', error);    
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

    const handleDrag = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === 'dragenter' || e.type === 'dragover') {
            setDragActive(true);
        } else if (e.type === 'dragleave') {
            setDragActive(false);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);

        const file = e.dataTransfer.files?.[0];
        if (file) {
            handleFileSelect(file);
        }
    };

    const handleNext = async () => {
        if (selectedFile) {
            try {
                await uploadFileToBackend(selectedFile);
                navigate('/step3');
            } catch (error) {
            }
        }
    };

    const handleBack = () => {
        navigate('/');
    };

    const removeFile = () => {
        setSelectedFile(null);
    };

    return (
        <div className="center-content">
            <div className="bg-white rounded-lg shadow-md p-12 max-w-4xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Escolha de Documento
                </h2>

                <p className="text-gray-700 mb-8 text-lg leading-relaxed">
                    Selecione o documento PDF que deseja assinar digitalmente.
                </p>

                {/* Área de Upload */}
                <div className="w-full max-w-2xl mb-8">
                    <div
                        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                            dragActive
                                ? 'border-blue-500 bg-blue-50'
                                : selectedFile
                                ? 'border-green-500 bg-green-50'
                                : 'border-gray-300 hover:border-blue-400'
                        }`}
                        onDragEnter={handleDrag}
                        onDragLeave={handleDrag}
                        onDragOver={handleDrag}
                        onDrop={handleDrop}
                    >
                        {selectedFile ? (
                            <div className="flex flex-col items-center">
                                <div className="text-green-600 mb-4">
                                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
                                    </svg>
                                </div>
                                <h3 className="text-lg font-semibold text-green-600 mb-2">
                                    Documento selecionado
                                </h3>
                                <p className="text-gray-700 mb-2">{selectedFile.name}</p>
                                <p className="text-sm text-gray-500 mb-4">
                                    {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                                </p>
                                <button
                                    onClick={removeFile}
                                    className="text-red-600 hover:text-red-800 text-sm underline"
                                >
                                    Remover documento
                                </button>
                            </div>
                        ) : (
                            <div>
                                <div className="text-gray-400 mb-4">
                                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clipRule="evenodd" />
                                    </svg>
                                </div>
                                <p className="text-lg font-semibold text-gray-700 mb-2">
                                    Arraste e solte o seu documento PDF aqui
                                </p>
                                <p className="text-gray-500 mb-4">ou</p>
                                <label className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-md cursor-pointer transition-colors">
                                    Selecionar documento
                                    <input
                                        type="file"
                                        accept=".pdf"
                                        onChange={handleFileChange}
                                        className="hidden"
                                    />
                                </label>
                                <p className="text-sm text-gray-500 mt-4">
                                    Apenas documentos PDF • Máximo 5MB
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Botões de navegação */}
                <div className="flex gap-4">
                    <button
                        onClick={handleBack}
                        disabled={isUploading}
                        className={`font-semibold py-3 px-8 rounded-md transition-colors duration-200 ${
                            isUploading 
                                ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                                : 'bg-gray-300 hover:bg-gray-400 text-gray-700'
                        }`}
                    >
                        VOLTAR
                    </button>
                    <button
                        onClick={handleNext}
                        disabled={!selectedFile || isUploading}
                        className={`font-semibold py-3 px-8 rounded-md transition-colors duration-200 flex items-center gap-2 ${
                            selectedFile && !isUploading
                                ? 'bg-blue-700 hover:bg-blue-800 text-white'
                                : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        }`}
                    >
                        {isUploading && (
                            <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                <path className="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                        )}
                        {isUploading ? 'A ENVIAR...' : 'CONTINUAR'}
                    </button>
                </div>
            </div>
        </div>
    );
}
