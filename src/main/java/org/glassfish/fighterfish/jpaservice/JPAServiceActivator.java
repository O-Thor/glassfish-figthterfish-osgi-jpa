
package org.glassfish.fighterfish.jpaservice;

import java.util.*;
import javax.persistence.spi.PersistenceProvider;
import org.glassfish.fighterfish.jpaservice.managed.ManagedPersistenceContextManager;
import org.glassfish.fighterfish.jpaservice.managed.ManagedPersistenceManager;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thor
 */
public class JPAServiceActivator implements BundleActivator {

    @Override
    public void start(BundleContext ctx) throws Exception {
        LOG.info("Starting JPA Service Spec");
        this.registerCommonProviders(ctx);
        this.ctxManager = new ManagedPersistenceContextManager(ctx);
        this.jpaBundleManager = new JPABundleManager(ctx);
        this.mgdPersistenceCtxMgr = new ManagedPersistenceContextManager(ctx);
        this.mgdPersistenceMgr = new ManagedPersistenceManager(ctx, this.mgdPersistenceCtxMgr);
        this.ctxManager.open();
        this.jpaBundleManager.open();
        this.mgdPersistenceCtxMgr.open();
        this.mgdPersistenceMgr.open();
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        LOG.info("Stopping JPA Service Spec");
        this.mgdPersistenceMgr.close();
        this.mgdPersistenceCtxMgr.close();
        this.jpaBundleManager.close();
        this.ctxManager.close();
        for(ServiceRegistration<PersistenceProvider> p : this.commonProviders) {
            p.unregister();
        }
    }

    /**
     * Register common known JPA implementations that are known not to register 
     * themselves as persistence providers
     * @param ctx 
     */
    @SuppressWarnings("unchecked")
    public void registerCommonProviders(BundleContext ctx) {
        
        Collection<String> providersMap =Arrays.asList(
                                            "org.eclipse.persistence.jpa.PersistenceProvider",
                                            "org.hibernate.jpa.HibernatePersistenceProvider"
                                        );
        
        this.commonProviders = new HashSet<>();
        try {
            Collection<ServiceReference<PersistenceProvider>> providers = ctx.getServiceReferences(PersistenceProvider.class, null);
            providerNames:
            for(String providerName : providersMap) {
                try {
                    for(ServiceReference<PersistenceProvider> provider : providers) {
                        Object osgiUnitProvider = provider.getProperty(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME);
                        if(osgiUnitProvider != null && osgiUnitProvider.equals(providerName)) {
                            LOG.info("Persistence Provider {} already found in OSGi context. Not registering", providerName);
                            continue providerNames;
                        }
                    }
                    @SuppressWarnings("unchecked")
                    Class<? extends PersistenceProvider> forName = (Class<? extends PersistenceProvider>) Class.forName(providerName);
                    Hashtable<String, Object> props = new Hashtable<>();
                    props.put(JPAConstants.JAVAX_PERSISTENCE_PROVIDER_NAME, providerName);
                    LOG.info("Persistence Provider '{}' registered as an OSGI service", providerName);
                    this.commonProviders.add(ctx.registerService(PersistenceProvider.class, forName.newInstance(), props));
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    LOG.error("Failed registering provider {}", providerName, ex);
                }
            }
        }catch(InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private ManagedPersistenceContextManager ctxManager;
    private JPABundleManager jpaBundleManager;
    private ManagedPersistenceContextManager mgdPersistenceCtxMgr;
    private ManagedPersistenceManager mgdPersistenceMgr;
    private Set<ServiceRegistration<PersistenceProvider>> commonProviders;

    private static final Logger LOG = LoggerFactory.getLogger(JPAServiceActivator.class);
}
