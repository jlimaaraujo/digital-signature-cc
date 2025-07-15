import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useNavigationGuard } from '../contexts/NavigationGuardContext';
import './Step5OTP.css';

export default function Step5OTP() {
    const navigate = useNavigate();
    const { setAllowedStep, resetFlow } = useNavigationGuard();
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
            // Recuperar todas as configurações de assinatura
            const hasVisualSignature = localStorage.getItem('hasVisualSignature');
            const showVisual = hasVisualSignature ? JSON.parse(hasVisualSignature) : false;

            // Recuperar informações de posição e página
            const signaturePage = localStorage.getItem('signaturePage');
            const signatureXPercent = localStorage.getItem('signatureXPercent');
            const signatureYPercent = localStorage.getItem('signatureYPercent');

            const page = signaturePage ? JSON.parse(signaturePage) : 1;
            const xPercent = signatureXPercent ? JSON.parse(signatureXPercent) : 50;
            const yPercent = signatureYPercent ? JSON.parse(signatureYPercent) : 50;

            // CORREÇÃO CRÍTICA: Recuperar motivo e local corretamente
            const motivoSalvo = localStorage.getItem('signatureMotivo');
            const localSalvo = localStorage.getItem('signatureLocal');
            
            // Parse dos valores salvos - podem ser null, string vazia ou valor real
            let motivo = null;
            let local = null;
            
            if (motivoSalvo && motivoSalvo !== 'null') {
                try {
                    const parsed = JSON.parse(motivoSalvo);
                    if (parsed && typeof parsed === 'string' && parsed.trim() !== '') {
                        motivo = parsed.trim();
                    }
                } catch (e) {
                    // Se não conseguir fazer parse, usar como string direta
                    if (motivoSalvo.trim() !== '' && motivoSalvo !== 'null') {
                        motivo = motivoSalvo.trim();
                    }
                }
            }
            
            if (localSalvo && localSalvo !== 'null') {
                try {
                    const parsed = JSON.parse(localSalvo);
                    if (parsed && typeof parsed === 'string' && parsed.trim() !== '') {
                        local = parsed.trim();
                    }
                } catch (e) {
                    // Se não conseguir fazer parse, usar como string direta
                    if (localSalvo.trim() !== '' && localSalvo !== 'null') {
                        local = localSalvo.trim();
                    }
                }
            }

            // Recuperar nome do documento
            const documentInfo = localStorage.getItem('documentInfo');
            let documentName = 'Documento';
            if (documentInfo) {
                try {
                    const parsedData = JSON.parse(documentInfo);
                    documentName = parsedData.name || 'Documento';
                } catch (error) {
                    // Falha silenciosa ao carregar nome do documento
                }
            }

            // Preparar payload para o backend - INCLUINDO MOTIVO E LOCAL
            const otpPayload = {
                otp: otpCode,
                documentName: documentName,
                hasVisualSignature: showVisual,
                signaturePage: page,
                signatureXPercent: xPercent,
                signatureYPercent: yPercent,
                motivo: motivo,  // ADICIONADO
                local: local     // ADICIONADO
            };

            // Chamar o endpoint de validação OTP com todos os dados necessários
            const response = await fetch('http://localhost:8080/api/signature/validate-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(otpPayload),
            });

            if (response.ok) {
                // O endpoint retorna diretamente o PDF assinado
                const blob = await response.blob();

                // Salvar o blob no localStorage para o Step6 fazer download
                const reader = new FileReader();
                reader.onload = function () {
                    const arrayBuffer = reader.result as ArrayBuffer;
                    const uint8Array = new Uint8Array(arrayBuffer);
                    const binaryString = Array.from(uint8Array).map(byte => String.fromCharCode(byte)).join('');
                    const base64String = btoa(binaryString);
                    localStorage.setItem('signedPdfBlob', base64String);

                    setAllowedStep(7); // Permitir navegação para o passo 7
                    navigate('/step7');
                };
                reader.readAsArrayBuffer(blob);
            } else {
                let errorMessage = 'Código OTP inválido ou expirado';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                } catch (jsonError) {
                    // Usar mensagem genérica em caso de erro de parsing
                    errorMessage = 'Código OTP inválido ou expirado';
                }
                setError(errorMessage);
            }
        } catch (error) {
            // Tratamento genérico de erros sem exposição de detalhes técnicos
            setError('Erro de conexão. Tente novamente.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleResendCode = async () => {
        setIsLoading(true);
        setError('');

        try {
            // Recuperar dados necessários do localStorage
            const processId = localStorage.getItem('processId');
            const phoneDigits = phoneNumber.replace(/\s/g, '');
            const citizenId = "+351" + phoneDigits; // O citizenId deve incluir o prefixo +351

            if (!processId) {
                setError('Dados da sessão perdidos. Volte ao Step4 e tente novamente.');
                return;
            }

            const response = await fetch('http://localhost:8080/api/signature/resend-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    processId: processId,
                    citizenId: citizenId
                }),
            });

            if (response.ok) {
                await response.json(); // Consumir resposta
                alert('Código reenviado com sucesso!');
            } else {
                let errorMessage = 'Erro ao reenviar código';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                    // Log removido em produção
                } catch (jsonError) {
                    // Usar mensagem genérica em caso de erro
                    errorMessage = 'Erro ao reenviar código';
                }
                setError(errorMessage);
            }
        } catch (error) {
            setError('Erro ao reenviar código. Tente novamente.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleBack = () => {
        resetFlow(); // Reiniciar o fluxo de assinatura
    };

    return (
        <div className="center-content">
            <div className="rounded-lg shadow-md p-12 max-w-2xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Validação por SMS
                </h2>

                <p className="text-gray-700 mb-8 text-lg leading-relaxed text-center">
                    O processo de assinatura foi iniciado e foi enviado um código OTP por SMS
                    para <b>{phoneNumber}</b>. Introduza o código recebido para completar a assinatura.
                </p>

                {/* Informação sobre segurança */}
                <div className="w-full mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-start">
                        <div>
                            <p className="text-green-700 text-xs leading-relaxed">
                                <b>Aviso:</b> O código OTP é válido apenas por 2 minutos e garante que
                                apenas você pode completar a assinatura do documento.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="forms w-full space-y-6">
                    {/* Campo OTP */}
                    <div className="w-full mb-6">
                        <label htmlFor="otp" className="block text-sm font-medium text-gray-700 mb-2">
                            <b>Código OTP:</b>
                        </label>
                        <span className="text-xs text-gray-500 mt-1 text-center">
                            Introduza o código OTP recebido por SMS
                        </span>
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
                    </div>
                    {/* Mensagem de erro */}
                    {error && (
                        <div className="error-message w-full mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                            <p className="text-red-700 text-sm">{error}</p>
                        </div>
                    )}
                </div>

                {/* Botões de ação */}
                <div className="flex flex-col gap-4 w-full">
                    {/* Botão cancelar */}
                    <button
                        onClick={handleBack}
                        className="button-cancel bg-gray-200 hover:bg-gray-300 text-gray-600 font-semibold py-2 px-6 rounded-md transition-colors duration-200"
                    >
                        CANCELAR
                    </button>
                    {/* Botão para reenviar código */}
                    <button
                        onClick={handleResendCode}
                        disabled={isLoading}
                        className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-2 px-6 rounded-md transition-colors duration-200 disabled:opacity-50"
                    >
                        REENVIAR CÓDIGO
                    </button>
                    <button
                        onClick={handleValidate}
                        disabled={isLoading || !otpCode}
                        className="bg-blue-700 hover:bg-blue-800 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center"
                    >
                        {isLoading ? (
                            <>
                                <div className="loading-spinner mr-2"></div>
                                A VALIDAR...
                            </>
                        ) : (
                            'VALIDAR CÓDIGO'
                        )}
                    </button>


                </div>
            </div>
        </div>
    );
}