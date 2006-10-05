package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.stax.StreamWriterBufferCreator;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.developer.StatefulWebServiceManager;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.spi.ProviderImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link InstanceResolver} that looks at JAX-WS cookie header to
 * determine the instance to which a message will be routed to.
 * @author Kohsuke Kawaguchi
 */
public class StatefulInstanceResolver<T> extends AbstractInstanceResolver<T> implements StatefulWebServiceManager<T> {
    private final Class<T> clazz;

    /**
     * This instance is used for serving messages that have no cookie
     * or cookie value that the server doesn't recognize.
     */
    private volatile @Nullable T fallback;

    /**
     * Maps object ID to instances.
     */
    private final Map<String,T> instances = Collections.synchronizedMap(new HashMap<String,T>());
    /**
     * Reverse look up for {@link #instances}.
     */
    private final Map<T,String> reverseInstances = Collections.synchronizedMap(new HashMap<T,String>());

    // fields for resource injection.
    private /*almost final*/ InjectionPlan<T> injectionPlan;
    private /*almost final*/ WebServiceContext webServiceContext;
    private final Method postConstructMethod;
    private final Method preDestroyMethod;

    public StatefulInstanceResolver(Class<T> clazz) {
        this.clazz = clazz;

        postConstructMethod = findAnnotatedMethod(clazz, PostConstruct.class);
        preDestroyMethod = findAnnotatedMethod(clazz, PreDestroy.class);
    }

    /**
     * Perform resource injection on the given instance.
     */
    private void prepare(T t) {
        // we can only start creating new instances after the start method is invoked.
        assert webServiceContext!=null;

        injectionPlan.inject(t,webServiceContext);
        invokeMethod(postConstructMethod,t);
    }

    @Override
    public @NotNull T resolve(Packet request) {
        HeaderList headers = request.getMessage().getHeaders();
        Header header = headers.get(COOKIE_TAG, true);
        String id=null;
        if(header!=null) {
            // find the instance
            id = header.getStringContent();
            T o = instances.get(id);
            if(o!=null)
                return o;

            // huh? what is this ID?
            logger.log(Level.INFO,"Request had an unrecognized object ID "+id);
        }

        // need to fallback
        T fallback = this.fallback;
        if(fallback!=null)
            return fallback;

        if(id==null)
            throw new WebServiceException(ServerMessages.STATEFUL_COOKIE_HEADER_REQUIRED(COOKIE_TAG));
        else
            throw new WebServiceException(ServerMessages.STATEFUL_COOKIE_HEADER_INCORRECT(COOKIE_TAG,id));
    }

    @Override
    public void start(WebServiceContext wsc) {
        injectionPlan = buildInjectionPlan(clazz);
        this.webServiceContext = wsc;
    }

    @Override
    public void dispose() {
        synchronized(instances) {
            for (T t : instances.values()) {
                dispose(t);
            }
            instances.clear();
        }
        if(fallback!=null)
            dispose(fallback);
        fallback = null;
    }

    @NotNull
    public W3CEndpointReference export(T o) {
        return export(W3CEndpointReference.class,o);
    }

    @NotNull
    public <EPR extends EndpointReference> EPR export(Class<EPR> adrsVer, T o) {
        String key = reverseInstances.get(o);
        if(key!=null)   return createEPR(key,adrsVer);

        // not exported yet.
        synchronized(this) {
            // double check now in the synchronization block to
            // really make sure that we can export.
            key = reverseInstances.get(o);
            if(key!=null)   return createEPR(key,adrsVer);

            if(o!=null)
                prepare(o);
            key = UUID.randomUUID().toString();
            instances.put(key,o);
            reverseInstances.put(o,key);
        }

        return createEPR(key,adrsVer);
    }

    /**
     * Creates an EPR that has the right key.
     */
    private <EPR extends EndpointReference> EPR createEPR(String key, Class<EPR> eprClass) {
        AddressingVersion adrsVer = AddressingVersion.fromSpecClass(eprClass);

        try {
            StreamWriterBufferCreator w = new StreamWriterBufferCreator();

            w.writeStartDocument();
            w.writeStartElement("wsa","EndpointReference", adrsVer.nsUri);
            w.writeNamespace("wsa",adrsVer.nsUri);
            w.writeStartElement("wsa","Address",adrsVer.nsUri);
            w.writeCharacters("TODO"); // TODO
            w.writeEndElement();
            w.writeStartElement("wsa","ReferenceParameters",adrsVer.nsUri);
            w.writeStartElement(COOKIE_TAG.getPrefix(), COOKIE_TAG.getLocalPart(), COOKIE_TAG.getNamespaceURI());
            w.writeCharacters(key);
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndDocument();

            // TODO: this can be done better by writing SAX code that produces infoset
            // and setting that as Source.
            return eprClass.cast(ProviderImpl.INSTANCE.readEndpointReference(
                new XMLStreamBufferSource(w.getXMLStreamBuffer())));
        } catch (XMLStreamException e) {
            throw new Error(e); // this must be a bug in our code
        }
    }

    public void unexport(@Nullable T o) {
        if(o==null)     return;
        String key = reverseInstances.get(o);
        if(key==null)   return; // already unexported
        instances.remove(key);
    }

    public void setFallbackInstance(T o) {
        if(o!=null)
            prepare(o);
        this.fallback = o;
    }

    private void dispose(T instance) {
        invokeMethod(preDestroyMethod,instance);
    }

    private static final QName COOKIE_TAG = new QName("http://jax-ws.dev.java.net/xml/ns/","objectId","jaxws");

    private static final Logger logger =
        Logger.getLogger(com.sun.xml.ws.util.Constants.LoggingDomain + ".server");
}
