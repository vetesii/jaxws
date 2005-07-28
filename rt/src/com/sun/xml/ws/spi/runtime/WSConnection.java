/**
 * $Id: WSConnection.java,v 1.5 2005-07-28 00:24:36 jitu Exp $
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.spi.runtime;

import com.sun.pept.transport.Connection;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


/**
 * Captures many transports that are used to talk with WS endpoints.
 * 
 * For endpoints deployed in light weight http server in J2SE, the implemenation
 * of this class uses HttpTransaction to read from or write to stream.
 *
 * For endpoints deployed in servlet container, the implementation of this
 * class uses HttpServletRequest to read a request, and uses HttpServletResponse
 * to write response.
 *
 * This also works for local transport, JMS transport.
 *
 * Runtime can access to the implementation of this interface using
 * messageInfo.getConnection()
 * 
 */

public interface WSConnection extends Connection {
    /**
     * It maps the enum constants to the following HTTP status codes.
     * OK=200, ONEWAY=202, UNSUPPORTED_MEDIA=415, MALFORMED_XML=400,
     * INTERNAL_ERR=500
     *
     */
    enum STATUS { OK, ONEWAY, UNSUPPORTED_MEDIA, MALFORMED_XML, 
                  INTERNAL_ERR, OTHER };
    
    /**
     * returns request headers. can we use javax.net.http.Headers ?
     */
    public Map<String,List<String>> getHeaders();
    
    /**
     * sets response headers. can we use javax.net.http.Headers ?
     */
    public void setHeaders(Map<String,List<String>> headers);
    public void setStatus(int status);
    public void setStatus(STATUS status);
    public int getStatus();
    
    public InputStream getInput();
    
    public OutputStream getOutput();
    
    public OutputStream getDebug();
    public void closeOutput();

}
