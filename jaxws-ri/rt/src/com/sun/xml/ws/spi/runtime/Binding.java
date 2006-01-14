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

package com.sun.xml.ws.spi.runtime;

import com.sun.xml.ws.api.pipe.Pipe;

/**
 * The <code>Binding</code> is the spi interface that extends the base interface
 * for JAX-WS protocol bindings.
 * This interface is implemented by com.sun.xml.ws.client.Binding.
 *
 * @deprecated
 *      we'll be converting {@link SystemHandlerDelegate} to {@link Pipe}.
 **/
public interface Binding extends javax.xml.ws.Binding {

  public SystemHandlerDelegate getSystemHandlerDelegate();

  public void setSystemHandlerDelegate(SystemHandlerDelegate delegate);
}
