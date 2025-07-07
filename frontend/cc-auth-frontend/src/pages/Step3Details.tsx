import { useNavigate } from 'react-router-dom';
import { useState, useEffect, useRef } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import './Step3Details.css';

// Configurar o worker do PDF.js
pdfjs.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs';

export default function Step3Details() {
    const navigate = useNavigate();
    const [showVisualSignature, setShowVisualSignature] = useState<boolean>(false);
    const [documentInfo, setDocumentInfo] = useState<any>(null);
    const [numPages, setNumPages] = useState<number>(0);
    const [selectedPage, setSelectedPage] = useState<number>(1);
    const [signaturePosition, setSignaturePosition] = useState({ x: 30, y: 11 });
    const [isDragging, setIsDragging] = useState(false);
    const [pageWidth, setPageWidth] = useState<number>(0);
    const [pageHeight, setPageHeight] = useState<number>(0);
    const [scale, setScale] = useState<number>(1);
    const containerRef = useRef<HTMLDivElement>(null);
    const markerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        // Recuperar informações do documento do localStorage
        const documentData = localStorage.getItem('documentInfo');
        if (documentData) {
            try {
                const parsedData = JSON.parse(documentData);
                setDocumentInfo(parsedData);
            } catch (error) {
                console.error('Erro ao carregar documento:', error);
            }
        }
    }, []);

    const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
        setNumPages(numPages);
    };

    const onPageLoadSuccess = (page: any) => {
        const { width, height } = page.getViewport({ scale: 1 });
        setPageWidth(width);
        setPageHeight(height);
        if (containerRef.current) {
            const containerWidth = containerRef.current.offsetWidth;
            // Usa altura máxima de 60vh para não passar do footer
            const containerHeight = Math.min(window.innerHeight * 0.6, 500);
            const widthScale = containerWidth / width;
            const heightScale = containerHeight / height;
            setScale(Math.min(widthScale, heightScale, 1)); 
        }
    };

    const handleMouseDown = (e: React.MouseEvent) => {
        e.preventDefault();
        setIsDragging(true);
        document.body.style.cursor = 'grabbing';
        document.body.classList.add('select-none');
    };

    const handleMouseMove = (e: MouseEvent) => {
        if (!isDragging || !containerRef.current || !markerRef.current) return;

        const container = containerRef.current;
        const containerRect = container.getBoundingClientRect();
        const markerWidth = markerRef.current.offsetWidth;
        const markerHeight = markerRef.current.offsetHeight;

        // Calcular posição relativa ao container
        let x = e.clientX - containerRect.left - markerWidth / 2;
        let y = e.clientY - containerRect.top - markerHeight / 2;

        // Limitar dentro dos bounds do container
        x = Math.max(0, Math.min(x, containerRect.width - markerWidth));
        y = Math.max(0, Math.min(y, containerRect.height - markerHeight));

        // Converter para percentagem para ser responsivo
        const xPercent = (x / containerRect.width) * 100;
        const yPercent = (y / containerRect.height) * 100;

        setSignaturePosition({ x: xPercent, y: yPercent });
    };

    const handleMouseUp = () => {
        setIsDragging(false);
        document.body.style.cursor = 'default';
        document.body.classList.remove('select-none');
    };

    useEffect(() => {
        if (isDragging) {
            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp);
            return () => {
                document.removeEventListener('mousemove', handleMouseMove);
                document.removeEventListener('mouseup', handleMouseUp);
            };
        }
    }, [isDragging]);

    const handleBack = () => {
        navigate('/step3');
    };

    const handleSubmit = () => {
        // Guardar todas as opções no localStorage incluindo posição exacta
        const signatureConfig = {
            showVisualSignature,
            page: selectedPage,
            position: {
                xPercent: signaturePosition.x,
                yPercent: signaturePosition.y
            },
            pageWidth,
            pageHeight
        };

        localStorage.setItem('signatureConfig', JSON.stringify(signatureConfig));
        navigate('/step5');
    };

    return (
        <div className="min-h-screen bg-gray-100 py-4">
            <h2 className="text-2xl font-bold text-blue-600 mb-8">
                        Detalhes da Assinatura
            </h2>
            {/* Main Content - layout lado a lado */}
            <div className="signature-details-layout bg-white rounded-lg shadow-lg p-8">
                {/* Preview do PDF à esquerda */}
                <div className="signature-details-preview">
                    <div
                        ref={containerRef}
                        className="pdf-preview-container flex items-center justify-center"
                        style={{
                            cursor: isDragging ? 'grabbing' : 'default',
                            background: '#fff',
                            width: pageWidth && pageHeight ? pageWidth * scale : 'auto',
                            height: pageWidth && pageHeight ? pageHeight * scale : 'auto',
                            minWidth: '320px',
                            minHeight: '400px',
                            maxWidth: '100%',
                            maxHeight: '60vh',
                            margin: '0 auto',
                            transition: 'width 0.2s, height 0.2s',
                        }}
                    >
                        {documentInfo && documentInfo.fileData ? (
                            <div style={{ position: 'relative', width: '100%', height: '100%' }}>
                                <Document
                                    file={documentInfo.fileData}
                                    onLoadSuccess={onDocumentLoadSuccess}
                                    loading={
                                        <div className="flex items-center justify-center h-full">
                                            <div className="loading-spinner"></div>
                                        </div>
                                    }
                                    error={
                                        <div className="flex items-center justify-center h-full text-red-500">
                                            <p>Erro ao carregar PDF</p>
                                        </div>
                                    }
                                >
                                    <Page
                                        pageNumber={selectedPage}
                                        scale={scale}
                                        className="pdf-page"
                                        renderAnnotationLayer={false}
                                        renderTextLayer={false}
                                        onLoadSuccess={onPageLoadSuccess}
                                    />
                                </Document>
                                {/* Marca de assinatura arrastável - só mostrar se checkbox estiver marcada */}
                                {showVisualSignature && (
                                    <div
                                        ref={markerRef}
                                        className={`signature-marker ${isDragging ? 'dragging' : ''}`}
                                        style={{
                                            left: `${signaturePosition.x}%`,
                                            top: `${signaturePosition.y}%`,
                                            transform: 'translate(-50%, -50%)',
                                            pointerEvents: isDragging ? 'none' : 'auto',
                                        }}
                                        onMouseDown={handleMouseDown}
                                    >
                                        ASSINATURA
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="flex items-center justify-center h-full text-gray-500">
                                <p className="text-center px-4">
                                    A carregar documento...
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Configuração e informação à direita */}
                <div className="signature-details-info space-y-6">
                    {/* Checkbox para assinatura visual */}
                    <div className="flex items-start space-x-3">
                        <input
                            type="checkbox"
                            id="visualSignature"
                            checked={showVisualSignature}
                            onChange={(e) => setShowVisualSignature(e.target.checked)}
                            className="mt-1 w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
                            />
                        <label htmlFor="visualSignature" className="text-gray-700 text-base leading-relaxed cursor-pointer">
                            Deseja que a assinatura esteja visível no documento?
                        </label>
                    </div>

                    {/* Configurações quando checkbox está marcada */}
                    {showVisualSignature && (
                        <div className="config-panel space-y-4 bg-gray-50 p-4 rounded-lg">
                            <h3 className="text-lg font-semibold text-gray-800">Configurar posição da assinatura:</h3>
                            <p className="text-sm text-gray-600">
                                Arraste a marca azul para posicionar a assinatura
                            </p>
                            {/* Seleção de página se houver múltiplas */}
                            {numPages > 1 && (
                                <div className="flex items-center space-x-3">
                                    <label htmlFor="pageSelect" className="page-pabel text-gray-700 font-medium">
                                        <b>Página:</b> 
                                    </label>
                                    <select
                                        id="pageSelect"
                                        value={selectedPage}
                                        onChange={(e) => setSelectedPage(Number(e.target.value))}
                                        className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        {Array.from({ length: numPages }, (_, i) => i + 1).map(page => (
                                            <option key={page} value={page}>
                                                {page} de {numPages}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Caixa de informação */}
                    <div className="info-box">
                        <div className="flex items-start">
                            <svg className="info-icon" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                            </svg>
                            <div className="ml-3">
                                <p className="info-title">Informação</p>
                                <p className="info-text">
                                    {showVisualSignature ? (
                                        'A assinatura será visível no documento PDF na posição selecionada. Pode arrastar a marca azul para ajustar a localização.'
                                    ) : (
                                        'A assinatura será apenas digital, sem representação visual no documento PDF.'
                                    )}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* Botões de navegação */}
                    <div className="button-group flex gap-4 mt-8 justify-center">
                        <button
                            onClick={handleBack}
                            className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                        >
                            VOLTAR
                        </button>
                        <button
                            onClick={handleSubmit}
                            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                        >
                            SUBMETER
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}