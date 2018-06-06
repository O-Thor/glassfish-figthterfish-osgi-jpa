package org.glassfish.fighterfish.jpaservice.managed;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import static javax.persistence.SynchronizationType.SYNCHRONIZED;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class ManagedEntityManagerFactory implements EntityManagerFactory {

    public ManagedEntityManagerFactory(ServiceReference<EntityManagerFactory> serviceRefUnit, Map<String, Object> props, ManagedPersistenceContextManager persistenceRegistry) {
        this.emfRef = serviceRefUnit;
        this.properties = new HashMap<>(props);
        this.contextManager = persistenceRegistry;
    }
    
    /**
     * Lookup EMF service
     *
     * @return
     */
    protected EntityManagerFactory getEMF() {
        return (EntityManagerFactory) emfRef.getBundle().getBundleContext().getService(emfRef);
    }    
    
    protected void ungetEMF() {
        emfRef.getBundle().getBundleContext().ungetService(emfRef);
    }

    public EntityManager createEntityManager() {        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating a container managed entity manager for the perstence unit {} with the following properties {}", emfRef, properties);
        }
        try {
            return new ManagedEntityManager(emfRef, null, this.contextManager);
        } finally {
            this.ungetEMF();
        }        
    }

    public void close() {
        throw new UnsupportedOperationException("Managed persistence context cannot be closed");
    }

    public EntityManager createEntityManager(Map map) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating a container managed entity manager for the perstence unit {} with the following properties {}", emfRef, properties);
        }
        Map emMap = new HashMap(properties);
        emMap.putAll(map);
        return new ManagedEntityManager(this.emfRef, emMap, contextManager);
    }

    public Cache getCache() {
        try {
        return getEMF().getCache();
        } finally {
            this.ungetEMF();
        }        
    }

    public CriteriaBuilder getCriteriaBuilder() {
        try {
            return getEMF().getCriteriaBuilder();
        } finally {
            this.ungetEMF();
        }  
    }

    public Metamodel getMetamodel() {
        try {
            return getEMF().getMetamodel();
        } finally {
            this.ungetEMF();
        }  
    }

    public PersistenceUnitUtil getPersistenceUnitUtil() {
        try {
            return getEMF().getPersistenceUnitUtil();
        } finally {
            this.ungetEMF();
        } 
    }

    public Map<String, Object> getProperties() {
        try {
            return getEMF().getProperties();
        } finally {
            this.ungetEMF();
        } 
    }

    public boolean isOpen() {
        try {
            return getEMF().isOpen();
        } finally {
            this.ungetEMF();
        } 
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        if(synchronizationType != SYNCHRONIZED) {
            throw new UnsupportedOperationException("Managed persistence context only supports synchronized");
        }
        return this.createEntityManager();
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        if(synchronizationType != SYNCHRONIZED) {
            throw new UnsupportedOperationException("Managed persistence context only supports synchronized");
        }
        return this.createEntityManager(map);
    }

    public void addNamedQuery(String name, Query query) {
        try {
            getEMF().addNamedQuery(name, query);
        } finally {
            this.ungetEMF();
        } 
    }

    public <T> T unwrap(Class<T> cls) {
        try {
            return getEMF().unwrap(cls);
        } finally {
            this.ungetEMF();
        } 
    }

    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        try {
            getEMF().addNamedEntityGraph(graphName, entityGraph);
        } finally {
            this.ungetEMF();
        } 
    }

    @Override
    public String toString() {
        return super.toString() + "[Managed-JPAService]";
    }        

    private final ServiceReference<EntityManagerFactory> emfRef;
    private final Map<String, Object> properties;
    private final ManagedPersistenceContextManager contextManager;
    
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger("org.glassfish.fighterfish.osgijpa");
}
