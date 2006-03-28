/**
 * <h1>JAX-WS 2.0 Handler Runtime</h1>
 * <p>This document describes the architecture of the handler code
 * in the JAX-WS 2.0 runtime.
 * TODO: Need to modify with re-arch
 * <p>Handlers may be specified by the deployment descriptor on the
 * server side, or by a wsdl customization or Java annotation. In the
 * case of a wsdl customization, wsimport will create the Java interface
 * with the handler chain annotation and will create a handler xml file
 * to which the annotation points. At runtime, thus, only deployment
 * descriptors and handler files pointed to by annotations are parsed.
 * The schema in all cases is the same, and so the actual parsing of
 * any file is done by
 * {@link com.sun.xml.ws.util.HandlerAnnotationProcessor}.
 *
 * <h3>Server side handler creation</h3>
 *
 * <p>The deployment descriptor is first parsed, and
 * {@link com.sun.xml.ws.transport.http.DeploymentDescriptorParser#setHandlersAndRoles}
 * parses the handler chains xml in the deployment descriptor if present.
 * It then sets the handlers and roles on the Binding object that has
 * already been created. Setting the handler chain on the binding does
 * not automatically create a handler chain caller.
 *
 * <p>Later, when
 * {@link com.sun.xml.ws.server.RuntimeEndpointInfo#init} is parsing the
 * annotations it checks for handlers on the binding. If there are handlers
 * already, it skips any further handler processing. In this way, the deployment
 * descriptor overrides any other handlers. If there are no handlers on
 * the binding at this point, RuntimeEndpointInfo has the
 * HandlerAnnotationProcessor parse the handler chain file and then
 * sets the handlers and roles on the binding.
 *
 * <h3>Client side handler creation</h3>
 *
 * <p>On the client side, the handler annotation file is parsed by
 * {@link com.sun.xml.ws.client.ServiceContextBuilder#processAnnotations}.
 * If there are any handlers, the ServiceContextBuilder gets the 
 * {@link com.sun.xml.ws.handler.HandlerResolverImpl} from
 * {@link com.sun.xml.ws.client.ServiceContext} and adds the handler
 * chain to the handler resolver. It also sets the roles on the service
 * context.
 *
 * <p>Unlike the server side, there is no binding object already
 * created when the handlers are parsed. When a binding provider is created, the
 * {@link com.sun.xml.ws.client.WSServiceDelegate} will use the handler
 * registry (which may be the HandlerResolverImpl or another handler
 * resolver set on the service by the user code) to get the handlers to
 * set on the binding. It will get the roles from the service context and
 * set those on the binding if it is a soap binding. The relevant method is
 * {@link com.sun.xml.ws.client.WSServiceDelegate#setBindingOnProvider}
 *
 * <h3>Calling the handlers</h3>
 *
 * <p>During a request or response, a
 * {@link com.sun.xml.ws.handler.HandlerChainCaller} is
 * created by the binding or may be created by a message dispatcher on the
 * server side (this happens in the http binding right now). In the binding
 * objects, the handler caller is not created until needed, and it sets
 * the handlers and roles (if present) on the handler chain caller then.
 * See {@link com.sun.xml.ws.binding.BindingImpl#getHandlerChainCaller}
 * and {@link com.sun.xml.ws.binding.SOAPBindingImpl#getHandlerChainCaller}
 * for more details.
 * 
 * <p>The handler chain caller does the handler invocation and controls the
 * flow of the handlers. For details of the code that calls the handler
 * chain caller, see
 * {@link com.sun.xml.ws.protocol.soap.client.SOAPMessageDispatcher}
 * on the client side and 
 * {@link com.sun.xml.ws.protocol.soap.server.SOAPMessageDispatcher} and
 * {@link com.sun.xml.ws.protocol.xml.server.XMLMessageDispatcher} on the
 * server side.
 *
 * TODO: this needs to be rewritten to reflect the new architecture.
 *
 * @ArchitectureDocument
 */
package com.sun.xml.ws.handler;
