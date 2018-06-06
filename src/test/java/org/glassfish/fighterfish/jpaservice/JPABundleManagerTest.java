
package org.glassfish.fighterfish.jpaservice;

import java.net.URL;
import static java.util.Arrays.asList;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.persistence.spi.PersistenceProvider;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import org.glassfish.fighterfish.jpaservice.util.TestUtils;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.expect;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.mockDefaults;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.mockServiceRef;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceUnitXml;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceXml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import org.osgi.framework.*;

/**
 *
 * @author thor
 */
public class JPABundleManagerTest {

    public JPABundleManagerTest() {
    }

    @Test
    public void addingBundle_should_not_create_tasks_for_bundles_that_are_stopping() {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);

        BundleEvent event = mock(BundleEvent.class);

        mockDefaults(bundle);

        when(bundle.getState()).thenReturn(Bundle.STOPPING);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        assertThat(m.addingBundle(bundle, event), is(nullValue()));

        verify(m, times(0)).createTask(bundle);
    }

    @Test
    public void addingBundle_should_submit_creation_task_for_bundles_containing_meta_header() {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);

        BundleEvent event = mock(BundleEvent.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        mockDefaults(bundle);

        bundle.getHeaders().put(JPAConstants.HEADER_META_PERSISTENCE, "");

        when(bundle.getState()).thenReturn(Bundle.STARTING);

        doReturn(mock(Future.class)).when(m).createTask(bundle);

        assertThat(m.addingBundle(bundle, event), is(instanceOf(Future.class)));

        verify(m).createTask(bundle);
    }

    @Test
    public void addingBundle_should_not_submit_creation_task_for_bundles_missing_meta_header() {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);

        BundleEvent event = mock(BundleEvent.class);

        mockDefaults(bundle);

        when(bundle.getState()).thenReturn(Bundle.STARTING);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        assertThat(m.addingBundle(bundle, event), is(nullValue()));

        verify(m, never()).createTask(bundle);
    }

    @Test
    public void tryyUninit_should_try_to_cancel_if_not_done_before_uninitializing() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        when(future.isDone()).thenReturn(Boolean.FALSE);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.tryUninit(bundle, future);

        verify(future).cancel(true);
        verify(jPAServiceBundle).uninit();
    }

    @Test
    public void tryyUninit_should_NOT_try_to_cancel_if_jpaservicebundle_is_already_initialized() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        when(future.isDone()).thenReturn(Boolean.TRUE);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.tryUninit(bundle, future);

        verify(future, never()).cancel(anyBoolean());
        verify(jPAServiceBundle).uninit();
    }

    @Test
    public void modifying_bundle_should_cause_initializetion_to_cancel() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        BundleEvent event = mock(BundleEvent.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        doNothing().when(m).tryUninit(bundle, future);
        when(event.getType()).thenReturn(BundleEvent.UPDATED);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.modifiedBundle(bundle, event, future);

        verify(m).tryUninit(bundle, future);
    }

    @Test
    public void modifying_bundle_should_initialize_jpaservice_bundle() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        BundleEvent event = mock(BundleEvent.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        doNothing().when(m).tryUninit(bundle, future);
        when(event.getType()).thenReturn(BundleEvent.UPDATED);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.modifiedBundle(bundle, event, future);

        verify(jPAServiceBundle).init();
    }

    @Test
    public void modifedBundle_when_uninit_fails_should_not_reinit() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        BundleEvent event = mock(BundleEvent.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        doThrow(RuntimeException.class).when(m).tryUninit(bundle, future);
        when(event.getType()).thenReturn(BundleEvent.UPDATED);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.modifiedBundle(bundle, event, future);

        verify(m).tryUninit(bundle, future);
        verify(jPAServiceBundle, never()).init();
    }

    @Test
    public void removedBundle_should_uninitialize_jpa_service_bundle() throws Exception {
        Bundle bundle = mock(Bundle.class);
        BundleContext ctx = mock(BundleContext.class);
        BundleEvent event = mock(BundleEvent.class);
        Future<JPAServiceBundle> future = mock(Future.class);
        JPAServiceBundle jPAServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        doNothing().when(m).tryUninit(bundle, future);
        when(event.getType()).thenReturn(BundleEvent.UPDATED);

        when(future.get()).thenReturn(jPAServiceBundle);
        when(future.get(anyLong(), (TimeUnit)argThat(notNullValue()))).thenReturn(jPAServiceBundle);

        m.removedBundle(bundle, event, future);

        verify(m).tryUninit(bundle, future);
        verify(jPAServiceBundle).setPersistenceProvider(null);

        assertThat(m.jpaServiceConsumers.containsKey(jPAServiceBundle), is(false));
        assertThat(m.jpaServiceUnprovidedConsumers.contains(jPAServiceBundle), is(false));
    }

    @Test
    public void addingService_should_try_to_find_the_best_provider_for_unprovided_consumers() {
        BundleContext ctx = mock(BundleContext.class);
        
        
        JPAServiceBundle unprovidedBundle = mock(JPAServiceBundle.class);
        Bundle bundle = mock(Bundle.class);
        mockDefaults(bundle);

        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.jpaServiceUnprovidedConsumers.add(unprovidedBundle);
        
        when(bundle.getHeaders()).thenReturn(new Hashtable<String, String>());

        when(provider.getBundle()).thenReturn(bundle);
        when(ctx.getService(provider)).thenReturn(providerService);
        
        doReturn(provider).when(m).findPersistenceProvider(unprovidedBundle);
        
        assertThat(m.addingService(provider), is(equalTo(providerService)));
        
        verify(m).findPersistenceProvider((JPAServiceBundle)any());
        verify(unprovidedBundle).updateProvider(provider);
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(0));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(1));
    }


    @Test
    public void addingService_should_not_try_updating_provider_if_none_is_found_for_unprovided_bundles() {
        BundleContext ctx = mock(BundleContext.class);
        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        JPAServiceBundle unprovidedBundle = mock(JPAServiceBundle.class);
        Bundle bundle = mock(Bundle.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.jpaServiceUnprovidedConsumers.add(unprovidedBundle);

        when(provider.getBundle()).thenReturn(bundle);
        when(ctx.getService(provider)).thenReturn(providerService);
        doReturn(null).when(m).findPersistenceProvider(unprovidedBundle);

        assertThat(m.addingService(provider), is(equalTo(providerService)));

        verify(m).findPersistenceProvider((JPAServiceBundle)any());
        verify(unprovidedBundle, never()).updateProvider(provider);
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(1));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(0));
    }

    @Test
    public void addingService_should__try_updating_provider_if_found_but_keep_in_unprovided_queue_if_fails_updating() {
        BundleContext ctx = mock(BundleContext.class);
        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        JPAServiceBundle unprovidedBundle = mock(JPAServiceBundle.class);
        Bundle bundle = mock(Bundle.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.jpaServiceUnprovidedConsumers.add(unprovidedBundle);
        
        when(provider.getBundle()).thenReturn(bundle);
        when(ctx.getService(provider)).thenReturn(providerService);
        doReturn(provider).when(m).findPersistenceProvider(unprovidedBundle);
        doThrow(RuntimeException.class).when(unprovidedBundle).updateProvider(provider);

        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(1));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(0));

        assertThat(m.addingService(provider), is(equalTo(providerService)));

        verify(m).findPersistenceProvider(unprovidedBundle);
        verify(unprovidedBundle).updateProvider(provider);
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(1));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(0));
    }

    @Test
    public void addingService_should_add_serviceReference_to_provider_set() {
        BundleContext ctx = mock(BundleContext.class);
        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerInstance = mock(PersistenceProvider.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));

        when(ctx.getService(provider)).thenReturn(providerInstance);
        doReturn(null).when(m).findPersistenceProvider((JPAServiceBundle) argThat(nullValue()));

        assertThat(m.addingService(provider), is(providerInstance));
        assertThat(m.providers, hasSize(1));

    }

    @Test
    public void removedService_should_remove_provider_reference() {
        BundleContext ctx = mock(BundleContext.class);

        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);

        JPAServiceBundle unprovidedJpaServiceBundle = mock(JPAServiceBundle.class);

        Bundle providedBundle = mock(Bundle.class);
        JPAServiceBundle providedJpaServiceBundle = mock(JPAServiceBundle.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.providers.add(provider);
        m.jpaServiceConsumers.put(providedJpaServiceBundle, provider);
        m.jpaServiceUnprovidedConsumers.add(unprovidedJpaServiceBundle);

        m.removedService(provider, providerService);

        assertThat(m.providers, hasSize(0));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(1));
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(1));
    }

    @Test
    public void removedService_should_remove_provider_referene_from_consumers_and_uninit() {
        BundleContext ctx = mock(BundleContext.class);

        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.providers.add(provider);

        m.removedService(provider, providerService);

        assertThat(m.providers, hasSize(0));
    }

    @Test
    public void removedService_should_uninitialize_reference_consumers() {
        BundleContext ctx = mock(BundleContext.class);

        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);


        Bundle consumerBundle = mock(Bundle.class);
        JPAServiceBundle consumerJpaServiceBundle = mock(JPAServiceBundle.class);

        when(consumerJpaServiceBundle.getBundle()).thenReturn(consumerBundle);
        when(consumerJpaServiceBundle.getPersistenceProvider()).thenReturn(provider);

        JPABundleManager m = spy(new JPABundleManager(ctx));
        m.providers.add(provider);
        m.trackingBundles.put(consumerBundle, consumerJpaServiceBundle);
        m.jpaServiceConsumers.put(consumerJpaServiceBundle, provider);

        assertThat(m.providers, hasSize(1));
        assertThat(m.trackingBundles.entrySet(), hasSize(1));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(1));
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(0));

        m.removedService(provider, providerService);

        assertThat(m.providers, hasSize(0));
        assertThat(m.jpaServiceConsumers.entrySet(), hasSize(0));
        assertThat(m.jpaServiceUnprovidedConsumers, hasSize(1));

        verify(consumerJpaServiceBundle).uninit();
    }

    @Test
    public void findPersistenceProvider_should_fail_if_more_than_one_provider() throws InvalidSyntaxException {
        BundleContext ctx = mock(BundleContext.class);
        Bundle consumerBundle = mock(Bundle.class);
        mockDefaults(consumerBundle);
        when(consumerBundle.getEntry(JPAConstants.DEFAULT_META_PERSITENCE)).thenReturn(this.getClass().getResource("persistence_2_1_multi_providers.xml"));
        when(consumerBundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        when(ctx.getServiceReferences(PersistenceProvider.class, null)).thenReturn(Arrays.<ServiceReference<PersistenceProvider>>asList(mock(ServiceReference.class), mock(ServiceReference.class)));

        final JPAServiceBundle consumerJpaServiceBundle = new JPAServiceBundle(ctx, consumerBundle);

        final JPABundleManager m = new JPABundleManager(ctx);

        expect(new JPAServiceSpecException(JPAServiceSpecException.Type.DESCRIPTOR_VALIDATION_FAILED_MULTIPLE_PROVIDERS, consumerBundle), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                m.findPersistenceProvider(consumerJpaServiceBundle);
            }
        });
    }

    @Test
    public void findPersistenceProvider_should_return_first_if_no_provider_specified() throws InvalidSyntaxException {
        BundleContext ctx = mock(BundleContext.class);
        Bundle consumerBundle = mock(Bundle.class);
        mockDefaults(consumerBundle);
        when(consumerBundle.getEntry(JPAConstants.DEFAULT_META_PERSITENCE)).thenReturn(this.getClass().getResource("persistence_2_1_multi_pu_no_providers_specified.xml"));
        when(consumerBundle.getHeaders()).thenReturn(new Hashtable<String, String>());

        List<ServiceReference<PersistenceProvider>> providers = Arrays.<ServiceReference<PersistenceProvider>>asList(mock(ServiceReference.class), mock(ServiceReference.class));
        when(ctx.getServiceReferences(PersistenceProvider.class, null)).thenReturn(providers);

        final JPAServiceBundle consumerJpaServiceBundle = new JPAServiceBundle(ctx, consumerBundle);
        final JPABundleManager m = new JPABundleManager(ctx);

        assertThat(m.findPersistenceProvider(consumerJpaServiceBundle), is(providers.get(0)));
    }
    
}
