
package org.glassfish.fighterfish.jpaservice;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceUnitXml;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceXml;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


/**
 * Translates JAXB XML Schema to {@link PersistenceUnitInfo}
 * 
 * @author thor
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    public PersistenceUnitInfoImpl(Bundle bundle, PersistenceXml persistenceXml, PersistenceUnitXml puXml) {
        this.bundle = bundle;
        this.persistenceXml = persistenceXml;
        this.unit = puXml;
        this.classLoader = new DelegatingBundleClassLoader(this.bundle);
        this.propertis =  puXml.getProperties();
    }
    
    @Override
    public String getPersistenceUnitName() {
        return this.unit.getName();
    }

    @Override
    public String getPersistenceProviderClassName() {
        return this.unit.getProvider();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return def(this.unit.getTransactionType(), PersistenceUnitTransactionType.JTA);
    }

    @Override
    public DataSource getJtaDataSource() {
        return this.lookupOrNull(this.unit.getJtaDataSource());
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return this.lookupOrNull(this.unit.getNonJtaDataSource());
    }

    @Override
    public List<String> getMappingFileNames() {
        return this.unit.getMappingFile();
    }

    @Override
    public List<URL> getJarFileUrls() {
        List<URL> jarFiles = new ArrayList<>(this.unit.getJarFile().size());
        for(String jf : this.unit.getJarFile()) {
            URL url = this.bundle.getResource(jf);
            if(url == null) {
                throw new RuntimeException("The persistence unit " + this.getPersistenceUnitName() + " listed a jar file but it could not be found in bundle " + this.bundle);
            }
            jarFiles.add(url);
        }
        return jarFiles;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return this.bundle.getResource("/");
    }

    @Override
    public List<String> getManagedClassNames() {
        return this.unit.getClazz();
    }

    @Override
    public boolean excludeUnlistedClasses() {
        Boolean excludeUnlistedClasses = this.unit.isExcludeUnlistedClasses();
        return excludeUnlistedClasses != null ? excludeUnlistedClasses : false;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return def(this.unit.getSharedCacheMode(), SharedCacheMode.UNSPECIFIED);
    }

    @Override
    public ValidationMode getValidationMode() {
        return def(this.unit.getValidationMode(), ValidationMode.AUTO);
    }

    @Override
    public Properties getProperties() {
        return this.propertis;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return this.persistenceXml.getVersion();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return this.classLoader;
    }
    
    protected DataSource lookupOrNull(String jndi) {
        try {
            if(jndi == null || jndi.isEmpty()) {
                return null;
            }
            //workaround for non-osgi jndi support (GF)
            BundleContext bundleContext = FrameworkUtil.getBundle(PersistenceUnitInfoImpl.class).getBundleContext();                    
            Collection<ServiceReference<DataSource>> serviceReferences = 
                        bundleContext.getServiceReferences(DataSource.class, 
                    "(&" + 
                        "(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")" + 
                        "(jndi-name=" + jndi + ")" + 
                    ")");
            if(!serviceReferences.isEmpty()) {
                for(ServiceReference<DataSource> ds : serviceReferences) {
                    return bundleContext.getService(ds);
                }
            }
            return null;
        } catch (InvalidSyntaxException ex) {
            throw new RuntimeException(MessageFormat.format("Invalid DataSource {0}", jndi), ex);
        }
    }    
    
    protected <T> T def(T value, T def) {
        return value != null ? value : def;
    }
    
    private final Bundle bundle;
    private final PersistenceXml persistenceXml;
    private final PersistenceUnitXml unit;
    private final DelegatingBundleClassLoader classLoader;    
    private final Properties propertis;

}
