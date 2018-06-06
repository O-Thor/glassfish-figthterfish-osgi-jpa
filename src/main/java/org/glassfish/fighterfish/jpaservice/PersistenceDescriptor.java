/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.fighterfish.jpaservice;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.spi.PersistenceUnitInfo;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceUnitXml;
import org.glassfish.fighterfish.jpaservice.xml.PersistenceXml;
import org.osgi.framework.Bundle;

/**
 *
 * @author thor
 */
public class PersistenceDescriptor {

    public PersistenceDescriptor(Bundle bundle) {
        this.bundle = Objects.requireNonNull(bundle);
    }   
    
    public void setPersistence(PersistenceXml persistence) {
        this.persistenceXml = persistence;
    }

    public PersistenceXml getPersistence() {
        return persistenceXml;
    }        

    public Set<PersistenceUnitInfo> getInfos() {
        if(infos == null) {
            Set<PersistenceUnitInfo> set = new HashSet<>(this.persistenceXml.getPersistenceUnit().size());
            for(PersistenceUnitXml pu : this.persistenceXml.getPersistenceUnit()) {
                set.add(new PersistenceUnitInfoImpl(bundle, this.persistenceXml, pu));
            }
            this.infos = set;
        }
        return infos;
    }
    
    private final Bundle bundle;
    private PersistenceXml persistenceXml;
    private Set<PersistenceUnitInfo> infos;
}
