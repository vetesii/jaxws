/*
 * $Id: MessageDispatcherHelper.java,v 1.1 2005-05-23 22:13:27 bbissett Exp $
 */

/*
* Copyright (c) 2004 Sun Microsystems, Inc.
* All rights reserved.
*/
package com.sun.xml.ws.client.dispatch.impl.protocol;

import com.sun.pept.ept.MessageInfo;
import com.sun.pept.presentation.MessageStruct;
import com.sun.pept.protocol.MessageDispatcher;
import com.sun.xml.ws.client.BindingImpl;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.client.ContextMap;
import com.sun.xml.ws.handler.HandlerChainCaller;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.util.logging.Logger;

/**
 * @author JAX-RPC RI Development Team
 */
public class MessageDispatcherHelper extends com.sun.xml.ws.client.SOAPMessageDispatcher
    implements MessageDispatcher, BindingProviderProperties {

    private static final Logger logger =
        Logger.getLogger(new StringBuffer().append(com.sun.xml.ws.util.Constants.LoggingDomain).append(".client.dispatch").toString());

    public MessageDispatcherHelper() {
        super();
    }

    protected void setResponseType(Throwable e, MessageInfo messageInfo) {
        if (e instanceof RuntimeException) {
            //leave for now- fix later
            if (e instanceof WebServiceException)
                messageInfo.setResponseType(MessageStruct.CHECKED_EXCEPTION_RESPONSE);
            else
                messageInfo.setResponseType(MessageStruct.UNCHECKED_EXCEPTION_RESPONSE);
        } else {
            messageInfo.setResponseType(MessageStruct.CHECKED_EXCEPTION_RESPONSE);
        }
    }

    protected HandlerChainCaller getHandlerChainCaller(MessageInfo messageInfo) {
        ContextMap context = (ContextMap)
            messageInfo.getMetaData(BindingProviderProperties.JAXRPC_CONTEXT_PROPERTY);
        BindingProvider provider = (BindingProvider)
            context.get(BindingProviderProperties.JAXRPC_CLIENT_HANDLE_PROPERTY);
        BindingImpl binding = (BindingImpl) provider.getBinding();
        return binding.getHandlerChainCaller();
    }

}

