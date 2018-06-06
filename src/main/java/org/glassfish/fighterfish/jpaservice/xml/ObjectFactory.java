
package org.glassfish.fighterfish.jpaservice.xml;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.glassfish.fighterfish.jpaservice.xml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of 
     * schema derived classes for package: org.glassfish.fighterfish.jpaservice.xml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PersistenceXml }
     * 
     */
    public PersistenceXml createPersistenceXml() {
        return new PersistenceXml();
    }

    /**
     * Create an instance of {@link PersistenceUnitXml }
     * 
     */
    public PersistenceUnitXml createPersistenceUnitXml() {
        return new PersistenceUnitXml();
    }

    /**
     * Create an instance of {@link PropertiesXml }
     * 
     */
    public PropertiesXml createPropertiesXml() {
        return new PropertiesXml();
    }

    /**
     * Create an instance of {@link PropertyXml }
     * 
     */
    public PropertyXml createPropertyXml() {
        return new PropertyXml();
    }

}
