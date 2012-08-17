/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.api.addressing;

import com.sun.istack.NotNull;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.TransportTubeFactory;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.BindingImpl;

/**
 * Delivers response messages targeted at non-anonymous endpoint addresses
 * @since 2.2.6
 */
public class NonAnonymousResponseProcessor {
	private static final NonAnonymousResponseProcessor DEFAULT = new NonAnonymousResponseProcessor();
	
	public static NonAnonymousResponseProcessor getDefault() {
		return DEFAULT;
	}
	
	protected NonAnonymousResponseProcessor() {}
	
    /**
     * Send a response to a non-anonymous address. Also closes the transport back channel
     * of {@link Packet} if it's not closed already.
     *
     * @param packet
     *      The response from our server, which will be delivered to the destination.
     * @return The response packet that should be used to complete the tubeline response processing
     */
	public Packet process(Packet packet) {
        Fiber.CompletionCallback fiberCallback = null;
        Fiber currentFiber = Fiber.getCurrentIfSet();
        if (currentFiber != null) {
            // Link completion of the current fiber to the new fiber that will
            // deliver the async response. This allows access to the response
            // packet that may be generated by sending a new message for the
            // current async response.

	        final Fiber.CompletionCallback currentFiberCallback =
	            currentFiber.getCompletionCallback();
	        
			if (currentFiberCallback != null) {
		          fiberCallback = new Fiber.CompletionCallback() {
		          public void onCompletion(@NotNull Packet response) {
		            currentFiberCallback.onCompletion(response);
		          }
		
		          public void onCompletion(@NotNull Throwable error) {
		            currentFiberCallback.onCompletion(error);
		          }
		        };
		        currentFiber.setCompletionCallback(null);
	        }
        }

        // we need to assemble a pipeline to talk to this endpoint.
		WSEndpoint<?> endpoint = packet.endpoint;
		WSBinding binding = endpoint.getBinding();
        Tube transport = TransportTubeFactory.create(Thread.currentThread().getContextClassLoader(),
            new ClientTubeAssemblerContext(
            		packet.endpointAddress, endpoint.getPort(), (WSService) null, 
            		binding, endpoint.getContainer(),
            		((BindingImpl) binding).createCodec(), null, null));
        Fiber fiber = endpoint.getEngine().createFiber();
        fiber.start(transport, packet, fiberCallback);
        
        // then we'll proceed the rest like one-way.
        Packet copy = packet.copy(false);
        copy.endpointAddress = null;
        
        return copy;
	}
}