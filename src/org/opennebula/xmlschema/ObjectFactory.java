//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.03.07 at 10:48:43 PM GMT 
//


package org.opennebula.xmlschema;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.opennebula.xmlschema package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.opennebula.xmlschema
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link org.opennebula.xmlschema.HOST }
     * 
     */
    public org.opennebula.xmlschema.HOST createHOST() {
        return new org.opennebula.xmlschema.HOST();
    }

    /**
     * Create an instance of {@link HOSTPOOL }
     * 
     */
    public HOSTPOOL createHOSTPOOL() {
        return new HOSTPOOL();
    }

    /**
     * Create an instance of {@link HOSTPOOL.HOST }
     * 
     */
    public HOSTPOOL.HOST createHOSTPOOLHOST() {
        return new HOSTPOOL.HOST();
    }

    /**
     * Create an instance of {@link org.opennebula.xmlschema.HOST.HOSTSHARE }
     * 
     */
    public org.opennebula.xmlschema.HOST.HOSTSHARE createHOSTHOSTSHARE() {
        return new org.opennebula.xmlschema.HOST.HOSTSHARE();
    }

    /**
     * Create an instance of {@link HOSTPOOL.HOST.HOSTSHARE }
     * 
     */
    public HOSTPOOL.HOST.HOSTSHARE createHOSTPOOLHOSTHOSTSHARE() {
        return new HOSTPOOL.HOST.HOSTSHARE();
    }

}