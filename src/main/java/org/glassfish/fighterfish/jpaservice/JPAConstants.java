package org.glassfish.fighterfish.jpaservice;

/**
 *
 * @author thor
 */
public final class JPAConstants {

    public static final String HEADER_META_PERSISTENCE = "Meta-Persistence";

    public static final String DEFAULT_META_PERSITENCE = "META-INF/persistence.xml";

    public static final String JAVAX_PERSISTENCE_PROVIDER = "javax.persistence.provider";
    
    /**
     * The service property key mapped to the persistence provider name
     * @TODO use a more correct value?!
     */
    public static final String JAVAX_PERSISTENCE_PROVIDER_NAME = "javax.persistence.provider.name";

    /**
     * The service property key mapped to the persistence unit name
     */
    public static final String OSGI_UNIT_NAME = "osgi.unit.name";
    
    /**
     * The version of the persistence bundle as a {@link Version} object
     */
    public static final String OSGI_UNIT_VERSION = "osgi.unit.version";
    
    /**
     * The service property key mapped to the {@link PersistenceProvider} implementation class name
     */
    public static final String OSGI_UNIT_PROVIDER = "osgi.unit.provider";

    /**
     * The service property key for managed persistence contexts
     */
    public static final String MANAGED_PERSISTENCE_CONTEXT = "org.glassfish.fighterfish.jpaservice.managed.active";
    
    /**
     * The service property key to mark persistence contexts as managed
     */
    public static final String MANAGED_DELEGATE_EMF = "org.glassfish.fighterfish.jpaservice.managed.delegate.emf";
    
    public static final String MANAGED_PROVIDER = "org.glassfish.fighterfish.jpaservice.managed.provider";
}
