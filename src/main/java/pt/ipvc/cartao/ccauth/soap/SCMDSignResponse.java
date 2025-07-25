
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
 *         &lt;element name="SCMDSignResult" type="{http://schemas.datacontract.org/2004/07/Ama.Structures.CCMovelSignature}SignStatus" minOccurs="0"/&gt;
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
    "scmdSignResult"
})
@XmlRootElement(name = "SCMDSignResponse")
public class SCMDSignResponse {

    @XmlElementRef(name = "SCMDSignResult", namespace = "http://Ama.Authentication.Service/", type = JAXBElement.class, required = false)
    protected JAXBElement<SignStatus> scmdSignResult;

    /**
     * Gets the value of the scmdSignResult property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link SignStatus }{@code >}
     *     
     */
    public JAXBElement<SignStatus> getSCMDSignResult() {
        return scmdSignResult;
    }

    /**
     * Sets the value of the scmdSignResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link SignStatus }{@code >}
     *     
     */
    public void setSCMDSignResult(JAXBElement<SignStatus> value) {
        this.scmdSignResult = value;
    }

}
