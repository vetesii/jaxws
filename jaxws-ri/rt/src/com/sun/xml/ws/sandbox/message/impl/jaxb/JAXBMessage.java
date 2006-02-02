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
package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.marshaller.SAX2DOMEx;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.RootElementSniffer;
import com.sun.xml.ws.util.exception.XMLStreamException2;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBResult;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

/**
 * {@link Message} backed by a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JAXBMessage extends AbstractMessageImpl {
    private HeaderList headers;

    /**
     * The JAXB object that represents the header.
     */
    private final Object jaxbObject;

    private final Bridge bridge;
    private final BridgeContext context;

    /**
     * Lazily sniffed payload element name
     */
    private String nsUri,localName;

    /**
     * If we have the infoset representation for the payload, this field is non-null.
     */
    private XMLStreamBuffer infoset;

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param marshaller
     *      The marshaller to be used to produce infoset from the object. Must not be null.
     * @param jaxbObject
     *      The JAXB object that represents the payload. must not be null. This object
     *      must be bound to an element (which means it either is a {@link JAXBElement} or
     *      an instanceof a class with {@link XmlRootElement}).
     * @param soapVer
     *      The SOAP version of the message. Must not be null.
     */
    public JAXBMessage( Marshaller marshaller, Object jaxbObject, SOAPVersion soapVer ) {
        super(soapVer);
        this.bridge = MarshallerBridgeContext.MARSHALLER_BRIDGE;
        this.context = new MarshallerBridgeContext(marshaller);
        this.jaxbObject = jaxbObject;
    }

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param marshaller
     *      The marshaller to be used to produce infoset from the object. Must not be null.
     * @param tagName
     *      The tag name of the payload element. Must not be null.
     * @param declaredType
     *      The expected type (IOW the declared defined in the schema) of the instance.
     *      If this is different from <tt>jaxbTypeObject</tt>, @xsi:type will be produced.
     * @param jaxbTypeObject
     *      The JAXB object that represents the payload. must not be null. This object
     *      may be a type that binds to an XML type, not an element, such as {@link String}
     *      or {@link Integer} that doesn't necessarily have an element name.
     * @param soapVer
     *      The SOAP version of the message. Must not be null.
     */
    public <T> JAXBMessage( Marshaller marshaller, QName tagName, Class<T> declaredType, T jaxbTypeObject, SOAPVersion soapVer ) {
        this(marshaller,new JAXBElement<T>(tagName,declaredType,jaxbTypeObject),soapVer);
        // fill in those known values eagerly
        this.nsUri = tagName.getNamespaceURI();
        this.localName = tagName.getLocalPart();
    }

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param bridge
     *      Specify the payload tag name and how <tt>jaxbObject</tt> is bound.
     * @param jaxbObject
     *      The object to be bound.
     * @param context
     *      The {@link BridgeContext} used for marshalling.
     */
    public JAXBMessage( Bridge bridge, Object jaxbObject, BridgeContext context, SOAPVersion soapVer ) {
        super(soapVer);
        // TODO: think about a better way to handle BridgeContext
        this.bridge = bridge;
        this.jaxbObject = jaxbObject;
        this.context = context;
        QName tagName = bridge.getTypeReference().tagName;
        this.nsUri = tagName.getNamespaceURI();
        this.localName = tagName.getLocalPart();
    }

    /**
     * Copy constructor.
     */
    public JAXBMessage(JAXBMessage that) {
        super(that);
        this.headers = that.headers;
        if(this.headers!=null)
            this.headers = new HeaderList(this.headers);

        this.jaxbObject = that.jaxbObject;
        this.bridge = that.bridge;
        // TODO: we need a different context
        this.context = that.context;
    }

    public boolean hasHeaders() {
        return headers!=null && !headers.isEmpty();
    }

    public HeaderList getHeaders() {
        if(headers==null)
            headers = new HeaderList();
        return headers;
    }

    public String getPayloadLocalPart() {
        if(localName==null)
            sniff();
        return localName;
    }

    public String getPayloadNamespaceURI() {
        if(nsUri==null)
            sniff();
        return nsUri;
    }

    public boolean hasPayload() {
        return true;
    }

    /**
     * Obtains the tag name of the root element.
     */
    private void sniff() {
        RootElementSniffer sniffer = new RootElementSniffer(false);
        try {
            bridge.marshal(context,jaxbObject,sniffer);
        } catch (JAXBException e) {
            // if it's due to us aborting the processing after the first element,
            // we can safely ignore this exception.
            //
            // if it's due to error in the object, the same error will be reported
            // when the readHeader() method is used, so we don't have to report
            // an error right now.
            nsUri = sniffer.getNsUri();
            localName = sniffer.getLocalName();
        }
    }

    public Source readPayloadAsSource() {
        return new JAXBBridgeSource(bridge,context,jaxbObject);
    }

    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        JAXBResult out = new JAXBResult(unmarshaller);
        bridge.marshal(context,jaxbObject,out);
        return (T)out.getResult();
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        try {
            if(infoset==null) {
                infoset = new XMLStreamBuffer();
                XMLStreamBufferResult sbr = new XMLStreamBufferResult(infoset);
                bridge.marshal(context,jaxbObject,sbr);
            }
            return infoset.processUsingXMLStreamReader();
        } catch (JAXBException e) {
            throw new XMLStreamException2(e);
        }
    }

    /**
     * Writes the payload as SAX events.
     */
    protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        try {
            // TODO: wrap this into a fragment filter
            bridge.marshal(context,jaxbObject,contentHandler);
        } catch (JAXBException e) {
            errorHandler.fatalError(new SAXParseException(e.getMessage(),NULL_LOCATOR,e));
        }
    }

    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        SOAPMessage msg = soapVersion.saajFactory.createMessage();
        SAX2DOMEx s2d = new SAX2DOMEx(msg.getSOAPPart());
        try {
            writeTo(s2d, XmlUtil.DRACONIAN_ERROR_HANDLER);
        } catch (SAXException e) {
            throw new SOAPException(e);
        }
        // TODO: add attachments and so on.
        // we can use helper classes, I think.
        return msg;
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        try {
            // TODO: XOP handling
            bridge.marshal(context,jaxbObject,sw);
        } catch (JAXBException e) {
            throw new XMLStreamException2(e);
        }
    }

    public Message copy() {
        return new JAXBMessage(this);
    }
}
