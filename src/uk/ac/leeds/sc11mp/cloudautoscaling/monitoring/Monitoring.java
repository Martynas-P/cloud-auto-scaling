package uk.ac.leeds.sc11mp.cloudautoscaling.monitoring;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Observer;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import uk.ac.leeds.sc11mp.cloudautoscaling.exception.OpennebulaException;
import uk.ac.leeds.sc11mp.cloudautoscaling.monitoring.event.RequestCountEvent;
import uk.ac.leeds.sc11mp.cloudautoscaling.monitoring.event.ResponseTimeEvent;

/**
 *
 * @author sc11mp
 */
public class Monitoring {
    private ScheduledExecutorService taskScheduler;
    private MonitorResponseTime monitorResponeTime;
    private MonitorRequestCount monitorRequestCount;
    private final ResponseTimeEvent responseTimeEvent;
    private final RequestCountEvent requestCountEvent;
    private int responseTimeMeasurement;
    private int requestCountMeasurement;
    private ScheduledFuture<?> responseTimeFuture;

    private static Logger logger = Logger.getLogger(Monitoring.class);
    
    private class MonitorResponseTime implements Runnable {   
        private String monitorUrl;
        private int movingAverageWindow;
        private Long responseTime = null;
        
        private long timeout;
        private Queue<Long> responseTimes;
        private Long lastRun;
        
        public MonitorResponseTime(String url, int movingAverageWindow) {
            this.monitorUrl = url;
            this.movingAverageWindow = movingAverageWindow;
            
            responseTimes = new ArrayBlockingQueue<Long>(movingAverageWindow);
            lastRun = null;
        }
        
        @Override
        public void run() {
            Long lastRun = System.currentTimeMillis() / 1000;
            if (this.lastRun == null) {
                lastRun = System.currentTimeMillis() / 1000;
            } else if (System.currentTimeMillis() / 1000 - this.lastRun < responseTimeMeasurement) {
                logger.info("Skipping response time measurement; queued up");
                return;
            }
            
            try {
                long startTime = System.currentTimeMillis();
            
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                URL url = new URL(monitorUrl);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setReadTimeout((int)timeout);
                
                long delta;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    while (reader.readLine() != null) {
                    }

                    reader.close();

                    delta = System.currentTimeMillis() - startTime;
                } catch (SocketTimeoutException e) {
                    delta = timeout;
                } catch (ConnectException e) {
                    delta = timeout;
                }
                
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                logger.debug("Request to " + monitorUrl + " took " + delta + "ms");
                
                if (responseTimes.size() == movingAverageWindow) {
                    responseTimes.remove();
                }
                
                responseTimes.add(delta);
                
                if (responseTimes.size() == movingAverageWindow) {
                    
                    long sum = 0;
                    
                    for (long i : responseTimes) {
                        sum += i;
                    }
                    
                    responseTime = Math.round((double) sum / movingAverageWindow);
                    logger.debug("Emitting ResponseTimeEvent. Response time average: " + responseTime);
                    this.lastRun = lastRun;
                    responseTimeEvent.emitEvent();
                }
                
            } catch (MalformedURLException e) {
                logger.error("Malformed URL", e);
            } catch (IOException e) {
                logger.error("Could not send HTTP request", e);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        public Long getResponseTime() {
            return responseTime;
        }
    }
    
    private class MonitorRequestCount implements Runnable {

        private Session sshSession;
        private Integer requestCount = null;

        public MonitorRequestCount(String ip, String username, String password) throws OpennebulaException {
            try {
                JSch jsch = new JSch();
                
                Properties properties = new Properties();
                properties.put("StrictHostKeyChecking", "no");
                
                sshSession = jsch.getSession(username, ip, 22);
                sshSession.setConfig(properties);
                sshSession.setPassword(password);
                sshSession.connect();
            } catch (JSchException ex) {
                throw new OpennebulaException("Could not connect to VM", ex);
            }
        }
        
        @Override
        public void run() {
            try {
                // Getting the request count.
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                channel.setCommand("cat /var/log/apache2/access.log | sed '/^\\s*$/d' | wc -l");
                channel.connect();
                
                String response = closeChannel(channel).trim();
                
                if (!response.isEmpty()) {
                    requestCount = Integer.parseInt(response.trim());

                    // Erasing the log
                    channel = (ChannelExec) sshSession.openChannel("exec");
                    channel.setCommand("echo '' > /var/log/apache2/access.log");
                    channel.connect();
                    closeChannel(channel);

                    requestCountEvent.emitEvent();

                    logger.debug("Requests counted: " + requestCount);
                } else {
                    logger.debug("No requests counted");
                }
            } catch (JSchException e) {
                logger.error("Could not connect via SSH to get request count", e);
            } catch (IOException e) {
                logger.error("Could not close SSH channel", e);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        private String closeChannel(ChannelExec channel) throws IOException {
            String response = "";
            InputStream in = channel.getInputStream();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    
                    response += new String(tmp, 0, i);
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            
            return response;
        }

        public Integer getResponseCount() {
            return requestCount;
        }
    }
    
    public Monitoring(String url, int responseTimeMeasurement, int movingAverageWindow, String ip, String username, String password, int requestCountMeasurement) 
            throws OpennebulaException {
        
        this.responseTimeMeasurement = responseTimeMeasurement; 
        this.requestCountMeasurement = requestCountMeasurement;
        
        responseTimeEvent = new ResponseTimeEvent();
        requestCountEvent = new RequestCountEvent();
        taskScheduler = new ScheduledThreadPoolExecutor(23);
        
        monitorResponeTime = new MonitorResponseTime(url, movingAverageWindow);     
        monitorRequestCount = new MonitorRequestCount(ip, username, password);
    }
    
    public void setResponseTimeout(long timeout) {
        this.monitorResponeTime.timeout = timeout;
    }
    
    public void clearAverage() {
        monitorResponeTime.responseTimes.clear();
    }
    
    public void start() {
        responseTimeFuture = taskScheduler.scheduleAtFixedRate(monitorResponeTime, responseTimeMeasurement, responseTimeMeasurement, TimeUnit.SECONDS);
        taskScheduler.scheduleAtFixedRate(monitorRequestCount, requestCountMeasurement, requestCountMeasurement, TimeUnit.SECONDS);
        
    }
    
    public void shutdown() {
        taskScheduler.shutdownNow();
    }
    
    public void setResponseTimeListener(Observer listener) {
        responseTimeEvent.deleteObservers();
        responseTimeEvent.addObserver(listener);
    }
    
    public void setRequestCountListener(Observer listener) {
        requestCountEvent.deleteObservers();
        requestCountEvent.addObserver(listener);
    }
    
    public Long getResponseTime() {
        return this.monitorResponeTime.getResponseTime();
    }
    
    public Integer getRequestCount() {
        return this.monitorRequestCount.getResponseCount();
    }

    public int getResponseTimeMeasurement() {
        return responseTimeMeasurement;
    }
    
    public void stopMonitoringResponseTime() {
        responseTimeFuture.cancel(true);
    }
    
    public void restartRespnseMonitoringTime() {
        responseTimeFuture = taskScheduler.scheduleAtFixedRate(monitorResponeTime, 2 * responseTimeMeasurement, responseTimeMeasurement, TimeUnit.SECONDS);
    }

    public int getRequestCountMeasurement() {
        return requestCountMeasurement;
    }
}
