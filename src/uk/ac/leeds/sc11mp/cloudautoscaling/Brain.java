package uk.ac.leeds.sc11mp.cloudautoscaling;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import uk.ac.leeds.sc11mp.cloudautoscaling.exception.OpennebulaException;
import uk.ac.leeds.sc11mp.cloudautoscaling.monitoring.Monitoring;
import uk.ac.leeds.sc11mp.cloudautoscaling.prediction.Prediction;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Host;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.Provisioning;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy.EnergyEfficientStrategy;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy.RandomStrategy;
import uk.ac.leeds.sc11mp.cloudautoscaling.provisioning.strategy.Strategy;

public class Brain {

    private Prediction predictionModule;
    private Provisioning provisioningModule;
    private Monitoring monitoringModule;
    
    private ResponseTimeListener responseTimeListener;
    private RequestCountListener requestCountListener;
    
    private static Logger logger = Logger.getLogger(Brain.class);
    
    // Listeners are defined as inner classes so that they could access 
    // modules more easily
    private class ResponseTimeListener implements Observer {

        private Long minResponseTime;
        private Long maxResponseTime;
        
        private Logger logger = Logger.getLogger(ResponseTimeListener.class);
        
        @Override
        public void update(Observable o, Object arg) {
            Long responseTime = monitoringModule.getResponseTime();
            
            logger.debug("Response time: " + responseTime);
            
            try {
                
                if (!provisioningModule.isDeploying()) {
                    if (responseTime.compareTo(minResponseTime) < 0 && provisioningModule.getClusterSize() > 1) {
                        logger.info("removing worker");
                        monitoringModule.stopMonitoringResponseTime();
                        provisioningModule.deallocateWorker();
                        monitoringModule.clearAverage();
                        logger.info("worker removed");
                        monitoringModule.restartRespnseMonitoringTime();
                    } else if (responseTime.compareTo(maxResponseTime) > 0) {
                        logger.info("adding worker");
                        monitoringModule.stopMonitoringResponseTime();
                        provisioningModule.allocateWorker();
                        monitoringModule.clearAverage();
                        monitoringModule.restartRespnseMonitoringTime();
                        logger.info("worker added");
                    }

                    logger.debug("Cluster's size: " + provisioningModule.getClusterSize());
                }
            } catch (OpennebulaException e) {
                logger.error("Could not adjust cluster's size", e);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        public Long getMinResponseTime() {
            return minResponseTime;
        }

        public void setMinResponseTime(Long minResponseTime) {
            this.minResponseTime = minResponseTime;
        }

        public Long getMaxResponseTime() {
            return maxResponseTime;
        }

        public void setMaxResponseTime(Long maxResponseTime) {
            this.maxResponseTime = maxResponseTime;
        }
    }
    
    private class RequestCountListener implements Observer {

        private int requestCountLimit;
        private double samples[];
        private int counter;

        public RequestCountListener(int windowSize, int requestCountLimit) {
            this.requestCountLimit = requestCountLimit;
            samples = new double[windowSize];
            counter = 0;
        }
        
        @Override
        public void update(Observable o, Object arg) {
            int requestCount = monitoringModule.getRequestCount();
            
            if (counter == samples.length) {
                for (int i = 0; i < samples.length - 1; i++) {
                    samples[i] = samples[i + 1];
                }
                
                samples[samples.length - 1] = requestCount;
                
                double prediction = predictionModule.predict(samples);
                int derivedClusterSize = (int)Math.ceil(prediction / (requestCountLimit * monitoringModule.getRequestCountMeasurement()));
                
                logger.info("Prediction: " + prediction + ". Cluster's size based on prediction: " + derivedClusterSize);
                
                try {
                    if (!provisioningModule.isDeploying()) {
                        if (derivedClusterSize < provisioningModule.getClusterSize()) {
                            logger.info("Removing worker based on prediction");
                            provisioningModule.deallocateWorker();
                            monitoringModule.clearAverage();
                            logger.debug("Cluster's size: " + provisioningModule.getClusterSize());
                        } else if (derivedClusterSize > provisioningModule.getClusterSize()) {
                            logger.info("Adding worker based on prediction");
                            provisioningModule.allocateWorker();
                            monitoringModule.clearAverage();
                            logger.debug("Cluster's size: " + provisioningModule.getClusterSize());
                        }
                    }
                } catch (OpennebulaException e) {
                    logger.error("Could not adjust the cluster's size from RequestCountListener.", e);
                }
                
            } else {
                samples[counter++] = requestCount;
            }
        }
        
    }
    
    public void shutdown() {
        logger.info("Shutting down Monitoring Module");
        if (monitoringModule != null) {
            monitoringModule.shutdown();
        }
        logger.info("Monitoring Module shutdown");
        
        logger.info("Shutting down Provisioning Module");
        if (provisioningModule != null) {
            provisioningModule.shutdown();
        }
        logger.info("Provisioning Module shutdown");
    }
    
    public Brain(String configurationPath) throws IOException{
        // Setting up log4j
        PropertyConfigurator.configure(configurationPath);
        
        logger.info("Starting the application");
        
        Properties properties = new Properties();
        properties.load(new FileInputStream(configurationPath));
        
        // Instantiating the Prediction Module
        logger.info("Starting the prediction module");
        
        String networkPath = properties.getProperty("model.neural_network_path");
        int windowSize = Integer.parseInt(properties.getProperty("model.window_size"));
        double dataMin = Double.parseDouble(properties.getProperty("model.data.min"));
        double dataMax = Double.parseDouble(properties.getProperty("model.data.max"));
        double normalisedMin = Double.parseDouble(properties.getProperty("model.data.normalised_min"));
        double normalisedMax = Double.parseDouble(properties.getProperty("model.data.normalised_max"));
        
        predictionModule = new Prediction(networkPath, windowSize, dataMin, dataMax, normalisedMin, normalisedMax);
        
        logger.info("Prediction module instantiated");
        
        // Instantiating the Provisioning module
        logger.info("Starting the Provisioning module");
        
        String username = properties.getProperty("opennebula.username");
        String password = properties.getProperty("opennebula.password");
        String apiUrl = properties.getProperty("opennebula.url");
        
        BufferedReader reader = new BufferedReader(new FileReader(properties.getProperty("opennebula.vm.template_path")));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        
        reader.close();
        
        List<Host> hosts = new ArrayList<Host>();
        
        String vmTemplate = stringBuilder.toString();
        
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("hosts.")) {
                int hostId = Integer.parseInt(key.split("\\.")[1]);
                double efficiency = Double.valueOf(properties.getProperty(key));
                
                Host host = new Host(hostId, efficiency);
                host.setAvailable(true);
                
                hosts.add(host);
            }
        }
        
        Strategy strategy = null;
        String strategyName = properties.getProperty("opennebula.vm.allocation_strategy");
        
        if (strategyName.equalsIgnoreCase("random")) {
            strategy = new RandomStrategy();
        } else if (strategyName.equalsIgnoreCase("energy")) {
            strategy = new EnergyEfficientStrategy();
        }
        
        provisioningModule = new Provisioning(strategy);
        provisioningModule.setVmTemplate(vmTemplate);
        provisioningModule.setHosts(hosts);
       
        // extracting how much memory the VM consumes from the template
        
        Pattern pattern = Pattern.compile("MEMORY=\"([0-9]+)\"");
        Matcher matcher = pattern.matcher(vmTemplate);
        
        if (matcher.find()) {
            int memoryUsed = Integer.parseInt(matcher.group(1)) * 1024;
            provisioningModule.setVmAllocatedMemory(memoryUsed);
        } else {
            logger.fatal("Could not determine VM's memory consumption");
            System.exit(1);
        }
        
        provisioningModule.setVmUsername(properties.getProperty("opennebula.vm.username"));
        provisioningModule.setVmPassword(properties.getProperty("opennebula.vm.password"));
        provisioningModule.setReservedMemory(Integer.parseInt(properties.getProperty("opennebula.vm.reserved_memory")));
        
        provisioningModule.setCollectdConfigurationPath(properties.getProperty("monitoring.collectd_configuration"));
        provisioningModule.setCollectdExecutable(properties.getProperty("monitoring.collectd_exec"));
        
        logger.info("Connecting to OpenNebula...");
        try {
            provisioningModule.connect(username, password, apiUrl);
        } catch (OpennebulaException e) {
            logger.fatal("Could not connect to Opennebula's API", e);
            System.exit(-1);
        }
        
        logger.info("Provisioning Module instantiated");
        
        // Initialising the Monitoring Module
        
        logger.info("Starting the Monitoring module");
        
        String monitoringUrl = "http://" + provisioningModule.getFrontIp() + properties.getProperty("monitoring.url");
        int responseTimeMeasurement = Integer.parseInt(properties.getProperty("monitoring.response_time_interval"));
        int movingAverageWindow = Integer.parseInt(properties.getProperty("monitoring.response_time_moving_average_window"));
        int requestCountMeasurement = Integer.parseInt(properties.getProperty("monitoring.request_count_interval"));
        
        monitoringModule = null;
        
        try {
            monitoringModule = new Monitoring(monitoringUrl, responseTimeMeasurement, movingAverageWindow, provisioningModule.getFrontIp(), 
                    properties.getProperty("opennebula.vm.username"), properties.getProperty("opennebula.vm.password"), requestCountMeasurement); 
        } catch (OpennebulaException e) {
            logger.fatal("Could not start monitoring module", e);
            System.exit(-1);
        }
        
        long minResponseTime = Long.parseLong(properties.getProperty("monitoring.min_response_time"));
        long maxResponseTime = Long.parseLong(properties.getProperty("monitoring.max_response_time"));
        long responseTimeout = Long.parseLong(properties.getProperty("monitoring.response_timeout"));
        
        responseTimeListener = new ResponseTimeListener();
        responseTimeListener.setMinResponseTime(minResponseTime);
        responseTimeListener.setMaxResponseTime(maxResponseTime);
        
        logger.info("Creating Response Time listener");
        monitoringModule.setResponseTimeListener(responseTimeListener);
        monitoringModule.setResponseTimeout(responseTimeout);
        
        logger.info("Creating Request Count listener");
        requestCountListener = new RequestCountListener(windowSize, Integer.parseInt(properties.getProperty("model.request_threshold")));
        monitoringModule.setRequestCountListener(requestCountListener);
        
        logger.info("Starting the monitoring");
        monitoringModule.start();
    }
    
    public void startLoop() {
        while (true) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
            }
        }
    }

    public static void main(String[] args) {
        Option helpOption = OptionBuilder.withDescription("Show help message")
                .create("help");
        
        Option configurationOption = OptionBuilder.withArgName("*.properties")
                .hasArgs()
                .isRequired()
                .withDescription("Path to the configuration file")
                .create("config");
        
        
        Options options = new Options();
        options.addOption(helpOption);
        options.addOption(configurationOption);
        
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        
        Brain app = null;
        try {
            cmd = parser.parse(options, args);
            
            if (cmd.hasOption("help")) {
                throw new ParseException(null);
            }
            
            app = new Brain(cmd.getOptionValue("config"));
            logger.info("AutoScaling started");
            app.startLoop();
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("CloudAutoScaling", options);
        } catch (IOException e) {
            logger.fatal("Could not read the configuration file", e);
        } finally {
            if (app != null) {
                app.shutdown();
            }
        }
        
        logger.info("Exiting...");
    }
}
