
package org.glassfish.fighterfish.jpaservice.managed;

import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class PersistenceSynchronization implements Synchronization {

    public PersistenceSynchronization(ServiceReference<EntityManagerFactory> emfRef, EntityManager em) {
        this.emfRef = Objects.requireNonNull(emfRef);
        this.em = Objects.requireNonNull(em);
    }

    @Override
    public void beforeCompletion() {
        LOG.debug("PersistenceSynchronization::beforeCompletion() in thread '{}'", Thread.currentThread());
    }

    /**
     * JTA handles clean up
     * 
     * @param status 
     */
    @Override
    public void afterCompletion(int status) {
        LOG.debug("PersistenceSynchronization::afterCompletion(Status={})", status);
        if (this.em.isOpen()) {
            LOG.debug("Closing EntityManager {}", em);
            this.em.close();            
        }
        this.emfRef.getBundle().getBundleContext().ungetService(emfRef);
    }
        
    private EntityManager em;
    private ServiceReference<EntityManagerFactory> emfRef;
                
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceSynchronization.class);            
}
