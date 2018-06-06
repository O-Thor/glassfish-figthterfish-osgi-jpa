/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.fighterfish.jpaservice.managed;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionSynchronizationRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class ManagedPersistenceContextManager extends ServiceTracker<TransactionSynchronizationRegistry, TransactionSynchronizationRegistry> {

    public ManagedPersistenceContextManager(BundleContext context) {
        super(context, TransactionSynchronizationRegistry.class, null);
    }        

    public EntityManager getCurrentPersistenceContext(ServiceReference<EntityManagerFactory> emfRef, Map<String, Object> properties) throws TransactionRequiredException {
        if(this.transactionRegistry == null) {
            throw new TransactionRequiredException("No " + TransactionSynchronizationRegistry.class.getName() + " service is available");
        } else if(this.transactionRegistry.getTransactionKey() == null) {
            throw new TransactionRequiredException("No transaction context active");
        }
        
        EntityManager em = null;        
                
        @SuppressWarnings("unchecked")
        Map<ServiceReference<EntityManagerFactory>, EntityManager> resourceMap = (Map<ServiceReference<EntityManagerFactory>, EntityManager>) this.transactionRegistry.getResource(this.TSRMapKey);
        
        if(resourceMap == null) {
            LOG.debug("Creating an new managed persistence context");
            resourceMap = new IdentityHashMap<>();
            try {
                this.transactionRegistry.putResource(this.TSRMapKey, resourceMap);
            }catch(IllegalStateException e) {
                LOG.debug("Unable to associate persistence context with transaction {}", this.transactionRegistry.getTransactionKey());
                throw new TransactionRequiredException("No transaction context active");
            }            
        } else {
            em = resourceMap.get(emfRef);
        }
        
        if(em == null) {            
            BundleContext ctx = emfRef.getBundle().getBundleContext();
            EntityManagerFactory emf = ctx.getService(emfRef);
            em = (properties == null) ? emf.createEntityManager() : emf.createEntityManager(properties);            
            try {                
                this.transactionRegistry.registerInterposedSynchronization(new PersistenceSynchronization(emfRef, em));                
                LOG.debug("Created a new persistence context {} for transaction {}", em, this.transactionRegistry.getTransactionKey());
            }catch(IllegalStateException e) {                
                LOG.debug("Unable to register synchronization with context", e);                
                em.close();
                throw new TransactionRequiredException("Unable to register synchroniztaion with context");
            }
            resourceMap.put(emfRef, em);
        }        
        return em;
    }

    public boolean isJtaAvailable() {
        return this.transactionRegistry != null;
    }

    public boolean isActiveTransaction() {
        return this.transactionRegistry != null && this.transactionRegistry.getTransactionKey() != null;
    }

    @Override
    public TransactionSynchronizationRegistry addingService(ServiceReference<TransactionSynchronizationRegistry> reference) {
        synchronized (lock) {            
            if(this.transactionRegistry == null) {
                TransactionSynchronizationRegistry service = super.addingService(reference);
                this.transactionRegistry = service;
                return service;
            } else {
                LOG.warn("{} service is already register. Ignoring.", reference);
                return null;
            }
        }
    }

    @Override
    public void modifiedService(ServiceReference<TransactionSynchronizationRegistry> reference, TransactionSynchronizationRegistry service) {
        LOG.info("Modified TransactionSynchronizationRegistry. No changes made to context");
    }

    @Override
    public void removedService(ServiceReference<TransactionSynchronizationRegistry> reference, TransactionSynchronizationRegistry service) {
        this.transactionRegistry = null;
        super.removedService(reference, service);
    }       
    
    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return this.transactionRegistry;
    }
     
    
    protected ExecutorService es = Executors.newCachedThreadPool();
    
    protected volatile TransactionSynchronizationRegistry transactionRegistry;
        
    protected final Object TSRMapKey = new Object() {
        @Override public int hashCode() {
            return 7777777;
        }
        @Override public boolean equals(Object obj) {
            return this == obj;
        }
    };    
    
    private final Object lock = new Object();
  
    private static final Logger LOG = LoggerFactory.getLogger(ManagedPersistenceContextManager.class);
    
}
