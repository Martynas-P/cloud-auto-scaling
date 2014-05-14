package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy;

import java.util.ArrayList;
import java.util.List;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.VirtualMachine;

/**
 * Interface for VM allocation strategies
 * @author sc11mp
 */
public abstract class Strategy {
    
    abstract public Host selectMachine(List<Host> hosts);
    abstract public VirtualMachine removeMachine(List<Host> hosts, List<VirtualMachine> vms);
    
    protected List<Host> getAvailableHosts(List<Host> hosts) {
        List<Host> availableHosts = new ArrayList<Host>();
        
        for (Host host : hosts) {
            if (host.isAvailable()) {
                availableHosts.add(host);
            }
        }
        
        return availableHosts;
    }
}
