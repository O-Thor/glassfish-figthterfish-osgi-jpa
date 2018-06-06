package org.glassfish.fighterfish.jpaservice.exception;

import java.text.MessageFormat;
import org.osgi.framework.Bundle;

/**
 * Exception explicitly defined by the OSGi JPA Service Specification
 *
 * @author thor
 */
public class JPAServiceSpecException extends JPAServiceException {

    public enum Type {
                
        FAILED_READING_DESCRIPTOR("127.4.2", "Failed loading persistence xml data"),
        
        /**
         * JPA bundle must define at least one Persistence Unit. Spec#127.4.3
         */
        NO_PERSISTENCE_UNITS_DEFINED("127.4.3", "No persistence units defined"),
        
        DESCRIPTOR_VERSION_INVALID("", "Persistence xml descriptor contains invalid version"),
        
        /**
         * 
         * @param URL to descriptor
         */
        DESCRIPTOR_VALIDATION_FAILED("", "Persistence xml descriptor is invalid {1}: {2}"),

        DECLARED_DESCRIPTOR_NOT_FOUND("", "Declared persistence xml descriptor {1} was not found"),
        
        /**
         * JPA bundle persistence.xml file cannot declare a dependency on more than one persistence provider at a time.
         */
        DESCRIPTOR_VALIDATION_FAILED_MULTIPLE_PROVIDERS("", "Persistence xml descriptor is invalid {1}: Has multiple persistence providers declared"),
        
        NO_PERSISTENCE_PROVIDER_AVAILABLE(""),
        
        FAILED_PROCESSING_DESCRIPTOR("", "Failed processing descriptor {1}"),
        
        MANAGED_PERSISTENCE_CONTEXT_DOES_NOT_EXIST("", "Managed context {1} does not exist"),
        
        MANAGED_PERSISTENCE_CONTEXT_ALREADY_REGISTERED("", "Managed context already has registered persistence context with the name {1}");
        
        private Type(String spec, String msg) {
            this.spec = spec;
            this.msg = msg;
        }
        
        private Type(String spec) {
            this.spec = spec;
            this.msg = name().replace("_", " ");
            this.msg = this.msg.substring(0, 1).toUpperCase() + this.msg.substring(1, this.msg.length());
        }
        
        private String spec;
        private String msg;
    }

    public JPAServiceSpecException(Type type, Bundle bundle, Object ... typeParams) {
        super(bundle, format(type, bundle, typeParams));
        this.type = type;
    }

    public JPAServiceSpecException(Type type, Bundle bundle, Throwable cause, Object ... typeParams) {
        super(bundle, format(type, bundle, typeParams), cause);
        this.type = type;
    }
    
    private static String format(Type type, Bundle bundle, Object ... typeParams) {
        Object[] args;
        if(typeParams != null && typeParams.length > 0) {
            Object[] tmpArray = new Object[typeParams.length +1]; tmpArray[0] = bundle;
            System.arraycopy(typeParams, 0, tmpArray, 1, typeParams.length);
            args = tmpArray;
        } else {
            args = new Object[1]; args[0] = bundle;
        }
        return MessageFormat.format("Spec#" + type.spec + " violation in {0}: " + type.msg, args);
    }

    public Type getType() {
        return type;
    }

    private final Type type;

}
