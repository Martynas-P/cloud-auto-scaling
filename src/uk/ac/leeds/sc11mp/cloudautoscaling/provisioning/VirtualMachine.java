package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning;

import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.opennebula.client.Client;
import org.opennebula.xmlschema.VM;
import org.w3c.dom.Node;

/**
 *
 * @author sc11mp
 */
public class VirtualMachine extends org.opennebula.client.vm.VirtualMachine {

    private String ip;
    private int hostId;
    
    public VirtualMachine(int i, Client client) {
        super(i, client);
        
        if (client != null) {
            getVmInfo();
        }
    }

    public VirtualMachine(Node node, Client client) {
        super(node, client);
        
        getVmInfo();
    }
    
    private void getVmInfo() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(VM.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            VM vm = (VM) unmarshaller.unmarshal(new StringReader(info().getMessage()));
            
            ip = vm.getTEMPLATE().getNIC().getIP();
        } catch (JAXBException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }
    
}
