
package org.glassfish.fighterfish.jpaservice.managed;

import java.util.Hashtable;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.glassfish.fighterfish.jpaservice.JPAConstants;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.MANAGED_PERSISTENCE_CONTEXT_ALREADY_REGISTERED;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.MANAGED_PERSISTENCE_CONTEXT_DOES_NOT_EXIST;
import org.glassfish.fighterfish.jpaservice.util.TestUtils;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.expect;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.mapSize;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.argThat;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author thor
 */
public class ManagedPersistenceManagerTest {

    public ManagedPersistenceManagerTest() {
    }

    @Test
    @SuppressWarnings({"unchecked", "unchecked"})
    public void addingService_should_register_managed_emf() {
        ManagedPersistenceContextManager mjemfm = mock(ManagedPersistenceContextManager.class);
        BundleContext ctx = mock(BundleContext.class);
        ManagedPersistenceManager m = new ManagedPersistenceManager(ctx, mjemfm);
        @SuppressWarnings("unchecked")
        ServiceReference<EntityManagerFactory> emfReference = mock(ServiceReference.class);
        ServiceRegistration<EntityManagerFactory> emfRegistration = mock(ServiceRegistration.class);

        EntityManagerFactory emfService = mock(EntityManagerFactory.class);

        when(emfReference.getProperty(JPAConstants.OSGI_UNIT_NAME)).thenReturn("test-unit");
        when(emfReference.getProperty(JPAConstants.OSGI_UNIT_VERSION)).thenReturn("2.1");
        when(ctx.registerService(
                argThat(equalTo(EntityManagerFactory.class)),
                Mockito.<EntityManagerFactory>argThat((Matcher)is(instanceOf(ManagedEntityManagerFactory.class))),
                Mockito.<Hashtable<String, Object>>any()))
            .thenReturn(emfRegistration);
        when(ctx.getService(emfReference)).thenReturn(emfService);

        m.open();

        assertThat(m.addingService(emfReference), is(instanceOf(EntityManagerFactory.class)));

        verify(ctx).registerService(
            argThat(equalTo(EntityManagerFactory.class)),
            (EntityManagerFactory)argThat(instanceOf(ManagedEntityManagerFactory.class)),
                argThat(
                    allOf(
                        TestUtils.<String, Object>dictEntry(JPAConstants.OSGI_UNIT_NAME,                "test-unit"),
                        TestUtils.<String, Object>dictEntry(JPAConstants.OSGI_UNIT_VERSION,             "2.1"),
                        TestUtils.<String, Object>dictEntry(JPAConstants.OSGI_UNIT_PROVIDER,            JPAConstants.MANAGED_PROVIDER),
                        TestUtils.<String, Object>dictEntry(JPAConstants.MANAGED_PERSISTENCE_CONTEXT,   true),
                        TestUtils.<String, Object>dictEntry(JPAConstants.MANAGED_DELEGATE_EMF,          true)
                    )
                )
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addingService_should_throw_ex_if_unit_name_already_exists_and_not_removed_within_wait_period() {

        Bundle dup_bundle = mock(Bundle.class);

        ManagedPersistenceContextManager mjemfm = mock(ManagedPersistenceContextManager.class);
        BundleContext ctx = mock(BundleContext.class);

        @SuppressWarnings("unchecked")
        ServiceRegistration<EntityManagerFactory> emfRegistration = mock(ServiceRegistration.class);
        @SuppressWarnings("unchecked")
        final ServiceReference<EntityManagerFactory> emf_duplicate_registration = mock(ServiceReference.class);


        String unitName = "test-unit";

        when(emf_duplicate_registration.getProperty(JPAConstants.OSGI_UNIT_NAME)).thenReturn(unitName);
        when(emf_duplicate_registration.getBundle()).thenReturn(dup_bundle);
        final ManagedPersistenceManager m = new ManagedPersistenceManager(ctx, mjemfm);
        m.open();
        m.registrations.put(unitName, emfRegistration);

        expect(new JPAServiceSpecException(MANAGED_PERSISTENCE_CONTEXT_ALREADY_REGISTERED, dup_bundle), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                m.addingService(emf_duplicate_registration);
            }
        });
    }

    @Test
    public void removedService_should_throw_ex_if_not_registered() {
        Bundle bundle = mock(Bundle.class);

        ManagedPersistenceContextManager mjemfm = mock(ManagedPersistenceContextManager.class);
        BundleContext ctx = mock(BundleContext.class);

        @SuppressWarnings("unchecked")
        final ServiceReference<EntityManagerFactory> emfRef = mock(ServiceReference.class);
        final EntityManagerFactory emfInstance = mock(EntityManagerFactory.class);

        when(emfRef.getBundle()).thenReturn(bundle);

        final ManagedPersistenceManager m = new ManagedPersistenceManager(ctx, mjemfm);
        m.open();

        expect(new JPAServiceSpecException(MANAGED_PERSISTENCE_CONTEXT_DOES_NOT_EXIST, bundle), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                m.removedService(emfRef, emfInstance);
            }
        });
    }

    @Test
    public void removedService_should_remove_reference_and_unregister() {
        Bundle bundle = mock(Bundle.class);

        ManagedPersistenceContextManager mjemfm = mock(ManagedPersistenceContextManager.class);
        BundleContext ctx = mock(BundleContext.class);

        @SuppressWarnings("unchecked")
        final ServiceRegistration<EntityManagerFactory> emfReg = mock(ServiceRegistration.class);
        final ServiceReference<EntityManagerFactory> emfRef = mock(ServiceReference.class);
        final EntityManagerFactory emfInstance = mock(EntityManagerFactory.class);

        String puName = "test-unit";
        when(emfRef.getProperty(JPAConstants.OSGI_UNIT_NAME)).thenReturn(puName);
        when(emfRef.getBundle()).thenReturn(bundle);

        final ManagedPersistenceManager m = new ManagedPersistenceManager(ctx, mjemfm);
        m.open();
        m.registrations.put(puName, emfReg);
        m.removedService(emfRef, emfInstance);
        verify(emfReg).unregister();
    }

    @Test
    @SuppressWarnings({"unchecked", "unchecked"})
    public void closing_should_unregister_all_registrations_and_cleanup() {
    Bundle bundle = mock(Bundle.class);
        ManagedPersistenceContextManager mjemfm = mock(ManagedPersistenceContextManager.class);
        BundleContext ctx = mock(BundleContext.class);
        final ServiceRegistration<EntityManagerFactory> emfReg = mock(ServiceRegistration.class);
        final ServiceRegistration<EntityManagerFactory> emfReg2 = mock(ServiceRegistration.class);
        final ManagedPersistenceManager m = new ManagedPersistenceManager(ctx, mjemfm);
        m.open();
        m.registrations.put("1", emfReg);
        m.registrations.put("2", emfReg2);
        m.close();
        verify(emfReg).unregister();
        verify(emfReg2).unregister();
        assertThat(m.registrations, is(anyOf((Matcher)nullValue(), mapSize(1))));
    }
}
