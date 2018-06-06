
package org.glassfish.fighterfish.jpaservice.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 
 *                                 Configuration of a persistence unit.
 * 
 *                             
 * 
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="provider" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="non-jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mapping-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="jar-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="class" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="exclude-unlisted-classes" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="shared-cache-mode" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-caching-type" minOccurs="0"/>
 *         &lt;element name="validation-mode" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-validation-mode-type" minOccurs="0"/>
 *         &lt;element name="properties" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="property" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="transaction-type" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-transaction-type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "description",
    "provider",
    "jtaDataSource",
    "nonJtaDataSource",
    "mappingFile",
    "jarFile",
    "clazz",
    "excludeUnlistedClasses",
    "sharedCacheMode",
    "validationMode",
    "properties"
})
public class PersistenceUnitXml
    implements Serializable
{

    private final static long serialVersionUID = 1L;
    @XmlElement
    protected String description;
    @XmlElement
    protected String provider;
    @XmlElement(name = "jta-data-source")
    protected String jtaDataSource;
    @XmlElement(name = "non-jta-data-source")
    protected String nonJtaDataSource;
    @XmlElement(name = "mapping-file")
    protected List<String> mappingFile;
    @XmlElement(name = "jar-file")
    protected List<String> jarFile;
    @XmlElement(name = "class")
    protected List<String> clazz;
    @XmlElement(name = "exclude-unlisted-classes", defaultValue = "true")
    protected Boolean excludeUnlistedClasses;
    @XmlElement(name = "shared-cache-mode")
    protected SharedCacheMode sharedCacheMode;
    @XmlElement(name = "validation-mode")
    protected ValidationMode validationMode;        
    @XmlElement
    @XmlJavaTypeAdapter(PersistencePropertiesXmlAdapter.class)
    protected Properties properties;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "transaction-type")
    protected PersistenceUnitTransactionType transactionType;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the provider property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Sets the value of the provider property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProvider(String value) {
        this.provider = value;
    }

    /**
     * Gets the value of the jtaDataSource property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJtaDataSource() {
        return jtaDataSource;
    }

    /**
     * Sets the value of the jtaDataSource property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJtaDataSource(String value) {
        this.jtaDataSource = value;
    }

    /**
     * Gets the value of the nonJtaDataSource property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNonJtaDataSource() {
        return nonJtaDataSource;
    }

    /**
     * Sets the value of the nonJtaDataSource property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNonJtaDataSource(String value) {
        this.nonJtaDataSource = value;
    }

    /**
     * Gets the value of the mappingFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mappingFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMappingFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getMappingFile() {
        if (mappingFile == null) {
            mappingFile = new ArrayList<String>();
        }
        return this.mappingFile;
    }

    /**
     * Gets the value of the jarFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the jarFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJarFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getJarFile() {
        if (jarFile == null) {
            jarFile = new ArrayList<String>();
        }
        return this.jarFile;
    }

    /**
     * Gets the value of the clazz property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clazz property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClazz().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getClazz() {
        if (clazz == null) {
            clazz = new ArrayList<String>();
        }
        return this.clazz;
    }

    /**
     * Gets the value of the excludeUnlistedClasses property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isExcludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    /**
     * Sets the value of the excludeUnlistedClasses property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setExcludeUnlistedClasses(Boolean value) {
        this.excludeUnlistedClasses = value;
    }

    /**
     * Gets the value of the sharedCacheMode property.
     * 
     * @return
     *     possible object is
     *     {@link SharedCacheMode }
     *     
     */
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    /**
     * Sets the value of the sharedCacheMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link SharedCacheMode }
     *     
     */
    public void setSharedCacheMode(SharedCacheMode value) {
        this.sharedCacheMode = value;
    }

    /**
     * Gets the value of the validationMode property.
     * 
     * @return
     *     possible object is
     *     {@link ValidationMode }
     *     
     */
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    /**
     * Sets the value of the validationMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValidationMode }
     *     
     */
    public void setValidationMode(ValidationMode value) {
        this.validationMode = value;
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesXml }
     *     
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesXml }
     *     
     */
    public void setProperties(Properties value) {
        this.properties = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the transactionType property.
     * 
     * @return
     *     possible object is
     *     {@link PersistenceUnitTransactionType }
     *     
     */
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the value of the transactionType property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersistenceUnitTransactionType }
     *     
     */
    public void setTransactionType(PersistenceUnitTransactionType value) {
        this.transactionType = value;
    }

}
