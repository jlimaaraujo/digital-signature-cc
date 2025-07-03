import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import './Step3Preview.css';

// Configurar o worker do PDF.js
pdfjs.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs';

export default function Step3Preview() {
    const navigate = useNavigate();
    const [documentInfo, setDocumentInfo] = useState<any>(null);
    const [numPages, setNumPages] = useState<number>(0);
    const [pageNumber, setPageNumber] = useState<number>(1);
    const [scale, setScale] = useState<number>(1.0);

    useEffect(() => {
        // Recuperar informações do documento do localStorage
        const documentData = localStorage.getItem('documentInfo');
        const documentHash = localStorage.getItem('documentHash');

        if (documentData) {
            try {
                const parsedData = JSON.parse(documentData);
                setDocumentInfo(parsedData);
            } catch (error) {
                // Se não conseguir fazer parse, ignorar erro
            }
        } else if (documentHash) {
            // Se só temos o hash, criar um objeto básico
            setDocumentInfo({
                name: 'Documento',
                size: 0,
                hash: documentHash
            });
        }
    }, []);

    const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
        setNumPages(numPages);
    };

    const handleNext = () => {
        // Navegar diretamente para a próxima etapa
        navigate('/step3details');
    };

    const handleBack = () => {
        navigate('/step2');
    };

    return (
        <>
            <div className="center-content">
                <div className="rounded-lg shadow-md p-12 max-w-4xl flex flex-col items-center">
                    <h2 className="text-2xl font-bold text-blue-600 mb-8">
                        Pré-visualização do Documento
                    </h2>

                    <p className="text-gray-700 mb-8 text-lg leading-relaxed">
                        Verifique o documento que será assinado digitalmente.
                    </p>

                    {/* Informações do documento */}
                    {documentInfo ? (
                        <div className="w-full max-w-4xl mb-8">
                            {/* Cabeçalho do visualizador */}
                            <div className="bg-gray-800 text-white p-4 rounded-t-lg">
                                {/* Nome do arquivo centralizado */}
                                <div className="text-center mb-5">
                                    <span className="text-lg font-medium">{documentInfo.name}</span>
                                </div>
                                
                                {/* Controles centralizados */}
                                <div className="flex items-center justify-center space-x-6">
                                    {/* Controles de zoom */}
                                    <div className="flex items-center space-x-2">
                                        <button
                                            onClick={() => setScale(prev => Math.max(0.5, prev - 0.25))}
                                            className="w-8 h-8 bg-gray-700 hover:bg-gray-600 rounded flex items-center justify-center text-sm font-bold transition-colors"
                                            title="Diminuir zoom"
                                        >
                                            -
                                        </button>
                                        <span className="text-sm font-medium min-w-[50px] text-center">
                                            {Math.round(scale * 100)}%
                                        </span>
                                        <button
                                            onClick={() => setScale(prev => Math.min(5.0, prev + 0.25))}
                                            className="w-8 h-8 bg-gray-700 hover:bg-gray-600 rounded flex items-center justify-center text-sm font-bold transition-colors"
                                            title="Aumentar zoom"
                                        >
                                            +
                                        </button>
                                        
                                        {/* Botões de zoom rápido */}
                                        <div className="flex items-center space-x-1 ml-2">
                                            
                                        </div>
                                    </div>
                                    
                                    {/* Navegação de páginas */}
                                    {numPages > 1 && (
                                        <div className="flex items-center space-x-2">
                                            <button
                                                onClick={() => setPageNumber(prev => Math.max(1, prev - 1))}
                                                disabled={pageNumber <= 1}
                                                className="w-8 h-8 bg-gray-700 hover:bg-gray-600 rounded flex items-center justify-center text-sm disabled:opacity-50 transition-colors"
                                            >
                                                ◀
                                            </button>
                                            <span className="text-sm font-medium min-w-[60px] text-center">
                                                {pageNumber} / {numPages}
                                            </span>
                                            <button
                                                onClick={() => setPageNumber(prev => Math.min(numPages, prev + 1))}
                                                disabled={pageNumber >= numPages}
                                                className="w-8 h-8 bg-gray-700 hover:bg-gray-600 rounded flex items-center justify-center text-sm disabled:opacity-50 transition-colors"
                                            >
                                                ▶
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                            
                            {/* Área do PDF com scroll */}
                            <div className="pdf-viewer-container">
                                {documentInfo.fileData ? (
                                    <div className="pdf-content-wrapper">
                                        <div className="pdf-document-wrapper">
                                            <Document
                                                file={documentInfo.fileData}
                                                onLoadSuccess={onDocumentLoadSuccess}
                                                loading={
                                                    <div className="pdf-loading-container">
                                                        <div className="text-gray-500 text-center">
                                                            <div className="pdf-loading-spinner"></div>
                                                            <p>Carregando PDF...</p>
                                                        </div>
                                                    </div>
                                                }
                                                error={
                                                    <div className="pdf-error-container">
                                                        <div className="text-center">
                                                            <svg className="w-12 h-12 mx-auto mb-4" fill="currentColor" viewBox="0 0 20 20">
                                                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                                            </svg>
                                                            <p>Erro ao carregar PDF</p>
                                                        </div>
                                                    </div>
                                                }
                                            >
                                                <Page 
                                                    pageNumber={pageNumber} 
                                                    scale={scale}
                                                    renderAnnotationLayer={false}
                                                    renderTextLayer={false}
                                                />
                                            </Document>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="h-full flex items-center justify-center text-gray-500">
                                        <div className="text-center">
                                            <svg className="w-16 h-16 mx-auto mb-4" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clipRule="evenodd" />
                                            </svg>
                                            <p className="text-lg">Arquivo não disponível para preview</p>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="w-full max-w-2xl mb-8">
                            <div className="border-2 border-red-500 bg-red-50 rounded-lg p-8 text-center">
                                <div className="text-red-600 mb-4">
                                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                    </svg>
                                </div>
                                <p className="text-lg font-semibold text-red-600 mb-2">
                                    Nenhum documento encontrado
                                </p>
                                <p className="text-red-500">
                                    Por favor, volte e selecione um documento
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Botões de navegação */}
                    <div className="flex gap-4">
                        <button
                            onClick={handleBack}
                            className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                        >
                            VOLTAR
                        </button>
                        <button
                            onClick={handleNext}
                            className="bg-blue-700 hover:bg-blue-800 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                        >
                            CONTINUAR
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}
