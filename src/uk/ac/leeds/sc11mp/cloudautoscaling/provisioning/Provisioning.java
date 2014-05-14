package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.host.HostPool;
import org.opennebula.xmlschema.HOSTPOOL;
import uk.ac.leeds.sc11mp.cloudautoscaling.exception.OpennebulaException;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy.Strategy;

/**
 *
 * @author sc11mp
 */
public class Provisioning {

    private Strategy strategy;
    private String vmTemplate;
    private List<Host> hosts;
    private Client oneClient;
    private String vmUsername;
    private String vmPassword;
    private int reservedMemory;
    private int vmAllocatedMemory;
    private String collectdConfigurationPath;
    private String collectdExecutable;
    private boolean deploying;
    
    private List<VirtualMachine> tomcatVms;
    private VirtualMachine httpVm;
    
    private final int MaxLoginRetries = 30;
    private final int MaxInstantiationRetries = 5;
    
    private static Logger logger = Logger.getLogger(Provisioning.class);
    
    private enum ApacheAction {
        Start,
        Reload
    }
    
    public Provisioning(Strategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        
        this.strategy = strategy;
        tomcatVms = new ArrayList<VirtualMachine>();
        deploying = false;
    }

    public Strategy getStrategy() {
        return strategy;
    }
    
    protected VirtualMachine allocateVm() throws OpennebulaException {
        logger.info("Allocating new VM");
        refreshHostAvailability();
        
        Host host = strategy.selectMachine(hosts);
        
        logger.debug("New VM will be allocated on host " + host.getHostId());
        
        OneResponse response = VirtualMachine.allocate(oneClient, vmTemplate);
        
        if (response.isError()) {
            logger.error("Could not allocate VM. Reason: " + response.getErrorMessage());
            throw new OpennebulaException("Could not allocate VM");
        } else {
            int vmId = Integer.parseInt(response.getMessage());
            
            VirtualMachine vm = new VirtualMachine(vmId, oneClient);
            response = vm.deploy(host.getHostId());
            vm.setHostId(host.getHostId());

            if (response.isError()) {
                logger.error("Could not deploy VM to selected host. Reason: " + response.getErrorMessage());
                throw new OpennebulaException("Could not deploy VM to selected host");
            } else {   
                logger.info("VM allocated and deployed");
                return vm;
            }      
        }
    }
    
    public void allocateWorker() throws OpennebulaException {
        deploying = true;
        boolean success = false;
        
        for (int i = 0; i < MaxInstantiationRetries && !success; i++) {
            VirtualMachine vm = null;
            try {
                logger.info("Adding new worker...");
                vm = null;
                vm = allocateVm();
                
                setupTomcatServer(vm);
                tomcatVms.add(vm);
                success = true;
                logger.info("Worker was successfuly instantiated");
            } catch (OpennebulaException e) {
                logger.error("Could not create new worker");
                
                if (vm != null) {
                    vm.cancel();
                }
            }
        }
        
        if (!success) {
            deploying = false;
            throw new OpennebulaException("Could not create new worker");
        }
        
        setupApacheServer(httpVm, ApacheAction.Reload);
        deploying = false;
        logger.info("Worker was successfully added to the cluster");
    }
    
    public void deallocateWorker() throws OpennebulaException {
        logger.info("Removing Tomcat worker");
        // Do not remove the last VM as there won't be an application running
        if (tomcatVms.size() > 1) {
            deploying = true;
            VirtualMachine vm = strategy.removeMachine(hosts, tomcatVms);
            
            tomcatVms.remove(vm);
            vm.cancel();
            
            setupApacheServer(httpVm, ApacheAction.Reload);
            
            deploying = false;
            logger.info("Worker removed");
        } else {
            logger.info("Did not remove worker. Only 1 worker left!");
        }
    }
    
    public void connect(String username, String password, String url) throws OpennebulaException {       
        logger.info("Connecting to the OpenNebula's API");
        try {
            oneClient = new Client(username + ":" + password, url);
        } catch (ClientConfigurationException e) {
            logger.error("Could not connect to the API", e);
            throw new IllegalArgumentException("Could not connect to the API");
        }
        
        // First, start HTTP Server...
        logger.info("Starting Apache");
        
        boolean success = false;
        for (int i = 0; i < MaxInstantiationRetries && !success; i++) {
            try {
                logger.info("Setting up Apache...");
                httpVm = null;
                httpVm = allocateVm();
                setupApacheServer(httpVm, ApacheAction.Start);
                success = true;
                logger.info("Apache was successfully started");
            } catch (OpennebulaException e) {
                logger.error("Could not instantiate Apache VM. Starting again.");
            }
        }
        
        if (!success) {
            throw new OpennebulaException("Could not start Apache");
        }
        
        // ... then add one worker
        success = false;
        
        for (int i = 0; i < MaxInstantiationRetries && !success; i++) {
            try {
                logger.info("Starting Tomcat cluster...");
                allocateWorker();
                success = true;
                logger.info("Tomcat cluster started.");
            } catch (OpennebulaException e) {
                logger.error("Could not start Tomcat cluster. Starting again.");
            }
        }
        
        if (!success) {
            throw new OpennebulaException("Could not start Tomcat cluster");
        }
    }
    
    protected void setupApacheServer(VirtualMachine vm, ApacheAction action) throws OpennebulaException {
        Session session = sshToVm(vm);
        
        try {
            ChannelExec channel = null;
            
            if (action == ApacheAction.Start) {
                executeCommand(session, "/etc/init.d/apache2 start");
                
                // setting up collectd
                String configuration = executeCommand(session, "cat " + collectdConfigurationPath + ";");
                configuration = configuration.replace("Listen \"\"", "Listen \"" + vm.getIp() + "\"")
                        .replace("Server \"\"", "")
                        .replace("\n", "\\n")
                        .replace("\"", "\\\"")
                        .replace("`", "\\`");
                
                String res = executeCommand(session, "echo -e \"" + configuration + "\" > " + collectdConfigurationPath + ";");
                
                executeCommand(session, collectdExecutable + ";");
            } else {
                StringBuilder workersFile = new StringBuilder();
                String tomcatInstances = "";
                String delimiter = "";

                workersFile.append("worker.list=balancer\\n");

                int counter = 1;
                for (VirtualMachine tomcatVm : tomcatVms) {
                    String nodeName = "tomcat" + counter;

                    tomcatInstances += delimiter + nodeName;
                    delimiter = ",";

                    workersFile.append("worker." + nodeName + ".type=ajp13\\n");
                    workersFile.append("worker." + nodeName + ".port=8009\\n");
                    workersFile.append("worker." + nodeName + ".host=" + tomcatVm.getIp() + "\\n");

                    counter++;
                }
                
                workersFile.append("worker.balancer.type=lb\\n");
                workersFile.append("worker.balancer.balance_workers=" + tomcatInstances);

                executeCommand(session, "echo -e \"" + workersFile.toString() + "\" > /etc/apache2/workers.properties;");
                executeCommand(session, "/etc/init.d/apache2 reload");
            }
            
            session.disconnect();
        } catch (Throwable e) {
            logger.error("Could not SSH into Apache's VM", e);
            throw new OpennebulaException("Could not SSH into Apache's VM");
        }
    }
    
    protected void setupTomcatServer(VirtualMachine vm) throws OpennebulaException {
        Session session = sshToVm(vm);
        try {
            executeCommand(session, "/opt/tomcat/bin/startup.sh;");
            
            // setting up collectd
            String configuration = executeCommand(session, "cat " + collectdConfigurationPath + ";");
                    
            configuration = configuration.replace("Listen \"\"", "")
                    .replace("Server \"\"", "Server \"" + httpVm.getIp() + "\"")
                    .replace("\n", "\\n")
                    .replace("\"", "\\\"")
                    .replace("`", "\\`");

            executeCommand(session, "echo -e \"" + configuration + "\" > " + collectdConfigurationPath + ";");
            executeCommand(session, collectdExecutable + ";");
        } catch (Throwable e) {
            logger.error("Could not set up Tomcat's VM");
            throw new OpennebulaException("Could not set up Tomcat's VM");
        }
        
        session.disconnect();
    }
    
    private Session sshToVm(VirtualMachine vm) throws OpennebulaException {
        boolean retry = true;
        int tries = 0;
        
        Session session = null;
        
        while (retry && tries < MaxLoginRetries) {
            try {
                JSch jsch = new JSch();
                
                Properties properties = new Properties();
                properties.put("StrictHostKeyChecking", "no");
                
                session = jsch.getSession(vmUsername, vm.getIp(), 22);
                session.setConfig(properties);
                session.setPassword(vmPassword);

                session.connect(30000);
                
                retry = false;
            } catch (JSchException ex) {
                tries++;
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex1) {
                    
                }
            }
        }
        
        if (retry == true) {
            logger.error("Could not SSH into VM " + vm.getId());
            
            throw new OpennebulaException("Could not SSH into VM");
        }
        
        return session;
    }
    
    protected void refreshHostAvailability() throws OpennebulaException{
        try {
            logger.info("Refreshing availability of hosts");
            
            JAXBContext jaxbContext = JAXBContext.newInstance(HOSTPOOL.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            HOSTPOOL pool = (HOSTPOOL) unmarshaller.unmarshal(new StringReader(HostPool.info(oneClient).getMessage()));
            
            for (HOSTPOOL.HOST hostInfo : pool.getHOST()) {
                
                for (Host host : hosts) {
                    if (host.getHostId() == hostInfo.getID().intValue()) {
                        
                        HOSTPOOL.HOST.HOSTSHARE hostShare = hostInfo.getHOSTSHARE();
                        if (hostShare.getCPUUSAGE().compareTo(hostShare.getMAXCPU()) < 0 &&
                                hostShare.getUSEDMEM().add(BigInteger.valueOf(getReservedMemory() + getVmAllocatedMemory())).compareTo(hostShare.getMAXMEM()) < 0) {
                            host.setAvailable(true);
                        } else {
                            host.setAvailable(false);
                        }
                        
                    }
                }
            }
            
            logger.info("Refreshed availability of hosts");
            
            for (Host host : hosts) {
                logger.debug("Host " + host.getHostId() + " is available? " + host.isAvailable());
            }
            
        } catch (JAXBException ex) {
            throw new OpennebulaException(ex);
        }
    }
    
    public void shutdown() {
        // Do not cancel the HTTP Server as the monitoring data
        // is stored on it.
        
        for (VirtualMachine vm : tomcatVms) {
            logger.info("Shutting down VM " + vm.getId());
            vm.cancel();
        }
    }
    
    private String executeCommand(Session session, String command) throws JSchException, IOException {
        String result = "";

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();
        
        // http://www.jcraft.com/jsch/examples/Exec.java.html
        
        InputStream input = channel.getInputStream();
        
        byte temp[] = new byte[1024];
        
        while (true) {
            while (input.available() > 0) {
                int read = input.read(temp, 0, 1024);
                
                if (read < 0) {
                    break;
                }
                
                result += new String(temp, 0, read);
            }
            
            if (channel.isClosed()) {
                if (input.available() > 0) {
                    continue;
                }
                break;
            }
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                
            }

        }
        
        channel.disconnect();
        return result;
    }

    public String getVmTemplate() {
        return vmTemplate;
    }

    public void setVmTemplate(String vmTemplate) {
        this.vmTemplate = vmTemplate;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public String getVmUsername() {
        return vmUsername;
    }

    public void setVmUsername(String vmUsername) {
        this.vmUsername = vmUsername;
    }

    public String getVmPassword() {
        return vmPassword;
    }

    public void setVmPassword(String vmPassword) {
        this.vmPassword = vmPassword;
    }

    public int getReservedMemory() {
        return reservedMemory;
    }

    public void setReservedMemory(int reservedMemory) {
        this.reservedMemory = reservedMemory;
    }

    public int getVmAllocatedMemory() {
        return vmAllocatedMemory;
    }

    public void setVmAllocatedMemory(int vmAllocatedMemory) {
        this.vmAllocatedMemory = vmAllocatedMemory;
    }
    
    public String getFrontIp() {
        return httpVm.getIp();
    }

    public int getClusterSize() {
        return tomcatVms.size();
    }

    public String getCollectdConfigurationPath() {
        return collectdConfigurationPath;
    }

    public void setCollectdConfigurationPath(String collectdConfigurationPath) {
        this.collectdConfigurationPath = collectdConfigurationPath;
    }

    public String getCollectdExecutable() {
        return collectdExecutable;
    }

    public void setCollectdExecutable(String collectdExecutable) {
        this.collectdExecutable = collectdExecutable;
    }

    public boolean isDeploying() {
        return deploying;
    }
}
