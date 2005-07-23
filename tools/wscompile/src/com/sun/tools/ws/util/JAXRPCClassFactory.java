/*
 * $Id: JAXRPCClassFactory.java,v 1.3 2005-07-23 04:11:01 kohlert Exp $
 */

/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tools.ws.util;

import java.util.Properties;

import com.sun.tools.ws.processor.config.WSDLModelInfo;
import com.sun.tools.ws.processor.generator.Names;
import com.sun.tools.ws.processor.modeler.wsdl.WSDLModeler20;
import com.sun.tools.ws.processor.modeler.wsdl.WSDLModelerBase;
import com.sun.tools.ws.wsdl.framework.AbstractDocument;
import com.sun.xml.ws.util.VersionUtil;

/**
 * Singleton factory class to instantiate concrete classes based on the jaxrpc version
 * to be used to generate the code.
 *
 * @author WS Development Team
 */
public class JAXRPCClassFactory {
    private static final JAXRPCClassFactory factory = new JAXRPCClassFactory();

    private static String classVersion = VersionUtil.JAXWS_VERSION_DEFAULT;

    private JAXRPCClassFactory() {
    }

    /**
     * Get the factory instance for the default version.
     * @return        JAXRPCClassFactory instance
     */
    public static JAXRPCClassFactory newInstance() {
        return factory;
    }

    /**
     * Sets the version to a static classVersion
     * @param version
     */
    public void setSourceVersion(String version) {
        if (version == null)
            version = VersionUtil.JAXWS_VERSION_DEFAULT;

        if (!VersionUtil.isValidVersion(version)) {
            // TODO: throw exception
        } else
            classVersion = version;
    }

    /**
     * Returns the WSDLModeler for specific target version.
     *
     * @param modelInfo
     * @param options
     * @return the WSDLModeler for specific target version.
     */
    public WSDLModelerBase createWSDLModeler(
        WSDLModelInfo modelInfo,
        Properties options) {
        WSDLModelerBase wsdlModeler = null;
        if (classVersion.equals(VersionUtil.JAXWS_VERSION_20))
            wsdlModeler =
                (WSDLModelerBase) new WSDLModeler20(modelInfo, options);
        else {
            // TODO: throw exception
        }
        return wsdlModeler;
    }

    /**
     * Returns the Names for specific target version.
     * //bug fix:4904604
     * @return instance of Names
     */
    public Names createNames() {
        Names names = new Names();
        return names;
    }

    public String getVersion() {
        return classVersion;
    }
}
