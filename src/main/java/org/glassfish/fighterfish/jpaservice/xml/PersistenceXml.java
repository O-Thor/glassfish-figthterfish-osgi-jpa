
package org.glassfish.fighterfish.jpaservice.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="persistence-unit" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                   &lt;element name="provider" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                   &lt;element name="jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                   &lt;element name="non-jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                   &lt;element name="mapping-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                   &lt;element name="jar-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                   &lt;element name="class" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                   &lt;element name="exclude-unlisted-classes" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *                   &lt;element name="shared-cache-mode" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-caching-type" minOccurs="0"/>
 *                   &lt;element name="validation-mode" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-validation-mode-type" minOccurs="0"/>
 *                   &lt;element name="properties" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="property" maxOccurs="unbounded" minOccurs="0">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                     &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="transaction-type" type="{http://xmlns.jcp.org/xml/ns/persistence}persistence-unit-transaction-type" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="version" use="required" type="{http://xmlns.jcp.org/xml/ns/persistence}versionType" fixed="2.1" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "persistence")
@XmlType(name = "", propOrder = {
    "persistenceUnit"
})
public class PersistenceXml
    implements Serializable
{

    private final static long serialVersionUID = 1L;
    @XmlElement(name = "persistence-unit", required = true)
    protected List<PersistenceUnitXml> persistenceUnit;
    @XmlAttribute(name = "version", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String version;

    /**
     * Gets the value of the persistenceUnit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the persistenceUnit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPersistenceUnit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PersistenceUnitXml }
     * 
     * 
     */
    public List<PersistenceUnitXml> getPersistenceUnit() {
        if (persistenceUnit == null) {
            persistenceUnit = new ArrayList<PersistenceUnitXml>();
        }
        return this.persistenceUnit;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        if (version == null) {
            return "2.1";
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

}
