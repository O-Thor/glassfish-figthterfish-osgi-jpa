
package org.glassfish.fighterfish.jpaservice;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 *
 * @author thor
 */
public class DelegatingBundleClassLoader extends ClassLoader implements BundleReference {

    public DelegatingBundleClassLoader(Bundle bundle) {
        this.bundle = Objects.requireNonNull(bundle);
    }

    @Override
    public URL getResource(String name) {
        return this.bundle.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = this.bundle.getResources(name);
        return resources != null ? resources : Collections.<URL>emptyEnumeration();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.bundle.loadClass(name);
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }        
        
    private final Bundle bundle;
}
