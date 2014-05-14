package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy;

import java.util.List;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.VirtualMachine;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;

/**
 *
 * @author sc11mp
 */

public class EnergyEfficientStrategy extends Strategy {

    @Override
    public Host selectMachine(List<Host> hosts) {
        List<Host> availableHosts = getAvailableHosts(hosts);
        Host bestHost = null;
        
        if (availableHosts.size() > 0) {
            bestHost = availableHosts.get(0);
            
            for (Host host : availableHosts) {
                if (host.getEnergyConsumptionCoefficient() < bestHost.getEnergyConsumptionCoefficient()) {
                    bestHost = host;
                }
            }
        }
        
        return bestHost;
    }

    @Override
    public VirtualMachine removeMachine(List<Host> hosts, List<VirtualMachine> vms) {
        VirtualMachine worstVm = vms.get(0);
        Host worstHost = null;
        
        for (Host host : hosts) {
            if (host.getHostId() == worstVm.getHostId()) {
                worstHost = host;
            }
        }

        for (Host host : hosts) {
            for (VirtualMachine vm : vms) {
                if (vm.getHostId() == host.getHostId()
                        && host.getEnergyConsumptionCoefficient() > worstHost.getEnergyConsumptionCoefficient()) {
                    worstHost = host;
                    worstVm = vm;
                    break;
                }
            }
        }

        return worstVm;
    }

}
