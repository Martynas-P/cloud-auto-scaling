package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy;

import java.util.List;
import java.util.Random;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.VirtualMachine;

/**
 * Selects a random host to allocate VM on
 * @author sc11mp
 */
public class RandomStrategy extends Strategy {

    private Random random;
    
    public RandomStrategy() {
        random = new Random();
    }
    
    @Override
    public Host selectMachine(List<Host> hosts) {
        
        List<Host> availableHosts = getAvailableHosts(hosts);
        
        for (Host host : hosts) {
            availableHosts.add(host);
        }
        
        Host selectedHost = null;
        
        if (availableHosts.size() > 0) {
            selectedHost = availableHosts.get(random.nextInt(availableHosts.size()));
        }
        
        return selectedHost;
    }

    @Override
    public VirtualMachine removeMachine(List<Host> hosts, List<VirtualMachine> vms) {
        VirtualMachine vm = null;
        
        if (vms.size() > 0) {
            vm = vms.get(random.nextInt(vms.size()));
        }
        
        return vm;
    }
    
}