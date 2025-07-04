
package pt.ipvc.cartao.ccauth.soap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignDocumentPoolingResult" type="{http://schemas.datacontract.org/2004/07/Ama.Structures.CCMovelSignature}SignResponse" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "signDocumentPoolingResult"
})
@XmlRootElement(name = "SignDocumentPoolingResponse")
public class SignDocumentPoolingResponse {

    @XmlElementRef(name = "SignDocumentPoolingResult", namespace = "http://Ama.Authentication.Service/", type = JAXBElement.class, required = false)
    protected JAXBElement<SignResponse> signDocumentPoolingResult;

    /**
     * Gets the value of the signDocumentPoolingResult property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link SignResponse }{@code >}
     *     
     */
    public JAXBElement<SignResponse> getSignDocumentPoolingResult() {
        return signDocumentPoolingResult;
    }

    /**
     * Sets the value of the signDocumentPoolingResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link SignResponse }{@code >}
     *     
     */
    public void setSignDocumentPoolingResult(JAXBElement<SignResponse> value) {
        this.signDocumentPoolingResult = value;
    }

}
