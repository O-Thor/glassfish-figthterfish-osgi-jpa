/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.fighterfish.jpaservice;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.concurrent.TimeoutException;
import javax.persistence.spi.PersistenceProvider;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceUnitXml;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class JPABundleManager extends BundleTracker<Future<JPAServiceBundle>> implements ServiceTrackerCustomizer<PersistenceProvider, PersistenceProvider>{

    public JPABundleManager(BundleContext context) {
        super(context, Bundle.ACTIVE | Bundle.STARTING, null);
    }

    @Override
    public Future<JPAServiceBundle> addingBundle(Bundle bundle, BundleEvent event) {
        if (bundle.getState() == Bundle.STOPPING) {
            LOG.info("Bundle {} is already stopping: Not processing", bundle);
            return null;
        }                    
        /**
         * Track only bundles that contain the 'Meta-Persistence' header,
         * spec#127.4.2
         */
        if (Collections.list(bundle.getHeaders().keys()).contains(JPAConstants.HEADER_META_PERSISTENCE)) {
            LOG.debug("adding bundle {}", bundle);
            try {
                return createTask(bundle);
            } catch (Exception e) {
                LOG.error("Failed constructing OSGiJPABundle {}", bundle, e);
            }
        }
        return null;
    }

    @Override
    public PersistenceProvider addingService(ServiceReference<PersistenceProvider> reference) {
        synchronized (this.mgrLock) {
            LOG.trace("Adding persistence provider {}", reference.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME));
            this.providers.add(reference);
            
            Map<JPAServiceBundle, ServiceReference<PersistenceProvider>> list = new HashMap<>();
            if(!this.jpaServiceUnprovidedConsumers.isEmpty()) {
                Iterator<JPAServiceBundle> it = this.jpaServiceUnprovidedConsumers.iterator();
                while(it.hasNext()) {
                    JPAServiceBundle jpaServiceConsumer = it.next();     
                    ServiceReference<PersistenceProvider> provider = this.findPersistenceProvider(jpaServiceConsumer);
                    if(provider != null) {
                        list.put(jpaServiceConsumer, provider);
                        it.remove();
                    }
                }
            }
            for(Entry<JPAServiceBundle, ServiceReference<PersistenceProvider>> e : list.entrySet()) {
                JPAServiceBundle bundle = e.getKey();
                ServiceReference<PersistenceProvider> provider = e.getValue();
                try {                    
                    ServiceReference<PersistenceProvider> oldProvider = bundle.getPersistenceProvider();
                    bundle.updateProvider(provider);                    
                    this.jpaServiceConsumers.put(bundle, provider);
                    if(oldProvider != null) {
                        LOG.info("JPA Service bundle {} was updated to use the Persistence Provider {}", provider);
                    } else {
                        LOG.info("JPA Service bundle {} was initialized to use the Persistence provider {}", provider);
                    }                    
                } catch(Exception ex) {
                    LOG.warn("Failed updating provider {} for JPA Service bundle {} ", provider, bundle, ex);
                    this.jpaServiceUnprovidedConsumers.add(bundle);                                        
                }                
            }
            LOG.debug("Added persistence provider {}", reference.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME));
            return context.getService(reference);
        }
    }

    @Override
    public void modifiedService(ServiceReference<PersistenceProvider> reference, PersistenceProvider service) {
        LOG.warn("JPA Service reference of EntityManagerFactory {} was modified. Changes do not cause reinitialization of JPA services", reference);
    }

    @Override
    public void removedService(ServiceReference<PersistenceProvider> reference, PersistenceProvider service) {
        synchronized (this.mgrLock) {
            if(!this.providers.remove(reference)) {
                LOG.warn("Persistence Provider reference {} was not removed from provider list", reference);
            }
            for (Iterator<Entry<JPAServiceBundle, ServiceReference<PersistenceProvider>>> it = this.jpaServiceConsumers.entrySet().iterator(); it.hasNext();) {
                Entry<JPAServiceBundle, ServiceReference<PersistenceProvider>> e = it.next();
                JPAServiceBundle jpaServiceBundle = e.getKey();
                if(     jpaServiceBundle.getPersistenceProvider() != null && 
                        jpaServiceBundle.getPersistenceProvider().equals(reference)) {
                    jpaServiceBundle.uninit();
                    trackingBundles.remove(jpaServiceBundle.getBundle());
                    it.remove();
                    this.jpaServiceUnprovidedConsumers.add(jpaServiceBundle);
                }
            }
            LOG.debug("Removed PersistenceProvider {}", reference.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME));                        
        }
    }    

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Future<JPAServiceBundle> object) {
        if(event != null && event.getType() == BundleEvent.UPDATED) {
            LOG.debug("Modifying JPA Service bundle {}", bundle);
            try {
                this.tryUninit(bundle, object);
            }catch(Exception e) {
                LOG.warn("JPA Service bundle {} uninitialization failed", bundle);
                return;
            }
            try {
                object.get().init();
            } catch (ExecutionException ex) {
                LOG.warn("JPA Service bundle {} initialization failed", bundle);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Future<JPAServiceBundle> object) {
        LOG.trace("Removing bundle {}", bundle);
        JPAServiceBundle jPAServiceBundle = null;
        boolean failedUninit = false, failedClean = false;
        try {
            this.tryUninit(bundle, object);            
        }catch(Exception e) {
            failedUninit = true;
            LOG.warn("JPA Service Bundle {} failed unitializing", bundle, e);
        }   
        try {
            jPAServiceBundle = object.get();
            if(jPAServiceBundle != null) {
                synchronized (this.mgrLock) {                
                    jPAServiceBundle.setPersistenceProvider(null);                                
                    this.jpaServiceConsumers.remove(jPAServiceBundle);
                    this.jpaServiceUnprovidedConsumers.remove(jPAServiceBundle);
                }
            }
        }catch(Exception e) {
            if(!failedUninit) {
                LOG.error("JPA Service Bundle {} failed cleaning up", bundle, e);
            } else {
                LOG.debug("JPA Service bundle {} failed cleaning up", bundle, e);
            }
        }                
        LOG.info("Removed JPA Service bundle {}", bundle);
    }
    
    protected void tryUninit(Bundle bundle, Future<JPAServiceBundle> object) {
        if (!object.isDone()) {
            object.cancel(true);
        }
        try {
            object.get(250, MILLISECONDS).uninit();
        } catch (ExecutionException ex) {
            throw new RuntimeException("Failed unitializing JPAServiceBundle " + bundle.getSymbolicName() + "-" + bundle.getVersion(), ex.getCause());
        } catch(TimeoutException | InterruptedException ex) {
            throw new RuntimeException("Failed waiting on JPA Service bundle " + bundle.getSymbolicName() + "-" + bundle.getVersion() + " to complete initialization", ex);
        }
    }

    @Override
    public void open() {
        this.es = Executors.newCachedThreadPool();
        this.persistenceProviderTracker = new ServiceTracker<>(context, PersistenceProvider.class, this);
        this.persistenceProviderTracker.open();
        super.open();
    }

    @Override
    public void close() {
        this.es.shutdownNow();
        this.es = null;
        super.close();
        this.persistenceProviderTracker.close();
        this.persistenceProviderTracker = null;
    }
    
    protected Future<JPAServiceBundle> createTask(Bundle bundle) {
        return es.submit(new CreateJPAServiceBundleTask(bundle));
    }
            
    protected ServiceReference<PersistenceProvider> findPersistenceProvider(JPAServiceBundle jpaServiceBundle) {
        Bundle bundle = jpaServiceBundle.getBundle();
        PersistenceDescriptor descriptor = jpaServiceBundle.getDescriptor();
        try {
            Collection<ServiceReference<PersistenceProvider>> serviceReferences = this.context.getServiceReferences(PersistenceProvider.class, null);
                        
            //validate single provider for all units in the same descriptor. Only needs to be specified once
            List<PersistenceUnitXml> puList = descriptor.getPersistence().getPersistenceUnit();
            String providerName = null;
            for(PersistenceUnitXml puxml : puList) {
                String puXmlProvider = puxml.getProvider();
                if(providerName == null) {                    
                    if(puXmlProvider != null) {
                        providerName = puXmlProvider;
                    }
                } else {
                    if(!puXmlProvider.equals(providerName)) {
                        throw new JPAServiceSpecException(JPAServiceSpecException.Type.DESCRIPTOR_VALIDATION_FAILED_MULTIPLE_PROVIDERS, bundle, descriptor);
                    }
                }
            }
            
            Iterator<ServiceReference<PersistenceProvider>> it = serviceReferences.iterator();
            while(it.hasNext()) {                
                ServiceReference<PersistenceProvider> pp = it.next();
                if(providerName == null) {
                    return pp;
                } else if(providerName.equals(pp.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME))) {
                    return pp;
                }
            }
            return null;
        } catch (InvalidSyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
                
    private class CreateJPAServiceBundleTask implements Callable<JPAServiceBundle> {

        public CreateJPAServiceBundleTask(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public JPAServiceBundle call() throws Exception {
            try {
                JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(context, this.bundle);
                ServiceReference<PersistenceProvider> provider = findPersistenceProvider(jpaServiceBundle);
                if(provider != null) {                    
                    jpaServiceBundle.updateProvider(provider);
                    synchronized (mgrLock) {
                        jpaServiceConsumers.put(jpaServiceBundle, provider);
                    }
                    LOG.info("JPA Service bundle {} initialized to use the Persistence Provider {}", this.bundle, provider.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME));
                } else {
                    synchronized (mgrLock) {
                        jpaServiceUnprovidedConsumers.add(jpaServiceBundle);
                        LOG.info("Provider not available for JPA Service bundle {}", this.bundle);
                    }
                }
                synchronized (mgrLock) {
                    trackingBundles.put(bundle, jpaServiceBundle);
                }
                return jpaServiceBundle;
            } catch (Exception e) {
                LOG.error("Failed initializing JPA Service bundle {}", bundle, e);
                throw new RuntimeException(e);
            }
        }
        
        private Bundle bundle;
        
    }    
    
    private ExecutorService es;
    private ServiceTracker<PersistenceProvider, PersistenceProvider> persistenceProviderTracker;
    
    private final Object mgrLock = new Object();
    
    //Synchronize access on {@link #mgrLock} for the following maps and sets
    
    protected final Map<Bundle, JPAServiceBundle> trackingBundles = new HashMap<>();    
    protected final Map<JPAServiceBundle, ServiceReference<PersistenceProvider>> jpaServiceConsumers = new HashMap<>();
    protected final Set<JPAServiceBundle> jpaServiceUnprovidedConsumers = new HashSet<>();    
    protected final Set<ServiceReference<PersistenceProvider>> providers = new HashSet<>();                       
    

    private static final Logger LOG = LoggerFactory.getLogger(JPABundleManager.class);
}
