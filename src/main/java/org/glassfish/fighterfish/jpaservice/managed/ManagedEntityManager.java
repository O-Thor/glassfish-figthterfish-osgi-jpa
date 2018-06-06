/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.fighterfish.jpaservice.managed;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class ManagedEntityManager implements EntityManager {

    public ManagedEntityManager(ServiceReference<EntityManagerFactory> emfRef, Map<String, Object> properties, ManagedPersistenceContextManager contextManager) {
        this.emfRef = emfRef;
        this.properties = properties;
        this.contextManager = contextManager;
    }

/**
     * Get the target persistence context
     *
     * @param forceTransaction Whether the returned entity manager needs to be
     * bound to a transaction
     * @throws TransactionRequiredException if forceTransaction is true and no
     * transaction is available
     * @return
     */
    private EntityManager getEm(boolean forceTransaction) {        
        try {
            if (forceTransaction) {
                return contextManager.getCurrentPersistenceContext(emfRef, properties);
            } else {
                if (contextManager.isActiveTransaction()) {
                    return contextManager.getCurrentPersistenceContext(emfRef, this.properties);
                } else {
                    if (!contextManager.isJtaAvailable() & LOG.isDebugEnabled()) {
                        LOG.debug("JTA is NOT available");
                    }
                    if (nonJTAEm == null) {                    
                        synchronized (this) {                       
                            if (nonJTAEm == null) {
                                BundleContext ctx = this.emfRef.getBundle().getBundleContext();
                                EntityManagerFactory emf = ctx.getService(emfRef);
                                try {
                                    nonJTAEm = emf.createEntityManager(properties);
                                } finally {
                                    ctx.ungetService(emfRef);
                                }
                            }
                        }
                    }
                    return nonJTAEm;
                }
            }
        }catch(javax.transaction.TransactionRequiredException e) {
            throw ((RuntimeException)new TransactionRequiredException("Transaction context not active").initCause(e));
        }
    }    
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void persist(Object entity) {
        this.getEm(true).persist(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T merge(T entity) {
        return getEm(false).merge(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void remove(Object entity) {
        getEm(true).remove(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return getEm(true).find(entityClass, primaryKey);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return getEm(true).find(entityClass, primaryKey, properties);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return getEm(true).find(entityClass, primaryKey, lockMode);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return getEm(true).find(entityClass, primaryKey, lockMode, properties);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return getEm(true).getReference(entityClass, primaryKey);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void flush() {
        getEm(true).flush();
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void setFlushMode(FlushModeType flushMode) {
        getEm(true).setFlushMode(flushMode);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public FlushModeType getFlushMode() {
        return getEm(true).getFlushMode();
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void lock(Object entity, LockModeType lockMode) {
        getEm(true).lock(entity, lockMode);;
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getEm(true).lock(entity, lockMode, properties);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void refresh(Object entity) {
        getEm(true).refresh(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        getEm(true).refresh(entity, properties);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        getEm(true).refresh(entity, lockMode);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getEm(true).refresh(entity, lockMode, properties);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void clear() {
        getEm(true).clear();
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void detach(Object entity) {
        getEm(true).detach(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public boolean contains(Object entity) {
        return getEm(true).contains(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public LockModeType getLockMode(Object entity) {
        return getEm(true).getLockMode(entity);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public void setProperty(String propertyName, Object value) {
        getEm(true).setProperty(propertyName, value);
    }
    
    /**
     * {@inheritDoc  
     */
    @Override
    public Map<String, Object> getProperties() {
        return getEm(true).getProperties();
    }

    @Override
    public Query createQuery(String qlString) {
        return getEm(false).createQuery(qlString);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return getEm(false).createQuery(criteriaQuery);
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        return getEm(false).createQuery(updateQuery);
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        return getEm(false).createQuery(deleteQuery);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return getEm(false).createQuery(qlString, resultClass);
    }

    @Override
    public Query createNamedQuery(String name) {
        return getEm(false).createNamedQuery(name);
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return getEm(false).createNamedQuery(name, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        return getEm(false).createNativeQuery(sqlString);
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        return getEm(false).createNamedQuery(sqlString, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return getEm(false).createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return getEm(false).createNamedStoredProcedureQuery(name);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return getEm(false).createStoredProcedureQuery(procedureName);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        return getEm(false).createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return getEm(false).createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public void joinTransaction() {
        getEm(true).joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return getEm(true).isJoinedToTransaction();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return getEm(false).unwrap(cls);
    }

    @Override
    public Object getDelegate() {
        return getEm(false).getDelegate();
    }

    @Override
    public void close() {
        throw new IllegalStateException("Managed EntityManager cannot be closed");
    }

    @Override
    public boolean isOpen() {
        return getEm(true).isOpen(); //@TODO always return true?
    }

    @Override
    public EntityTransaction getTransaction() {
        return getEm(true).getTransaction();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return getEm(false).getEntityManagerFactory();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return getEm(false).getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return getEm(false).getMetamodel();
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        return getEm(false).createEntityGraph(rootType);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        return getEm(false).createEntityGraph(graphName);
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        return getEm(false).getEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return getEm(false).getEntityGraphs(entityClass);
    }

    @Override
    public String toString() {
        return getEm(false).toString() + "[Managed-JPAService]";
    }    
    
    private final ServiceReference<EntityManagerFactory> emfRef;
    private final Map<String, Object> properties;
    private final ManagedPersistenceContextManager contextManager;
    
    private volatile EntityManager nonJTAEm;
        
    private static final Logger LOG = LoggerFactory.getLogger(ManagedEntityManager.class);
}
