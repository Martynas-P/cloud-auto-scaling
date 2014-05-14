package uk.ac.leeds.sc11mp.cloudautoscaling.provisioning;

/**
 *
 * @author sc11mp
 */
public class Host {
    
    protected int hostId;
    protected double energyConsumptionCoefficient;
    protected boolean available;

    public Host(int hostId, double energyConsumptionCoefficient) {
        this.hostId = hostId;
        this.energyConsumptionCoefficient = energyConsumptionCoefficient;
    }

    public int getHostId() {
        return hostId;
    }

    public double getEnergyConsumptionCoefficient() {
        return energyConsumptionCoefficient;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
    
}
