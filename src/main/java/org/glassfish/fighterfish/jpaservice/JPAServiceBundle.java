package org.glassfish.fighterfish.jpaservice;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import static org.glassfish.fighterfish.jpaservice.JPAConstants.DEFAULT_META_PERSITENCE;
import static org.glassfish.fighterfish.jpaservice.JPAConstants.HEADER_META_PERSISTENCE;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.*;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceXml;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author thor
 */
public class JPAServiceBundle implements Serializable {

    private static final long serialVersionUID = 04_21_2014L;

    public JPAServiceBundle(BundleContext managerCtx, Bundle bundle) {
        this.managerCtx = Objects.requireNonNull(managerCtx);
        this.bundle = Objects.requireNonNull(bundle);
        
        String value = this.bundle.getHeaders().get(HEADER_META_PERSISTENCE);
        
        String headerMeta;
        if (value == null || value.isEmpty()) {
            headerMeta = DEFAULT_META_PERSITENCE;
        } else {
            headerMeta = value;
        }
        
        String[] xmlFiles = headerMeta.split("\\s*,\\s*");
        PersistenceXml p = new PersistenceXml();
        try {
            Unmarshaller um = jc.createUnmarshaller();
            if(xmlFiles != null && xmlFiles.length > 0) {
                for (String xml : xmlFiles) {
                    URL xmlUrl = this.bundle.getEntry(headerMeta);
                    if(xmlUrl == null) {
                        if(value == null) {
                            throw new JPAServiceSpecException(NO_PERSISTENCE_UNITS_DEFINED, bundle);
                        } else {
                            throw new JPAServiceSpecException(DECLARED_DESCRIPTOR_NOT_FOUND, bundle, xmlUrl);
                        }                        
                    }
                    PersistenceXml tmpPersistence; 

                    // Validate schema spec#127.4.3
                    XMLReader reader = XMLReaderFactory.createXMLReader();                    
                    XMLFilterImpl filter = new XMLFilterImpl() {
                        @Override
                        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                            super.startElement("", localName, qName, atts);
                        }
                    };
                    filter.setParent(reader);
                    
                    //StreamSource xmlSource = new StreamSource(xmlUrl.openStream());
                    SAXSource saxSource = new SAXSource(filter, new InputSource(xmlUrl.openStream()));
                    //XMLStreamReader xsr = new XMLStreamFilterImplxif.createXMLStreamReader(xmlUrl.openStream());                     
                    tmpPersistence = (PersistenceXml) um.unmarshal(saxSource);

                    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);                    
                    Schema s;
                    Validator va;
                    try {
                        switch(tmpPersistence.getVersion()) {
                            case "2.1":
                                    s = sf.newSchema(this.getClass().getResource("/persistence_2_1.xsd"));
                                    va = s.newValidator();                                                                    
                                break;
                            case "2.0":
                                    s = sf.newSchema(this.getClass().getResource("/persistence_2_0.xsd"));
                                    va = s.newValidator();                               
                                break;                            
                           case "1.0":
                                    s = sf.newSchema(this.getClass().getResource("/persistence_1_0.xsd"));
                                    va = s.newValidator();                                
                                break;                            
                            default:
                                throw new JPAServiceSpecException(DESCRIPTOR_VERSION_INVALID, bundle);
                        }                        
                        
                        va.validate(new StreamSource(xmlUrl.openStream()));
                    }catch(SAXException ex) {
                        throw new JPAServiceSpecException(DESCRIPTOR_VALIDATION_FAILED, bundle, ex, xmlUrl, ex.getMessage());
                    }
                    p.getPersistenceUnit().addAll(tmpPersistence.getPersistenceUnit());
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            }
            this.descriptor = new PersistenceDescriptor(bundle);
            this.descriptor.setPersistence(p);
            this.emfServices = new HashSet<>();
        } catch (JAXBException | IOException | SAXException ex) {
            ex.printStackTrace();
            throw new JPAServiceSpecException(FAILED_READING_DESCRIPTOR, bundle, ex);
        }
        if (this.descriptor.getPersistence().getPersistenceUnit().isEmpty()) {
            throw new JPAServiceSpecException(NO_PERSISTENCE_UNITS_DEFINED, this.bundle);
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public PersistenceDescriptor getDescriptor() {
        return descriptor;
    }

    public void init() {
        if(this.emfs != null) {
            throw new IllegalStateException("Cannot initialize JPA Service Bundle: Already initialized");
        }        
        LOG.debug("Initializing JPA Service Bundle");
        synchronized (this.lock) {
            this.emfs = new HashMap<>();       
            PersistenceProvider providerService = this.managerCtx.getService(provider);
            if(providerService != null) {
                try {
                    for (PersistenceUnitInfo pui : this.descriptor.getInfos()) {
                        if (Thread.currentThread().isInterrupted()) {
                            LOG.debug("Interupted... Returning");
                            return;
                        }                               
                        try {
                            
                            if(pui.getJtaDataSource() == null) {
                                LOG.warn("JPA Service bundle persistence unit {} has no JTA DataSource", pui.getPersistenceUnitName());
                            }
                            
                            this.handleSpecialJpaIntegration(pui);
                            
                            EntityManagerFactory emf = providerService.createContainerEntityManagerFactory(pui, pui.getProperties());                                   
                            this.emfs.put(pui, emf);
                            
                            Hashtable<String, Object> props = new Hashtable<>();
                            
                            props.put(JPAConstants.OSGI_UNIT_NAME, pui.getPersistenceUnitName());
                            props.put(JPAConstants.OSGI_UNIT_VERSION, this.descriptor.getPersistence().getVersion());
                            props.put(JPAConstants.OSGI_UNIT_PROVIDER, pui.getPersistenceProviderClassName());
                            props.put(JPAConstants.MANAGED_PERSISTENCE_CONTEXT, true);
                            this.emfServices.add(this.managerCtx.registerService(EntityManagerFactory.class, emf, props));
                            LOG.info("Registered EntityManagerFactory service with the configuration: {}", props);
                        }catch(Exception e) {
                            throw new JPAServiceSpecException(FAILED_PROCESSING_DESCRIPTOR, this.bundle, e, pui.getPersistenceUnitName());
                        }
                    }
                } finally {
                    this.managerCtx.ungetService(provider);
                }
            } else {
                throw new JPAServiceSpecException(NO_PERSISTENCE_PROVIDER_AVAILABLE, this.bundle);
            }
            this.initialized = true;
        }
    }

    public void uninit() {        
        LOG.debug("Uninitializing JPA Service Bundle");            
        synchronized (this.lock) {
            if(this.emfServices != null) {
                for(ServiceRegistration<EntityManagerFactory> reg : emfServices) {
                    LOG.debug("EntityManagerFactory service {} for Persistence Unit \"{}\" to be unregistered", reg, reg.getReference().getProperty(JPAConstants.OSGI_UNIT_NAME));
                    try {
                        reg.unregister();
                    } catch(Exception e) {
                        LOG.debug("EntityManagerFactory service {} for Persistence Unit \"{}\" failed to be unregistered", e);
                    }
                }
                emfServices.clear();
                emfServices = null;
            }
            if(this.emfs != null) {
                for(Map.Entry<PersistenceUnitInfo, EntityManagerFactory> emf : emfs.entrySet()) {
                    try {                    
                        if(emf.getValue().isOpen()) {                        
                            emf.getValue().close();
                            LOG.debug("EntityManagerFactory {} was closed", emf.getValue());
                        }
                    }catch(Exception e) {
                        PersistenceUnitInfo pu = emf.getKey();
                        LOG.error("Failure closing EntityManagerFactory for Persistence Unit \"{}\" in bundle {}", pu.getPersistenceUnitName(), this.bundle);
                    }
                }
                emfs.clear();
                emfs = null;
            }
            this.initialized = false;
        }            
        LOG.debug("Uninitialization complete");
    }

    protected DataSource lookupDS(String jndi) {
        try {
            InitialContext ic = new InitialContext();
            return (DataSource) ic.lookup("java:comp/" + jndi);
        } catch (NamingException ex) {
            return null;
        }
    }

    /**
     * Updating providers causes JPA Service Bundle to uninit (if necessary) and (re) initialize
     * 
     * @param provider 
     */
    public void updateProvider(ServiceReference<PersistenceProvider> provider) {
        synchronized (this.lock) {
            if(this.initialized) {
                this.uninit();    
            }
            this.setPersistenceProvider(provider);
            this.init();
        }
    }

    public ServiceReference<PersistenceProvider> getPersistenceProvider() {
        return provider;
    }

    public void setPersistenceProvider(ServiceReference<PersistenceProvider> provider) {
        this.provider = provider;
    }        
    
    
    protected void handleSpecialJpaIntegration(PersistenceUnitInfo pui) {
        //providers
        final String ECLIPSELINK_PROVIDER = "org.eclipse.persistence.jpa.PersistenceProvider";
        final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";
        
        switch(pui.getPersistenceProviderClassName()) {
            case ECLIPSELINK_PROVIDER:
                final String ECLIPSELINK_PLATFORM_TARGET_SERVER = "eclipselink.target-server"; 
                    //This should become fixed in EclipseLink 2.6 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=248328)
                    if(System.getProperty("eclipselink.security.usedoprivileged") == null) {
                        System.setProperty("eclipselink.security.usedoprivileged", Boolean.TRUE.toString());
                    }        
                    //This is necessary to get EclipseLink to integrate properly with host container (https://eclipse.org/eclipselink/documentation/2.4/jpa/extensions/p_target_server.htm)
                    if(!pui.getProperties().contains(ECLIPSELINK_PLATFORM_TARGET_SERVER)) {
                        pui.getProperties().put(ECLIPSELINK_PLATFORM_TARGET_SERVER, System.getProperty("ECLIPSELINK_PLATFORM_TARGET_SERVER", "SunAS9"));
                    }
                break;
            case HIBERNATE_PROVIDER:
                final String HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS = "hibernate.transaction.manager_lookup_class";
                final String HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_GF = "org.hibernate.transaction.SunONETransactionManagerLookup";
                final String HIBERNATE_JTA_PLATFORM = "hibernate.transaction.jta.platform";
                final String HIBERNATE_JTA_PLATFORM_SUN_ONE_JTA_PLATFORM = "org.hibernate.service.jta.platform.internal.SunOneJtaPlatform";
                if(!pui.getProperties().contains(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_GF)) {
                    pui.getProperties().put(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS, HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_GF);
                }
                if(!pui.getProperties().contains(HIBERNATE_JTA_PLATFORM)) {
                    pui.getProperties().put(HIBERNATE_JTA_PLATFORM, HIBERNATE_JTA_PLATFORM_SUN_ONE_JTA_PLATFORM);
                }
                break;
        }      
    }
    
    private final BundleContext managerCtx;    
    private final Bundle bundle;
    
    private PersistenceDescriptor descriptor;
    
    private ServiceReference<PersistenceProvider> provider;
    
    private Map<PersistenceUnitInfo, EntityManagerFactory> emfs;    
    private Set<ServiceRegistration<EntityManagerFactory>> emfServices;
    
    protected boolean initialized = false;
    
    private final Object lock = new Object();
            
    //private JPABundleManager manager;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JPAServiceBundle.class);
    
    private static JAXBContext jc;

    static {
        try {
            jc = JAXBContext.newInstance(PersistenceXml.class);
        } catch (JAXBException ex) {
            throw new Error("Failed initializing JAXBContext");
        }
        
    }
    
}
