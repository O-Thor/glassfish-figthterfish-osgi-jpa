
package org.glassfish.fighterfish.jpaservice.xml;

import java.util.Properties;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author thor
 */
public class PersistencePropertiesXmlAdapter extends XmlAdapter<PropertiesXml, java.util.Properties>{

    @Override
    public Properties unmarshal(PropertiesXml props) throws Exception {
        Properties propertyList = new Properties();
        if(props != null) {
            for(PropertyXml p : props.getProperty()) {
                propertyList.put(p.getName(), p.getValue());
            }
        }
        return propertyList;
    }

    @Override
    public PropertiesXml marshal(Properties v) throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }
   
}
