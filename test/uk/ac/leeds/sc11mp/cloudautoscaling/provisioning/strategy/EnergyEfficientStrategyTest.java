package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.VirtualMachine;

/**
 *
 * @author martynas
 */
public class EnergyEfficientStrategyTest {

    private static EnergyEfficientStrategy strategy;

    @BeforeClass
    public static void setUp() throws ClientConfigurationException {
        strategy = new EnergyEfficientStrategy();
    }

    @Test
    public void testAllocate1() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.1);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 1.2);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(3, 1.3);
        host.setAvailable(true);
        hosts.add(host);

        Host result = strategy.selectMachine(hosts);
        assertEquals(0, result.getHostId());
    }

    @Test
    public void testAllocate2() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 0.9);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 0.8);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(3, 0.7);
        host.setAvailable(true);
        hosts.add(host);

        Host result = strategy.selectMachine(hosts);
        assertEquals(3, result.getHostId());
    }

    @Test
    public void testAllocate3() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.1);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 0.9);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(3, 1.3);
        host.setAvailable(true);
        hosts.add(host);

        Host result = strategy.selectMachine(hosts);
        assertEquals(2, result.getHostId());
    }

    @Test
    public void testAllocate4() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 1.3);
        host.setAvailable(true);
        hosts.add(host);

        Host result = strategy.selectMachine(hosts);
        assertNotNull(result);
        assertTrue(result.getHostId() != 2);
    }
    
    @Test
    public void testRemove1() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.1);
        host.setAvailable(true);
        hosts.add(host);
        
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        VirtualMachine vm;
        
        vm = new VirtualMachine(0, null);
        vm.setHostId(0);
        vms.add(vm);

        vm = new VirtualMachine(1, null);
        vm.setHostId(1);
        vms.add(vm);

        VirtualMachine result = strategy.removeMachine(hosts, vms);

        assertNotNull(result);
        assertEquals(1, result.getHostId());
    }
    
    @Test
    public void testRemove2() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.1);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 1.2);
        host.setAvailable(true);
        hosts.add(host);
        
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        VirtualMachine vm;
        
        vm = new VirtualMachine(0, null);
        vm.setHostId(0);
        vms.add(vm);

        vm = new VirtualMachine(1, null);
        vm.setHostId(1);
        vms.add(vm);

        VirtualMachine result = strategy.removeMachine(hosts, vms);

        assertNotNull(result);
        assertEquals(1, result.getHostId());
    }
    
    @Test
    public void testRemove3() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(1, 1.1);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(2, 1.1);
        host.setAvailable(true);
        hosts.add(host);
        
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        VirtualMachine vm;
        
        vm = new VirtualMachine(0, null);
        vm.setHostId(0);
        vms.add(vm);

        vm = new VirtualMachine(1, null);
        vm.setHostId(1);
        vms.add(vm);

        vm = new VirtualMachine(2, null);
        vm.setHostId(2);
        vms.add(vm);

        VirtualMachine result = strategy.removeMachine(hosts, vms);

        assertNotNull(result);
        assertTrue(result.getHostId() != 0);
    }
    
    @Test
    public void testRemove4() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(3, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(4, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(5, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(6, 1.1);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(7, 1.2);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(8, 1.3);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(9, 0.8);
        host.setAvailable(true);
        hosts.add(host);

        host = new Host(10, 0.9);
        host.setAvailable(true);
        hosts.add(host);
        
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        VirtualMachine vm;
        
        vm = new VirtualMachine(0, null);
        vm.setHostId(9);
        vms.add(vm);

        vm = new VirtualMachine(1, null);
        vm.setHostId(9);
        vms.add(vm);

        VirtualMachine result = strategy.removeMachine(hosts, vms);

        assertNotNull(result);
        assertTrue(result.getHostId() == 9);
    }

}
