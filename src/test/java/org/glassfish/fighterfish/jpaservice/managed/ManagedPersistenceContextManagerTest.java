
package org.glassfish.fighterfish.jpaservice.managed;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import org.glassfish.fighterfish.jpaservice.util.TestUtils;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.CheckMessage.CHECK_MESSAGE;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.expect;
import static org.glassfish.fighterfish.jpaservice.util.TestUtils.mockServiceRef;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author thor
 */
public class ManagedPersistenceContextManagerTest {
    
    public ManagedPersistenceContextManagerTest() {
    }

    @Test
    public void retrieving_current_persistence_context_should_fail_when_jta_is_unavailable() throws Exception {
        BundleContext ctx = mock(BundleContext.class);        
        final EntityManagerFactory emf = mock(EntityManagerFactory.class);
        final Map<String, Object> props = mock(Map.class);
        
        final ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);
                
        final ServiceReference<EntityManagerFactory> emfRef = mockServiceRef(EntityManagerFactory.class);
        
        expect(new javax.transaction.TransactionRequiredException("No " + TransactionSynchronizationRegistry.class.getName() + " service is available"), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                m.getCurrentPersistenceContext(emfRef, props);
            }
        }, CHECK_MESSAGE);
    }
    
    @Test
    public void retrieving_current_persistence_context_should_fail_when_no_active_transaction_unavailable() throws Exception {
        BundleContext ctx = mock(BundleContext.class);        
        TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
        
        final ServiceReference<EntityManagerFactory> emfRef = mockServiceRef(EntityManagerFactory.class);
        final Map<String, Object> props = mock(Map.class);
        
        final ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);
        
        m.transactionRegistry = tsr;
        
        expect(new javax.transaction.TransactionRequiredException("No transaction context active"), new TestUtils.TestRunnable() {
            @Override
            public void run() throws Exception {
                m.getCurrentPersistenceContext(emfRef, props);
            }
        }, CHECK_MESSAGE);
    }    
    
    @Test
    public void retrieving_current_context_should_try_to_retrieve_existing_context() throws Exception {
        BundleContext ctx = mock(BundleContext.class);        
        TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
        ServiceReference<EntityManagerFactory> emfRef = mockServiceRef(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        Map<String, Object> props = mock(Map.class);
        
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);        
        m.transactionRegistry = tsr;
        
        when(tsr.getTransactionKey()).thenReturn(new Object());
        when(emfRef.getBundle().getBundleContext().getService(emfRef).createEntityManager(props)).thenReturn(em);
                        
        m.getCurrentPersistenceContext(emfRef, props);
        
        verify(tsr).getResource(argThat(is(m.TSRMapKey)));
    }      
    
    @Test
    public void retrieving_current_context_should_create_new_one_if_none_exists() throws Exception {
        BundleContext ctx = mock(BundleContext.class);        
        TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
        ServiceReference<EntityManagerFactory> emfRef = mockServiceRef(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        Map<String, Object> props = mock(Map.class);
        
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);        
        m.transactionRegistry = tsr;
        
        when(emfRef.getBundle().getBundleContext().getService(emfRef).createEntityManager(props)).thenReturn(em);
        when(tsr.getTransactionKey()).thenReturn(new Object());        
        
        assertThat(m.getCurrentPersistenceContext(emfRef, props), is(em));
        
        verify(tsr).putResource(argThat(is(m.TSRMapKey)), argThat(is(instanceOf(Map.class))));
    }    
    
    @Test
    public void retrieving_current_context_register_interposed_sync_when_context_is_created() throws Exception {
        BundleContext ctx = mock(BundleContext.class);        
        TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
        ServiceReference<EntityManagerFactory> emfRef = mockServiceRef(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        Map<String, Object> props = mock(Map.class);
        
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);        
        m.transactionRegistry = tsr;
        
        when(emfRef.getBundle().getBundleContext().getService(emfRef).createEntityManager(props)).thenReturn(em);
        when(tsr.getTransactionKey()).thenReturn(new Object());        
        
        assertThat(m.getCurrentPersistenceContext(emfRef, props), is(em));
        
        verify(tsr).registerInterposedSynchronization((Synchronization)argThat(instanceOf(PersistenceSynchronization.class)));
    }      
    

    @Test
    public void adding_first_tran_syn_registry_should_keep_reference() throws Exception {
        
        BundleContext ctx = mock(BundleContext.class);        
        
        Bundle bundle = mock(Bundle.class);        
        BundleContext bundleContext = mock(BundleContext.class);        
        
        ServiceReference<TransactionSynchronizationRegistry> tsrRef = mock(ServiceReference.class);
        TransactionSynchronizationRegistry tsrInstance = mock(TransactionSynchronizationRegistry.class);
                
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);     
        
        
        when(tsrRef.getBundle()).thenReturn(bundle);
        when(ctx.getService(tsrRef)).thenReturn(tsrInstance);
                        
        assertThat(m.addingService(tsrRef), is(tsrInstance));   
    }
    

    @Test
    public void adding_another_tran_syn_registry_should_not_get_tracked() throws Exception {
        
        BundleContext ctx = mock(BundleContext.class);        
        
        Bundle bundle = mock(Bundle.class);        
        
        ServiceReference<TransactionSynchronizationRegistry> tsrRef = mock(ServiceReference.class);
        
        TransactionSynchronizationRegistry tsrInstance = mock(TransactionSynchronizationRegistry.class);
        TransactionSynchronizationRegistry existingInstance = mock(TransactionSynchronizationRegistry.class);
        
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);     
        
        
        m.transactionRegistry = existingInstance;
        
        when(tsrRef.getBundle()).thenReturn(bundle);
        when(ctx.getService(tsrRef)).thenReturn(tsrInstance);
                
        assertThat(m.addingService(tsrRef), is(nullValue(TransactionSynchronizationRegistry.class)));   
    }    
    
    @Test
    public void removing_trans_sync_registry_should_unget_service() throws Exception {
        
        BundleContext ctx = mock(BundleContext.class);        
        
        Bundle bundle = mock(Bundle.class);        
        BundleContext bundleContext = mock(BundleContext.class);        
        
        ServiceReference<TransactionSynchronizationRegistry> tsrRef = mock(ServiceReference.class);
        
        TransactionSynchronizationRegistry tsrInstance = mock(TransactionSynchronizationRegistry.class);
        TransactionSynchronizationRegistry existingInstance = mock(TransactionSynchronizationRegistry.class);
        
        ManagedPersistenceContextManager m = new ManagedPersistenceContextManager(ctx);     
        
        
        m.transactionRegistry = existingInstance;
        
        when(tsrRef.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(tsrRef)).thenReturn(tsrInstance);
                
        assertThat(m.addingService(tsrRef), is(nullValue(TransactionSynchronizationRegistry.class)));   
    }        
    
}
