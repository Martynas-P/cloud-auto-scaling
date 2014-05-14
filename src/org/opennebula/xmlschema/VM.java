package org.opennebula.xmlschema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author sc11mp
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "VM")
@XmlType(name = "", propOrder = {
    "id",
    "template"
})
public class VM {
    
    @XmlElement(name = "ID", required = true)
    protected Integer id;
    @XmlElement(name = "TEMPLATE", required = true)
    protected Template template;

    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public Template getTEMPLATE() {
        return template;
    }

    public void setTEMPLATE(Template template) {
        this.template = template;
    }
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "nic"
    })
    public static class Template {
        
        @XmlElement(name = "NIC", required = true)
        protected Nic nic;

        public Nic getNIC() {
            return nic;
        }

        public void setNIC(Nic nic) {
            this.nic = nic;
        }
        
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "ip"
    })
    public static class Nic {
    
        @XmlElement(name = "IP", required = true)
        protected String ip;

        public String getIP() {
            return ip;
        }

        public void setIP(String ip) {
            this.ip = ip;
        }
        
    }
    
}
