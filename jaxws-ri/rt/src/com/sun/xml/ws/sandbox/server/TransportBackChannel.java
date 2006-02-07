package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.message.Packet;

/**
 * Represents a transport back-channel.
 *
 * <p>
 * When the JAX-WS runtime finds out that the request
 * {@link Packet} being processed is known not to produce
 * a response, it invokes the {@link #close()} method
 * to indicate that the transport does not need to keep
 * the channel for the response message open.
 *
 * <p>
 * This allows the transport to close down the communication
 * channel sooner than wainting for
 * {@link WSEndpoint#process(Packet,WebServiceContextDelegate,TransportBackChannel)}
 * method to return, thereby improving the overall throughput
 * of the system.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitu
 */
public interface TransportBackChannel {
    /**
     * See the class javadoc for the discussion.
     *
     * <p>
     * JAX-WS is not guaranteed to call this method for all
     * operations that do not have a response. This is merely
     * a hint.
     *
     * <p>
     * When the implementation of this method fails to close
     * the connection successfuly, it should record the error,
     * and return normally. Do not throw any exception.
     */
    void close();
}
