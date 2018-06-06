package org.glassfish.fighterfish.jpaservice.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import org.glassfish.fighterfish.jpaservice.JPAConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables use of injection for EntityManager and EntityManagerFactory using CDI
 *
 * @author thor
 */
public class JpaServiceExtension implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery abd, BeanManager bm) {
        LOG.info("Starting JPA Service Extension for persistence context injection");
        this.injectionPoints = new ConcurrentHashMap<>();
        this.targetsOfInterest = new HashSet<>();
        this.targetsOfInterest.addAll(Arrays.asList(EntityManager.class, EntityManagerFactory.class));
    }

    /**
     * Handle Java EE components
     * @param pit
     */
    void afterProcessInjectionTarget(@Observes ProcessInjectionTarget<?> pit) {
        this.process(pit.getInjectionTarget().getInjectionPoints());
        if(pit.getAnnotatedType().equals(PersistenceContext.class)) {
            LOG.error("Point of inquiry: I thought of overwriting PersistenceContext but not sure if CDI can.. Well I guess I should, because it is processes by CDI. Found: {}", pit);
        }
        for(InjectionPoint ip : pit.getInjectionTarget().getInjectionPoints()) {
            if(ip.getAnnotated() instanceof AnnotatedField && ((AnnotatedField)ip.getAnnotated()).getAnnotation(PersistenceContext.class) != null) {
                LOG.error("Point of inquiry: I thought of overwriting PersistenceContext but not sure if CDI can.. Well I guess I should, because it is processes by CDI. Found: {}", ip);
            }
        }
    }

    /**
     * Handle CDI components
     * @param pb
     */
    void afterProcessBean(@Observes ProcessBean<?> pb) {
        this.process(pb.getBean().getInjectionPoints());
    }

    protected void process(Set<InjectionPoint> ips) {
        for(InjectionPoint ip : ips) {
            Set<InjectionPoint> set = this.injectionPoints.get(ip.getType());
            if(set == null) {
                Set<InjectionPoint> newSet = Collections.newSetFromMap(new ConcurrentHashMap<InjectionPoint, Boolean>());
                set = this.injectionPoints.putIfAbsent(ip.getType(), newSet);
                if(set == null) {
                    set = newSet;
                }
            }
            set.add(ip);
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        LOG.debug("Registering injection points for JPA Services");
        Set<String> registeredEm = new HashSet<>();
        Set<String> registeredEmf = new HashSet<>();
        for(Entry<Type, Set<InjectionPoint>> e : this.injectionPoints.entrySet()) {
            Type type = e.getKey();
            for(InjectionPoint ip : e.getValue()) {
                if(ip.getType().equals(EntityManager.class)) {
                    LOG.trace("JPA Service injection type found {}", EntityManager.class);
                    String name = findPuName(ip);
                    if(!registeredEm.contains(name)) {
                        LOG.debug("Registered PersistenceContextBean for persistence context \"{}\"", Objects.toString(name, "<null>"));
                        abd.addBean(new PersistenceContextBean(name, ip));
                        registeredEm.add(name);
                    } else {
                        LOG.debug("PersistenceContextBean already registered for persistence context \"{}\"", Objects.toString(name, "<null>"));
                    }
                } else if(ip.getType().equals(EntityManagerFactory.class)) {
                    LOG.trace("JPA Service injection type found {}", EntityManagerFactory.class);
                    String name = findPuName(ip);
                    if(!registeredEmf.contains(name)) {
                        LOG.debug("Registered PersistenceUnitBean for persistence context \"{}\"", Objects.toString(name, "<null>"));
                        abd.addBean(new PersistenceUnitBean(name, ip));
                        registeredEmf.add(name);
                    } else {
                        LOG.debug("PersistenceUnitBean already registered for persistence context \"{}\"", Objects.toString(name, "<null>"));
                    }
                }
            }
        }
        LOG.trace("Registered {} persistence beans", registeredEm.size() + registeredEmf.size());
    }

    protected String findPuName(InjectionPoint ip) {
        String name = null;
        for(Annotation a : ip.getQualifiers()) {
            if(a.annotationType().equals(Named.class)) {
                Named named = (Named)a;
                name = named.value();
                LOG.trace("Injection point has a @Named qualifier with the value '{}'. Will be used to resolve Dependency to persistence context.");
                break;
            }
        }
        return name;
    }

    ConcurrentMap<Type, Set<InjectionPoint>> injectionPoints;
    Set<Class<?>> targetsOfInterest;

    protected class PersistenceUnitBean extends AbstractBean<EntityManagerFactory> {

        public PersistenceUnitBean(String puName, InjectionPoint injectionPoint) {
            super(puName, EntityManagerFactory.class, injectionPoint);
        }

        @Override
        public EntityManagerFactory create(CreationalContext<EntityManagerFactory> creationalContext) {
            BundleContext ctx = FrameworkUtil.getBundle(JpaServiceExtension.class).getBundleContext();
            Collection<ServiceReference<EntityManagerFactory>> serviceRefs;
            try {
                serviceRefs = ctx.getServiceReferences(EntityManagerFactory.class, this.serviceFilter);
            } catch (InvalidSyntaxException ex) {
                throw new RuntimeException("EntityManagerFactory service filter is invalid. Check Persistence context name \"" + this.puName + "\"", ex);
            }
            if(serviceRefs != null && !serviceRefs.isEmpty()) {
                logMultipleManagedServices(serviceRefs);
                ServiceReference<EntityManagerFactory> firstRef = serviceRefs.iterator().next();
                EntityManagerFactory service = ctx.getService(firstRef);
                LOG.debug("Injecting EntityManager from service {} using {}", EntityManagerFactory.class, service);
                this.creationCtxServiceRefMap.put(creationalContext, firstRef);
                return service;
            } else {
                LOG.debug("No managed {} service available with the name \"{}\"", EntityManagerFactory.class.getName(), Objects.toString(this.puName, "<null>"));
                return super.create(creationalContext);
            }
        }

    }

    protected class PersistenceContextBean extends AbstractBean<EntityManager> {

        public PersistenceContextBean(String puName, InjectionPoint injectionPoint) {
            super(puName, EntityManager.class, injectionPoint);
        }

        @Override
        public EntityManager create(CreationalContext<EntityManager> creationalContext) {
            BundleContext ctx = FrameworkUtil.getBundle(JpaServiceExtension.class).getBundleContext();
            Collection<ServiceReference<EntityManagerFactory>> serviceRefs;
            try {
                serviceRefs = ctx.getServiceReferences(EntityManagerFactory.class, this.serviceFilter);
            } catch (InvalidSyntaxException ex) {
                throw new RuntimeException("EntityManagerFactory service filter is invalid. Check Persistence context name \"" + this.puName + "\"", ex);
            }
            if(serviceRefs != null && !serviceRefs.isEmpty()) {
                logMultipleManagedServices(serviceRefs);
                ServiceReference<EntityManagerFactory> firstEMF = serviceRefs.iterator().next();
                EntityManagerFactory service = ctx.getService(firstEMF);
                EntityManager em = service.createEntityManager();
                this.creationCtxServiceRefMap.put(creationalContext, firstEMF);
                LOG.debug("Injecting EntityManager using {}", EntityManagerFactory.class, service);
                return em;
            } else {
                LOG.debug("No {} service available", EntityManagerFactory.class.getName());
                return super.create(creationalContext);
            }
        }

    }

    protected abstract class AbstractBean<T> implements Bean<T> {

        AbstractBean(String puName, Class<T> targetClass, InjectionPoint injectionPoint) {
            if(puName != null && puName.isEmpty()) {
                this.puName = puName;
            } else {
                this.puName = null;
            }
            this.targetClass = Objects.requireNonNull(targetClass);
            this.injectionPoint = Objects.requireNonNull(injectionPoint, "Injection point required");
            this.serviceFilter =
                            "(&" +
                                "(" + JPAConstants.MANAGED_PERSISTENCE_CONTEXT + "=TRUE)" +
                                "(" + JPAConstants.MANAGED_DELEGATE_EMF + "=TRUE)" +
                                "(" + JPAConstants.OSGI_UNIT_NAME + "=" + (this.puName == null ? "*" : this.puName) + ")" +
                            ")";
        }

        @Override
        public Class<?> getBeanClass() {
            return this.targetClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.EMPTY_SET;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> types = new HashSet<Type>();
            types.add(this.targetClass);
            types.add(Object.class);
            return types;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            qualifiers.add(new AnnotationLiteral<Default>() {});
            qualifiers.add(new AnnotationLiteral<Any>() {});
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return RequestScoped.class;
        }

        @Override
        public String getName() {
            return this.targetClass.getCanonicalName() + "Bean";
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.EMPTY_SET;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public T create(CreationalContext<T> creationalContext) {
            throw new ServiceException(MessageFormat.format("EntityManagerFactory service \"{0}\" is unavailable", Objects.toString(this.puName, "<default>")));
        }                

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext) {
            BundleContext ctx = FrameworkUtil.getBundle(JpaServiceExtension.class).getBundleContext();
            ServiceReference<?> ref = this.creationCtxServiceRefMap.get(creationalContext);
            if(ref == null) {
                LOG.error("{} could not be found for creationalContextinstance \"{}\"", creationalContext, instance);
            } else  {
                try {
                    ctx.ungetService(ref);
                } catch(IllegalStateException ex) {
                    LOG.warn("The {} was no longer valid when cleaning up CDI instance \"{}\"", ServiceReference.class.getName(), ref);
                }
            }
        }

        protected void logMultipleManagedServices(Collection<ServiceReference<EntityManagerFactory>> serviceRefs) {
            if(LOG.isDebugEnabled() && serviceRefs.size() > 1) {
                StringBuilder sb = new StringBuilder(256);
                for (Iterator<ServiceReference<EntityManagerFactory>> it = serviceRefs.iterator(); it.hasNext();) {
                    ServiceReference<EntityManagerFactory> sr = it.next();
                    sb.append("ServiceRef: ").append(sr).append('[');
                    for (Iterator<String> kIt = Arrays.asList(sr.getPropertyKeys()).iterator(); kIt.hasNext();) {
                        String k = kIt.next();
                        sb.append(k).append("=>").append(sr.getProperty(k));
                        if(kIt.hasNext()) { sb.append(","); }
                    }
                    sb.append(']');
                    if(it.hasNext()) {
                        sb.append(", ");
                    }
                }
                LOG.debug("EntityManagerFactory service filter found more than one possible services. This is most likely an error. Picking first one. Found: {}", sb);
            }
        }

        protected final String puName;
        protected final Class<T> targetClass;
        protected final InjectionPoint injectionPoint;
        protected final String serviceFilter;
        /**
         * All created instance and their corresponding {@link ServiceReference} must be properly mapped for bean destruction to cleanup properly
         */
        protected final ConcurrentMap<CreationalContext<T>, ServiceReference<?>> creationCtxServiceRefMap = new ConcurrentHashMap<>();
    }

    private static Logger LOG = LoggerFactory.getLogger(JpaServiceExtension.class);
}
