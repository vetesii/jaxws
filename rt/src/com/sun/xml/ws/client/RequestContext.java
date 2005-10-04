/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.client;

import java.util.Iterator;

import javax.xml.ws.BindingProvider;


public class RequestContext extends ContextMap {

    public RequestContext(BindingProvider provider) {
        super(provider);
    }

    public RequestContext(PortInfoBase port, BindingProvider provider) {
        super(port, provider);
    }

    public RequestContext copy() {
        RequestContext _copy = new RequestContext(_owner);
        Iterator i = getPropertyNames();

        while (i.hasNext()) {
            String name = (String) i.next();
            Object value = this.get(name);
            _copy.put(name, value);
        }

        return _copy;
    }
}
