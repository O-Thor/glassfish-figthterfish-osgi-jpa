
package org.glassfish.fighterfish.jpaservice.managed;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManagerFactory;
import org.glassfish.fighterfish.jpaservice.JPAConstants;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.MANAGED_PERSISTENCE_CONTEXT_ALREADY_REGISTERED;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.MANAGED_PERSISTENCE_CONTEXT_DOES_NOT_EXIST;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for EMFs service registrations that can be managed and keeps track of managed EMFs for them.
 * 
 * @author thor
 */
public class ManagedPersistenceManager extends ServiceTracker<EntityManagerFactory, EntityManagerFactory> {

    public ManagedPersistenceManager(BundleContext context, ManagedPersistenceContextManager contextManager) {
        super(context, filter, null);        
        this.contextManager = Objects.requireNonNull(contextManager, "JtaPersistenceContextManager is required");
    }

    @Override
    public EntityManagerFactory addingService(ServiceReference<EntityManagerFactory> reference) {        
        String puName = (String) reference.getProperty(JPAConstants.OSGI_UNIT_NAME);
        if(puName == null) {
            puName = "";
        }        
        long    minWaitTimeNS = TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS),
                waitIntervalCheckNS = TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS),
                startTimeNS = System.nanoTime();
        
        synchronized (this.lock) {
            while(this.registrations.containsKey(puName)) {
                try {
                    LOG.info("Persistence context {} already registered, waiting for it to unregister before continuing");
                    TimeUnit.NANOSECONDS.timedWait(this.lock, minWaitTimeNS/100);
                    if((startTimeNS + minWaitTimeNS) >= System.nanoTime()) {
                        throw new JPAServiceSpecException(MANAGED_PERSISTENCE_CONTEXT_ALREADY_REGISTERED, reference.getBundle(), puName);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }            
        }
        synchronized (this.lock) {                           
            Hashtable<String, Object> props = new Hashtable<>();                            
            props.put(JPAConstants.OSGI_UNIT_NAME, puName);
            props.put(JPAConstants.OSGI_UNIT_VERSION, reference.getProperty(JPAConstants.OSGI_UNIT_VERSION));
            props.put(JPAConstants.OSGI_UNIT_PROVIDER, JPAConstants.MANAGED_PROVIDER);
            props.put(JPAConstants.MANAGED_PERSISTENCE_CONTEXT, true);                    
            props.put(JPAConstants.MANAGED_DELEGATE_EMF, true);              
            
            ServiceRegistration<EntityManagerFactory> r = this.context.registerService(EntityManagerFactory.class, new ManagedEntityManagerFactory(reference, props, contextManager), props);
            this.registrations.put(puName, r);     
            LOG.info("Registered managed EntityManagerFactory service with the configuration: {}", props);
        }        
        return super.addingService(reference);
    }

    @Override
    public void modifiedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {
        LOG.warn("Modification of Entity Manager Factory service {} has no effect on running services");
    }   
    
    @Override
    public void removedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {        
        synchronized (this.lock) {
            String unitName = (String) reference.getProperty(JPAConstants.OSGI_UNIT_NAME);
            if(unitName == null) {
                unitName = "";
            }            
            ServiceRegistration<EntityManagerFactory> registration = this.registrations.remove(unitName);
            if(registration == null) {
                throw new JPAServiceSpecException(MANAGED_PERSISTENCE_CONTEXT_DOES_NOT_EXIST, reference.getBundle(), reference);
            }            
            registration.unregister();
            this.lock.notifyAll();
        }          
        super.removedService(reference, service);
        LOG.info("Removed managed persistence context: {}", Objects.toString(reference.getProperty(JPAConstants.OSGI_UNIT_NAME), "<null>"));
    }

    @Override
    public void open() {
        super.open();
        this.registrations = new HashMap<>();
    }

    @Override
    public void close() {
        super.close();
        synchronized (this.lock) {
            for(ServiceRegistration<EntityManagerFactory> r : this.registrations.values()) {
                try {
                    r.unregister();
                }catch(Exception e) {
                    LOG.warn("Failed unregistering managed EntityManagerFactory service: {}", r);
                }
            }
            this.registrations = null;
        }
    }        
    
    private final ManagedPersistenceContextManager contextManager;    
    
    /**
     * access must be synchronized on {@link #lock} 
     **/
    protected Map<String, ServiceRegistration<EntityManagerFactory>> registrations;        
    private final Object lock = new Object();    
    
    private static Filter filter;    
    
    static {
        try {
            filter = FrameworkUtil.createFilter(
                    "(&" + 
                        "(" + Constants.OBJECTCLASS + "=" + EntityManagerFactory.class.getName() + ")" + 
                        "(" + JPAConstants.MANAGED_PERSISTENCE_CONTEXT + "=TRUE)" + 
                        "(!(" + JPAConstants.MANAGED_DELEGATE_EMF + "=*))" + 
                    ")");
        } catch (InvalidSyntaxException ex) {
            throw new Error("Failed creating filter");
        }
    }
        
    private static final Logger LOG = LoggerFactory.getLogger(ManagedPersistenceManager.class);        

}
