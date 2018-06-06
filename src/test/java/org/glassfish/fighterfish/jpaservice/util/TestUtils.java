
package org.glassfish.fighterfish.jpaservice.util;


import java.text.MessageFormat;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author thor
 */
public class TestUtils {
            
    public enum CheckMessage {
        CHECK_MESSAGE;
    }
    
    
    public static void expect(Exception expected, TestRunnable run) {
        expect(expected, run, null);
    }
    
    /**
     * 
     * @param expected
     * @param run
     * @param checkMessage null if not wanted see {@link #expect(java.lang.Exception, org.glassfish.fighterfish.jpaservice.util.TestUtils.TestRunnable) }
     */
    public static void expect(Exception expected, TestRunnable run, CheckMessage checkMessage) {
        try {
            run.run();
            fail("Expected exception " + expected.getClass().getCanonicalName());
        }catch(Exception haveEx) {
            if(expected instanceof JPAServiceSpecException && haveEx instanceof JPAServiceSpecException) {
                JPAServiceSpecException expectedSe = (JPAServiceSpecException)expected,
                                        haveSe = (JPAServiceSpecException)haveEx;
                if(!Objects.equals(expectedSe.getType(), haveSe.getType())) {
                    failed(haveEx, "\t\t{0}\n\tof type\n\\t{1}\n\t\tbut got:\n\t\t{2}", JPAServiceSpecException.class.getSimpleName(), expectedSe.getType(), haveSe.getType());
                } else if(!Objects.equals(expectedSe.getBundle(), haveSe.getBundle())) {
                    failed(haveEx, "\texception from bundle:\n\t\t{0}\n\tbut was from:\n\t\t{1}", expectedSe.getBundle(), haveSe.getBundle());
                }
            } else if(Objects.equals(expected.getClass(), haveEx.getClass())) {
                 if(checkMessage != null && !Objects.equals(haveEx.getMessage(), expected.getMessage())) {
                    failed(haveEx, "\t\t{0}(\"{1}\")\n\tbut got:\n\t\t{2}(\"{3}\")", expected.getClass().getCanonicalName(), expected.getMessage(), haveEx.getClass().getCanonicalName(), haveEx.getMessage());
                 }
            } else {
                failed(haveEx, "\t\t{0}\n\tbut got:\n\t\t{2}", expected.getClass().getCanonicalName(), haveEx.getClass().getCanonicalName());
            }
        }
    }
    
    private static void failed(Exception ex, Object ... params) {
        failed(ex, "", params);
    }
    
    private static void failed(Exception ex, String msg, Object ... params) {
        throw new AssertionError(MessageFormat.format("Expected:\n" + msg + "\n\n", params), ex);
    }
        
    public interface TestRunnable {
        public void run() throws Exception;
    }
    
    private static final AtomicInteger counter = new AtomicInteger(10000);
    
    public static void mockDefaults(Bundle bundleMock) {
        mockDefaults(bundleMock, null);
    } 
    
    public static void mockDefaults(Bundle bundleMock, String name) {
        when(bundleMock.toString()).thenReturn((name != null ? name : "BundleMock-" + counter.incrementAndGet()) + ":1.0.0");
        when(bundleMock.getHeaders()).thenReturn(new Hashtable<String, String>()); 
    }
    
    /**
     * Creates a {@link ServiceReference} mock with default instance, bundle and context
     * 
     * @param <T>
     * @param clazz
     * @return 
     */
    public static <T> ServiceReference<T> mockServiceRef(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        ServiceReference<T> ref = mock(ServiceReference.class);
        T instance = mock(clazz);
        Bundle bundle = mock(Bundle.class);
        mockDefaults(bundle);
        BundleContext ctx = mock(BundleContext.class);
        when(ref.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(ctx);
        when(ctx.getService(ref)).thenReturn(instance);
        return ref;
    }
    
    public static <K, V> Matcher<Dictionary<K,V>> dictEntry(final K key, final V value) {
        return new BaseMatcher<Dictionary<K,V>>() {
            @Override
            public boolean matches(Object item) {
                Dictionary<K,V> object = (Dictionary<K,V>)item;
                return object.get(key).equals(value);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("dictionary containing " + key + "=" + value);
            }
        };
    }
    
    public static <K, V> Matcher<Dictionary<K,V>> dictEntry(final Matcher<K> keyMatcher, final Matcher<V> valueMatcher) {
        return new BaseMatcher<Dictionary<K,V>>() {
            @Override
            public boolean matches(Object item) {
                Dictionary<K,V> object = (Dictionary<K,V>)item;
                for(K k : Collections.list(object.keys())) {
                    if(keyMatcher.matches(k) && valueMatcher.matches(object.get(k))) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("dictionary containing entry matching key that ")
                        .appendDescriptionOf(keyMatcher)
                        .appendText(" and a value that ")
                        .appendDescriptionOf(valueMatcher);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                description.appendText("but was not found in ").appendValue(item);
            }                        
        };
    }    
    
    public static <K, V> Matcher<Dictionary<K,V>> dictSize(final int size) {
        return new BaseMatcher<Dictionary<K,V>>() {
            @Override
            public boolean matches(Object item) {
                Dictionary<K,V> object = (Dictionary<K,V>)item;
                return object.size() == size;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("dictionary size ").appendValue(size);
            }
        };
    }    
    
    public static <K, V> Matcher<Map<K, V>> mapSize(final int size) {
        return new BaseMatcher<Map<K, V>>() {
            @Override
            public boolean matches(Object item) {
                Map<K, V> m = (Map< K, V>) item;
                return m != null && m.size() == size;
            }

            @Override
            public void describeTo(Description description) {                
                description.appendText("map with size " + size);
            }
        };
    }
}
