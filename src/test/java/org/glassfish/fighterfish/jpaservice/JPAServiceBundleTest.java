package org.glassfish.fighterfish.jpaservice;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import static org.glassfish.fighterfish.jpaservice.JPAConstants.DEFAULT_META_PERSITENCE;
import org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException;
import static org.glassfish.fighterfish.jpaservice.exception.JPAServiceSpecException.Type.NO_PERSISTENCE_PROVIDER_AVAILABLE;
import org.glassfish.fighterfish.jpaservice.util.TestUtils;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.dictEntry;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.dictSize;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.expect;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.mockDefaults;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author thor
 */
public class JPAServiceBundleTest {

    @Test
    public void constructor_should_fail_on_no_descriptors() {
        final BundleContext ctx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);

        mockDefaults(bundle);

        expect(new JPAServiceSpecException(JPAServiceSpecException.Type.NO_PERSISTENCE_UNITS_DEFINED, bundle),
                new TestUtils.TestRunnable() {
                    @Override
                    public void run() {
                        new JPAServiceBundle(ctx, bundle);
                    }
                }
        );
    }

    @Test
    public void constructor_should_read_descriptor() {
        final BundleContext ctx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);

        mockDefaults(bundle);
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(this.getClass().getResource("persistence_2_1.xml"));
        assertThat(new JPAServiceBundle(ctx, bundle).getDescriptor().getPersistence().getPersistenceUnit(), hasSize(1));
    }

    @Test
    public void constructor_should_read_empty_persistence_header_using_default_value() {
        final BundleContext ctx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);

        mockDefaults(bundle);

        URL pXml = this.getClass().getResource("persistence_2_1.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        
        try {
            JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(ctx, bundle);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        verify(bundle).getEntry(DEFAULT_META_PERSITENCE);
    }

    @Test
    public void constructor_should_validate_descriptors_for_1_0_to_2_1() {
        List<URL> urls = Arrays.asList(
                this.getClass().getResource("persistence_1_0i.xml"),
                this.getClass().getResource("persistence_2_0i.xml"),
                this.getClass().getResource("persistence_2_1i.xml")
        );
        for (URL pXml : urls) {
            final BundleContext ctx = mock(BundleContext.class);
            final Bundle bundle = mock(Bundle.class);

            mockDefaults(bundle);
            when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
            expect(
                    new JPAServiceSpecException(JPAServiceSpecException.Type.DESCRIPTOR_VALIDATION_FAILED, bundle),
                    new TestUtils.TestRunnable() {
                        @Override
                        public void run() throws Exception {
                            JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(ctx, bundle);
                        }
                    });
        }
    }
    
    @Test
    public void constructor_should_parse_descriptors_from_1_0_to_2_1() {
        List<URL> urls = Arrays.asList(
                this.getClass().getResource("persistence_1_0.xml"),
                this.getClass().getResource("persistence_2_0.xml"),
                this.getClass().getResource("persistence_2_1.xml")
        );
        for (URL pXml : urls) {
            final BundleContext ctx = mock(BundleContext.class);
            final Bundle bundle = mock(Bundle.class);

            mockDefaults(bundle);
            
            when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);    
            
            JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(ctx, bundle);
        }
    }
    
    @Test
    public void constructor_should_initialize_descriptor_from_xml() {
        final BundleContext ctx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);

        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        
        JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(ctx, bundle);
        assertThat( jpaServiceBundle.getDescriptor(),
                allOf(
                        notNullValue(),
                        hasProperty("persistence", 
                                hasProperty("persistenceUnit", hasSize(1))
                        ),
                        hasProperty("infos",  hasSize(1))
                )
        );
    }
    
    
    @Test
    public void initializer_should_throw_jpa_spec_exc_if_unavailable() {
        final BundleContext mgrCtx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);
        @SuppressWarnings("unchecked")
        final ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        final PersistenceProvider providerInstance = mock(PersistenceProvider.class);
        
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        //when(mgrCtx.getService(provider)).thenReturn(providerInstance);
        
        final JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(mgrCtx, bundle);
        jpaServiceBundle.setPersistenceProvider(provider);
        expect(new JPAServiceSpecException(NO_PERSISTENCE_PROVIDER_AVAILABLE, bundle), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                jpaServiceBundle.init();
            }
        });
                
        verify(mgrCtx).getService(provider);
    }
    
    @Test
    @SuppressWarnings({"unchecked", "unchecked", "unchecked", "unchecked", "unchecked"})
    public void initializer_should_create_containter_EMF_for_every_punit() {
        final BundleContext mgrCtx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);
        final EntityManagerFactory emf = mock(EntityManagerFactory.class);
        
        @SuppressWarnings("unchecked")
        final ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        final PersistenceProvider providerService = mock(PersistenceProvider.class);
        
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1_no_ds.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        when(mgrCtx.getService(provider)).thenReturn(providerService);        

        final JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(mgrCtx, bundle);
        PersistenceUnitInfo pui = jpaServiceBundle.getDescriptor().getInfos().iterator().next();
                        
        when(providerService.createContainerEntityManagerFactory(pui, pui.getProperties())).thenReturn(emf);        
                
        jpaServiceBundle.setPersistenceProvider(provider);
        jpaServiceBundle.init();
               
        //creates emfs..
        verify(providerService).createContainerEntityManagerFactory(pui, pui.getProperties());
        
        verify(mgrCtx).<EntityManagerFactory>registerService( 
            notNull(Class.class),
            (EntityManagerFactory)argThat(instanceOf(EntityManagerFactory.class)), 
            argThat(
                allOf(                    
                    dictEntry(JPAConstants.OSGI_UNIT_NAME, pui.getPersistenceUnitName()),                    
                    dictEntry(JPAConstants.OSGI_UNIT_VERSION, jpaServiceBundle.getDescriptor().getPersistence().getVersion()),
                    dictEntry(JPAConstants.OSGI_UNIT_PROVIDER, pui.getPersistenceProviderClassName())
                )
            )
        );
    }    
    
    @Test
    public void initializer_should_unget_provider_when_retrieved() {
        final BundleContext mgrCtx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);
        @SuppressWarnings("unchecked")
        final ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        final PersistenceProvider providerService = mock(PersistenceProvider.class);
        
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1_no_ds.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        when(mgrCtx.getService(provider)).thenReturn(providerService);
        
        final JPAServiceBundle jpaServiceBundle = new JPAServiceBundle(mgrCtx, bundle);
        jpaServiceBundle.setPersistenceProvider(provider);
        jpaServiceBundle.init();      
        
        verify(mgrCtx).ungetService(provider);
    }
    
    /**
     * Handle integration
     * 
     * @see JPAServiceBundle#handleSpecialJpaIntegration(javax.persistence.spi.PersistenceUnitInfo) 
     */
    @Test
    @SuppressWarnings({"unchecked", "unchecked", "unchecked", "unchecked"})
    public void initializer_should_handle_special_integration_problems_with_eclipse_link() {
        final BundleContext mgrCtx = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);
        final EntityManagerFactory emf = mock(EntityManagerFactory.class);
        
        @SuppressWarnings("unchecked")
        final ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        final PersistenceProvider providerService = mock(PersistenceProvider.class);
        
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1_no_ds.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        when(mgrCtx.getService(provider)).thenReturn(providerService);        

        final JPAServiceBundle jpaServiceBundle = spy(new JPAServiceBundle(mgrCtx, bundle));
        
        PersistenceUnitInfo pui = jpaServiceBundle.getDescriptor().getInfos().iterator().next();
                        
        when(providerService.createContainerEntityManagerFactory(pui, pui.getProperties())).thenReturn(emf);        
                
        jpaServiceBundle.setPersistenceProvider(provider);
                
        int dictSize = pui.getProperties().size();
        
        assertThat(pui.getProperties(), 
                allOf(
                        not(
                                dictEntry((Matcher)is("eclipselink.target-server"), is(System.getProperty("eclipselink.target-server", "SunAS9")))
                        ), 
                        dictSize(dictSize)
                )
        );
        
        jpaServiceBundle.init();
        
        assertThat(pui.getProperties(), 
                allOf(
                        dictEntry((Matcher)is("eclipselink.target-server"), is(System.getProperty("eclipselink.target-server", "SunAS9"))),
                        dictSize(dictSize+1)
                )
        );
        
        verify(jpaServiceBundle).handleSpecialJpaIntegration(argThat(is(pui)));                
    }
        
    
    @Test
    public void updating_provider_should_never_call_uninit_when_not_initialized() {
        BundleContext mgrCtx = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        @SuppressWarnings("unchecked")
        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1_no_ds.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        when(mgrCtx.getService(provider)).thenReturn(providerService);
        
        final JPAServiceBundle jpaServiceBundle = spy(new JPAServiceBundle(mgrCtx, bundle));        
        
        doNothing().when(jpaServiceBundle).init();
        
        jpaServiceBundle.updateProvider(provider);
        
        verify(jpaServiceBundle, never()).uninit();                
        verify(jpaServiceBundle).init();  
    }
    
    @Test
    public void updating_provider_should_call_unit_if_already_initialized() {
        BundleContext mgrCtx = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        @SuppressWarnings("unchecked")
        ServiceReference<PersistenceProvider> provider = mock(ServiceReference.class);
        PersistenceProvider providerService = mock(PersistenceProvider.class);
        mockDefaults(bundle);
        
        URL pXml = this.getClass().getResource("persistence_2_1_no_ds.xml");
        when(bundle.getEntry(DEFAULT_META_PERSITENCE)).thenReturn(pXml);
        when(mgrCtx.getService(provider)).thenReturn(providerService);
        
        final JPAServiceBundle jpaServiceBundle = spy(new JPAServiceBundle(mgrCtx, bundle));
        jpaServiceBundle.setPersistenceProvider(provider);
        jpaServiceBundle.initialized = true;        
        
        doNothing().when(jpaServiceBundle).init();
        doNothing().when(jpaServiceBundle).uninit();
        
        jpaServiceBundle.updateProvider(provider);
        
        verify(jpaServiceBundle).uninit();                
        verify(jpaServiceBundle).init();    
    }    

}
