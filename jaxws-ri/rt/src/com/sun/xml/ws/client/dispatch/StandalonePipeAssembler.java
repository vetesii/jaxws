/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.sandbox.handler.ClientHandlerPipe;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.sandbox.impl.StreamSOAPDecoder;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

public class StandalonePipeAssembler implements PipelineAssembler {
    public Pipe createClient(WSDLPort wsdlModel, WSService service, WSBinding binding) {
        Pipe head = createTransport(wsdlModel,service,binding);
        if(!binding.getHandlerChain().isEmpty()) {
            Pipe handlerPipe = new ClientHandlerPipe(binding, head);
            head = handlerPipe;
        }
        return head;
    }

    protected Pipe createTransport(WSDLPort wsdlModel, WSService service, WSBinding binding) {
        Pipe p = new HttpTransportPipe(
            TestEncoderImpl.get(binding.getSOAPVersion()),
            // TestDecoderImpl.get(binding.getSOAPVersion()));
            StreamSOAPDecoder.create(binding.getSOAPVersion()));
        return p;
    }

    public Pipe createServer(WSDLPort wsdlModel, WSEndpoint endpoint, Pipe terminal) {
        return terminal;
    }
}
