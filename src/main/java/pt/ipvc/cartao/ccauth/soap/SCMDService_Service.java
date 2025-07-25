
package pt.ipvc.cartao.ccauth.soap;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.3.2
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "SCMDService", targetNamespace = "http://tempuri.org/", wsdlLocation = "https://preprod.cmd.autenticacao.gov.pt/Ama.Authentication.Frontend/SCMDService.svc?wsdl")
public class SCMDService_Service
    extends Service
{

    private final static URL SCMDSERVICE_WSDL_LOCATION;
    private final static WebServiceException SCMDSERVICE_EXCEPTION;
    private final static QName SCMDSERVICE_QNAME = new QName("http://tempuri.org/", "SCMDService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("https://preprod.cmd.autenticacao.gov.pt/Ama.Authentication.Frontend/SCMDService.svc?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SCMDSERVICE_WSDL_LOCATION = url;
        SCMDSERVICE_EXCEPTION = e;
    }

    public SCMDService_Service() {
        super(__getWsdlLocation(), SCMDSERVICE_QNAME);
    }

    public SCMDService_Service(WebServiceFeature... features) {
        super(__getWsdlLocation(), SCMDSERVICE_QNAME, features);
    }

    public SCMDService_Service(URL wsdlLocation) {
        super(wsdlLocation, SCMDSERVICE_QNAME);
    }

    public SCMDService_Service(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SCMDSERVICE_QNAME, features);
    }

    public SCMDService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SCMDService_Service(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns SCMDService
     */
    @WebEndpoint(name = "BasicHttpBinding_SCMDService")
    public SCMDService getBasicHttpBindingSCMDService() {
        return super.getPort(new QName("http://tempuri.org/", "BasicHttpBinding_SCMDService"), SCMDService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns SCMDService
     */
    @WebEndpoint(name = "BasicHttpBinding_SCMDService")
    public SCMDService getBasicHttpBindingSCMDService(WebServiceFeature... features) {
        return super.getPort(new QName("http://tempuri.org/", "BasicHttpBinding_SCMDService"), SCMDService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SCMDSERVICE_EXCEPTION!= null) {
            throw SCMDSERVICE_EXCEPTION;
        }
        return SCMDSERVICE_WSDL_LOCATION;
    }

}
