
package org.glassfish.fighterfish.jpaservice.exception;

import java.text.MessageFormat;
import org.osgi.framework.Bundle;

/**
 *
 * @author thor
 */
@Deprecated
public class JPAServiceException extends RuntimeException {

    public JPAServiceException(Bundle bundle) {
        this.bundle = bundle;
    }

    public JPAServiceException(Bundle bundle, String message) {
        super(message);
        this.bundle = bundle;
    }

    public JPAServiceException(Bundle bundle, String message, Throwable cause) {
        super(message, cause);
        this.bundle = bundle;
    }

    public JPAServiceException(Bundle bundle, Throwable cause) {
        super(cause);
        this.bundle = bundle;
    }   
    
    private static String formatMessage(String msg, Bundle bundle) {
        return MessageFormat.format("Bundle: " + bundle + ": " + msg, bundle);
    }

    public Bundle getBundle() {
        return bundle;
    }        
    
    private final Bundle bundle;
}
