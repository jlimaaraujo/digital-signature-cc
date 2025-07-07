import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import './Step5OTP.css';

export default function Step5OTP() {
    const navigate = useNavigate();
    const [otpCode, setOtpCode] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [phoneNumber, setPhoneNumber] = useState<string>('');

    useEffect(() => {
        // Recuperar número de telefone do localStorage se disponível
        const phone = localStorage.getItem('phoneNumber');
        if (phone) {
            setPhoneNumber(phone);
        }
    }, []);

    const handleOtpChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value.replace(/\D/g, '').slice(0, 6);
        setOtpCode(value);
        if (error) setError('');
    };

    const handleValidate = async () => {
        if (!otpCode) {
            setError('Por favor, introduza o código OTP');
            return;
        }

        if (otpCode.length < 4) {
            setError('O código OTP deve ter pelo menos 4 dígitos');
            return;
        }

        setIsLoading(true);
        setError('');

        try {
            // Recuperar a opção de assinatura visual
            const hasVisualSignature = localStorage.getItem('hasVisualSignature');
            const showVisual = hasVisualSignature ? JSON.parse(hasVisualSignature) : false;
            
            // Chamar o endpoint de validação OTP
            const response = await fetch('http://localhost:8080/api/signature/validate-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    otp: otpCode,
                    hasVisualSignature: showVisual
                }),
            });

            if (response.ok) {
                // O endpoint retorna diretamente o PDF assinado
                const blob = await response.blob();
                
                // Salvar o blob no localStorage para o Step6 fazer download
                const reader = new FileReader();
                reader.onload = function() {
                    const arrayBuffer = reader.result as ArrayBuffer;
                    const uint8Array = new Uint8Array(arrayBuffer);
                    const binaryString = Array.from(uint8Array).map(byte => String.fromCharCode(byte)).join('');
                    const base64String = btoa(binaryString);
                    localStorage.setItem('signedPdfBlob', base64String);
                    
                    // Navegar para o Step6 (resultado final)
                    navigate('/step7');
                };
                reader.readAsArrayBuffer(blob);
            } else {
                let errorMessage = 'Código OTP inválido ou expirado';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                } catch (jsonError) {
                    errorMessage = `Erro ${response.status}: ${response.statusText}`;
                }
                setError(errorMessage);
            }
        } catch (error) {
            // Melhor tratamento de diferentes tipos de erros
            if (error instanceof TypeError && error.message.includes('fetch')) {
                setError('Erro de conexão. Verifique se o backend está a funcionar em http://localhost:8080');
            } else {
                setError('Erro de conexão. Verifique se o backend está a funcionar.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleResendCode = async () => {
        setIsLoading(true);
        setError('');

        try {
            // Recuperar dados necessários do localStorage
            const phoneDigits = phoneNumber.replace(/\s/g, '');
            const formattedPhone = "+351" + phoneDigits; // Adicionar prefixo país
            const documentInfo = localStorage.getItem('documentInfo');
            let docName = 'Contrato Teste'; // valor padrão
            
            if (documentInfo) {
                try {
                    const parsedData = JSON.parse(documentInfo);
                    docName = parsedData.name || 'Documento';
                } catch (error) {
                    // Se não conseguir fazer parse, usar nome padrão
                }
            }

            // Para reenvio, vamos usar os dados salvos
            const savedPin = localStorage.getItem('userPin');
            
            if (!savedPin) {
                setError('Dados da sessão perdidos. Volte ao Step4 e tente novamente.');
                return;
            }

            const response = await fetch('http://localhost:8080/api/signature/sign', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    phoneNumber: formattedPhone,
                    pin: savedPin,
                    docName: docName
                }),
            });

            if (response.ok) {
                alert('Código reenviado com sucesso!');
            } else {
                let errorMessage = 'Erro ao reenviar código';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                } catch (jsonError) {
                    errorMessage = `Erro ${response.status}: ${response.statusText}`;
                }
                setError(errorMessage);
            }
        } catch (error) {
            if (error instanceof TypeError && error.message.includes('fetch')) {
                setError('Erro de conexão. Verifique se o backend está a funcionar em http://localhost:8080');
            } else if (error instanceof Error) {
                setError(error.message);
            } else {
                setError('Erro ao reenviar código. Tente novamente.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleBack = () => {
        navigate('/step4');
    };

    return (
        <div className="center-content">
            <div className="rounded-lg shadow-md p-12 max-w-2xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Validação por SMS
                </h2>

                <p className="text-gray-700 mb-8 text-lg leading-relaxed text-center">
                    O processo de assinatura foi iniciado e foi enviado um código OTP por SMS 
                    para o seu número de telemóvel. Introduza o código recebido para completar a assinatura.
                </p>

                {/* Informação do número */}
                {phoneNumber && (
                    <div className="w-full mb-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
                        <div className="text-center">
                            <p className="text-sm text-blue-700">
                                Código enviado para: <strong>+351 {phoneNumber}</strong>
                            </p>
                        </div>
                    </div>
                )}

                {/* Campo OTP */}
                <div className="w-full mb-6">
                    <label htmlFor="otp" className="block text-sm font-medium text-gray-700 mb-2">
                        Código OTP
                    </label>
                    <input
                        type="text"
                        id="otp"
                        value={otpCode}
                        onChange={handleOtpChange}
                        placeholder="123456"
                        maxLength={6}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg text-center tracking-widest font-mono"
                        disabled={isLoading}
                    />
                    <p className="text-xs text-gray-500 mt-1 text-center">
                        Introduza o código de 4-6 dígitos recebido por SMS
                    </p>
                </div>

                {/* Mensagem de erro */}
                {error && (
                    <div className="w-full mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                        <div className="flex items-center">
                            <p className="text-red-700 text-sm">{error}</p>
                        </div>
                    </div>
                )}

                {/* Informação sobre segurança */}
                <div className="w-full mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-start">
                        <div>
                            <p className="text-green-800 text-sm font-medium mb-1">Segurança</p>
                            <p className="text-green-700 text-xs leading-relaxed">
                                O código OTP é válido apenas por alguns minutos e garante que 
                                apenas você pode completar a assinatura do documento.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Ajuda se não recebeu o código */}
                <div className="w-full mb-8 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                    <div className="flex items-start">
                        <div>
                            <p className="text-yellow-800 text-sm font-medium mb-1">Não recebeu o código?</p>
                            <ul className="text-yellow-700 text-xs leading-relaxed space-y-1">
                                <li>• Verifique se o número está correto: +351 {phoneNumber}</li>
                                <li>• Aguarde alguns minutos, pode haver delay</li>
                                <li>• Verifique a pasta de SMS ou mensagens filtradas</li>
                                <li>• Certifique-se que tem cobertura de rede</li>
                                <li>• Use o botão "Reenviar Código" se necessário</li>
                            </ul>
                        </div>
                    </div>
                </div>

                {/* Botões de ação */}
                <div className="flex flex-col gap-4 w-full">
                    <button
                        onClick={handleValidate}
                        disabled={isLoading || !otpCode}
                        className="bg-blue-700 hover:bg-blue-800 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center"
                    >
                        {isLoading ? (
                            <>
                                <div className="loading-spinner mr-2"></div>
                                VALIDANDO...
                            </>
                        ) : (
                            'VALIDAR CÓDIGO'
                        )}
                    </button>

                    {/* Botão para reenviar código */}
                    <button
                        onClick={handleResendCode}
                        disabled={isLoading}
                        className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-2 px-6 rounded-md transition-colors duration-200 disabled:opacity-50"
                    >
                        REENVIAR CÓDIGO
                    </button>

                    {/* Botão voltar */}
                    <button
                        onClick={handleBack}
                        className="bg-gray-200 hover:bg-gray-300 text-gray-600 font-semibold py-2 px-6 rounded-md transition-colors duration-200"
                    >
                        VOLTAR
                    </button>
                </div>
            </div>
        </div>
    );
}
