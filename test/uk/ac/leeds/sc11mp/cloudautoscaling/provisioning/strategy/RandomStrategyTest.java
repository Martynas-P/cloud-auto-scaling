/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy;

import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.VirtualMachine;

/**
 *
 * @author martynas
 */
public class RandomStrategyTest {
    
    private static RandomStrategy strategy;
    
    public RandomStrategyTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        strategy = new RandomStrategy();
    }

    @Test
    public void testSelect1() {
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
        assertNotNull(result);
    }

    @Test
    public void testSelect2() {
        List<Host> hosts = new ArrayList<Host>();
        Host host;
        host = new Host(0, 1.0);
        host.setAvailable(true);
        hosts.add(host);

        Host result = strategy.selectMachine(hosts);
        assertNotNull(result);
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
        
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        VirtualMachine vm;
        
        vm = new VirtualMachine(0, null);
        vm.setHostId(0);
        vms.add(vm);

        vm = new VirtualMachine(1, null);
        vm.setHostId(0);
        vms.add(vm);

        VirtualMachine result = strategy.removeMachine(hosts, vms);

        assertNotNull(result);
        assertEquals(0, result.getHostId());
    }
    
}
