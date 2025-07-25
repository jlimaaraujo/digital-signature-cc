
package pt.ipvc.cartao.ccauth.soap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MultipleSignRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MultipleSignRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ApplicationId" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/&gt;
 *         &lt;element name="IsBiometricValidationEnable" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="Pin" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="UserId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultipleSignRequest", namespace = "http://schemas.datacontract.org/2004/07/Ama.Structures.CCMovelSignature", propOrder = {
    "applicationId",
    "isBiometricValidationEnable",
    "pin",
    "userId"
})
public class MultipleSignRequest {

    @XmlElement(name = "ApplicationId", required = true, nillable = true)
    protected byte[] applicationId;
    @XmlElementRef(name = "IsBiometricValidationEnable", namespace = "http://schemas.datacontract.org/2004/07/Ama.Structures.CCMovelSignature", type = JAXBElement.class, required = false)
    protected JAXBElement<Boolean> isBiometricValidationEnable;
    @XmlElement(name = "Pin", required = true, nillable = true)
    protected String pin;
    @XmlElement(name = "UserId", required = true, nillable = true)
    protected String userId;

    /**
     * Gets the value of the applicationId property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getApplicationId() {
        return applicationId;
    }

    /**
     * Sets the value of the applicationId property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setApplicationId(byte[] value) {
        this.applicationId = value;
    }

    /**
     * Gets the value of the isBiometricValidationEnable property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getIsBiometricValidationEnable() {
        return isBiometricValidationEnable;
    }

    /**
     * Sets the value of the isBiometricValidationEnable property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setIsBiometricValidationEnable(JAXBElement<Boolean> value) {
        this.isBiometricValidationEnable = value;
    }

    /**
     * Gets the value of the pin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPin() {
        return pin;
    }

    /**
     * Sets the value of the pin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPin(String value) {
        this.pin = value;
    }

    /**
     * Gets the value of the userId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the value of the userId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserId(String value) {
        this.userId = value;
    }

}
