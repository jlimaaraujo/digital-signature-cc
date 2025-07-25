import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { FaRegEye } from "react-icons/fa";
import { FaEyeSlash } from "react-icons/fa";
import './Step4Signature.css';
import { useNavigationGuard } from '../contexts/NavigationGuardContext';

export default function Step4Signature() {
    const navigate = useNavigate();
    const [setDocumentInfo] = useState<any>(null);
    const [phoneNumber, setPhoneNumber] = useState<string>('');
    const [pin, setPin] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [showPin, setShowPin] = useState<boolean>(false);
    const { setAllowedStep } = useNavigationGuard();

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

        // Recuperar configurações de assinatura do localStorage
        const signatureConfigData = localStorage.getItem('signatureConfig');
        if (signatureConfigData) {
            try {
                const signatureConfig = JSON.parse(signatureConfigData);
                // Guardar para uso posterior no Step5
                localStorage.setItem('hasVisualSignature', JSON.stringify(signatureConfig.showVisualSignature || false));
                localStorage.setItem('signaturePage', JSON.stringify(signatureConfig.page || 1));
                localStorage.setItem('signatureXPercent', JSON.stringify(signatureConfig.position?.xPercent || 50));
                localStorage.setItem('signatureYPercent', JSON.stringify(signatureConfig.position?.yPercent || 50));

                // Tratar motivo e local corretamente - só salvar se existirem e não forem vazios
                const motivo = signatureConfig.motivo && signatureConfig.motivo.trim() ? signatureConfig.motivo.trim() : null;
                const local = signatureConfig.local && signatureConfig.local.trim() ? signatureConfig.local.trim() : null;

                localStorage.setItem('signatureMotivo', JSON.stringify(motivo));
                localStorage.setItem('signatureLocal', JSON.stringify(local));

            } catch (error) {
                // Se não conseguir fazer parse, usar valores padrão
                localStorage.setItem('hasVisualSignature', JSON.stringify(false));
                localStorage.setItem('signaturePage', JSON.stringify(1));
                localStorage.setItem('signatureXPercent', JSON.stringify(50));
                localStorage.setItem('signatureYPercent', JSON.stringify(50));
                localStorage.setItem('signatureMotivo', JSON.stringify(null));
                localStorage.setItem('signatureLocal', JSON.stringify(null));
            }
        } else {
            // Se não houver configuração salva, usar valores padrão
            localStorage.setItem('hasVisualSignature', JSON.stringify(false));
            localStorage.setItem('signaturePage', JSON.stringify(1));
            localStorage.setItem('signatureXPercent', JSON.stringify(50));
            localStorage.setItem('signatureYPercent', JSON.stringify(50));
            localStorage.setItem('signatureMotivo', JSON.stringify(null));
            localStorage.setItem('signatureLocal', JSON.stringify(null));
        }
    }, []);

    const handleBack = () => {
        navigate('/step4');
    };

    const handleSign = async () => {
        if (!phoneNumber || !pin) {
            setError('Por favor, preencha todos os campos');
            return;
        }

        // Remove espaços para validar apenas os dígitos
        const phoneDigits = phoneNumber.replace(/\s/g, '');
        const formattedPhone = "+351" + phoneDigits;
        if (phoneDigits.length !== 9) {
            setError('O número de telemóvel deve ter 9 dígitos');
            return;
        }

        if (pin.length < 4 || pin.length > 6) {
            setError('O PIN deve ter entre 4 a 6 dígitos');
            return;
        }

        setIsLoading(true);
        setError('');

        try {
            // Guardar dados no localStorage para uso posterior
            localStorage.setItem('phoneNumber', phoneDigits);
            localStorage.setItem('userPin', pin);

            // Recuperar a configuração de assinatura completa
            const signatureConfigData = localStorage.getItem('signatureConfig');
            let hasVisualSignature = false;
            if (signatureConfigData) {
                try {
                    const signatureConfig = JSON.parse(signatureConfigData);
                    hasVisualSignature = signatureConfig.showVisualSignature || false;
                } catch (error) {
                    // Falha silenciosa ao carregar configuração
                }
            }
            localStorage.setItem('hasVisualSignature', JSON.stringify(hasVisualSignature));

            // Chamar endpoint para assinar documento
            const documentInfo = localStorage.getItem('documentInfo');
            let docName = 'Contrato Teste';

            if (documentInfo) {
                try {
                    const parsedData = JSON.parse(documentInfo);
                    docName = parsedData.name || 'Documento';
                } catch (error) {
                    // Se não conseguir fazer parse, usar nome padrão
                }
            }

            const signResponse = await fetch('http://localhost:8080/api/signature/sign', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    phoneNumber: formattedPhone,
                    pin: pin,
                    docName: docName
                }),
            });

            if (!signResponse.ok) {
                let errorMessage = 'Erro ao assinar documento';
                try {
                    const signError = await signResponse.json();
                    errorMessage = signError.message || errorMessage;
                } catch (jsonError) {
                    // Usar mensagem genérica em caso de erro
                    errorMessage = 'Erro ao assinar documento';
                }
                throw new Error(errorMessage);
            }

            // Guardar o processId retornado para uso posterior
            const processId = await signResponse.text();
            localStorage.setItem('processId', processId);
            // Log removido em produção

            setAllowedStep(6); // Permitir navegação para o passo 6
            navigate('/step6');
        } catch (error) {
            // Tratamento genérico de erros sem exposição de detalhes técnicos
            if (error instanceof Error) {
                setError(error.message);
            } else {
                setError('Erro ao processar assinatura. Tente novamente.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    const formatPhoneNumber = (value: string) => {
        // Remove tudo que não for dígito
        const digits = value.replace(/\D/g, '');

        // Limita a 9 dígitos
        const limitedDigits = digits.slice(0, 9);

        // Formata como XXX XXX XXX
        if (limitedDigits.length <= 3) {
            return limitedDigits;
        } else if (limitedDigits.length <= 6) {
            return `${limitedDigits.slice(0, 3)} ${limitedDigits.slice(3)}`;
        } else {
            return `${limitedDigits.slice(0, 3)} ${limitedDigits.slice(3, 6)} ${limitedDigits.slice(6)}`;
        }
    };

    const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const formatted = formatPhoneNumber(e.target.value);
        setPhoneNumber(formatted);
        if (error) setError('');
    };

    const handlePinChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value.replace(/\D/g, '').slice(0, 6);
        setPin(value);
        if (error) setError('');
    };

    const togglePinVisibility = () => {
        setShowPin(!showPin);
    };

    return (
        <div className="center-content">
            <div className="rounded-lg shadow-md p-12 max-w-2xl flex flex-col items-center">
                <h2 className="text-2xl font-bold text-blue-600 mb-8">
                    Assinatura Digital
                </h2>

                <p className="text-gray-700 mb-8 text-lg leading-relaxed text-center">
                    Para assinar o documento digitalmente, introduza os seus dados da Chave Móvel Digital.
                </p>

                {/* Formulário de assinatura */}
                <div className="forms w-full space-y-6">
                    {/* Número de telemóvel */}
                    <div className="flex items-center gap-4">
                        <label htmlFor="phone" className="label-phone text-sm font-medium text-gray-700 whitespace-nowrap">
                            <b>Nº de telemóvel:</b>
                        </label>
                        <span className="text-xs text-gray-500">
                            Introduza o número associado à sua Chave Móvel Digital
                        </span>
                        <input
                            type="tel"
                            id="phone"
                            value={phoneNumber}
                            onChange={handlePhoneChange}
                            placeholder="9XX XXX XXX"
                            className="px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg"
                            style={{ width: '180px' }}
                            disabled={isLoading}
                        />
                    </div>

                    {/* PIN */}
                    <div className="flex items-center gap-4">
                        <label htmlFor="pin" className="label-pin text-sm font-medium text-gray-700 whitespace-nowrap">
                            <b>PIN:</b>
                        </label>
                        <span className="text-xs text-gray-500">
                            PIN de 4 a 6 dígitos da sua Chave Móvel Digital
                        </span>
                        <div className="relative">
                            <input
                                type={showPin ? "text" : "password"}
                                id="pin"
                                value={pin}
                                onChange={handlePinChange}
                                placeholder="••••••"
                                maxLength={6}
                                className="px-4 py-3 pr-12 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg text-center tracking-widest"
                                style={{ width: '180px' }}
                                disabled={isLoading}
                            />
                            <button
                                type="button"
                                onClick={togglePinVisibility}
                                className="toggle-visibility absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700 focus:outline-none"
                                disabled={isLoading}
                            >
                                {showPin ? <FaEyeSlash size={18} /> : <FaRegEye size={18} />}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Mensagem de erro */}
                {error && (
                    <div className="w-full mt-6 flex justify-center">
                        <p style={{ color: '#dc2626', fontWeight: 'bold', fontSize: '1rem', textAlign: 'center' }}>{error}</p>
                    </div>
                )}

                {/* Informação sobre o processo */}
                <div className="w-full mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="flex items-start">
                        <div>
                            <p className="text-blue-700 text-xs leading-relaxed">
                                Após clicar em "Assinar Documento", será enviado um código OTP por SMS
                                para o seu número de telemóvel para validar a assinatura.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Botões de navegação */}
                <div className="flex gap-4 mt-8">
                    <button
                        onClick={handleBack}
                        className="bg-gray-300 hover:bg-gray-400 text-gray-700 font-semibold py-3 px-8 rounded-md transition-colors duration-200"
                        disabled={isLoading}
                    >
                        VOLTAR
                    </button>
                    <button
                        onClick={handleSign}
                        disabled={isLoading}
                        className="bg-blue-700 hover:bg-blue-800 text-white font-semibold py-3 px-8 rounded-md transition-colors duration-200 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center"
                    >
                        {isLoading ? (
                            <>
                                <div className="loading-spinner mr-2"></div>
                                A ASSINAR E A ENVIAR OTP...
                            </>
                        ) : (
                            'ASSINAR DOCUMENTO'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}