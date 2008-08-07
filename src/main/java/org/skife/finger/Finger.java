/*
 * Copyright 2008 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.finger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.integration.jmx.IoServiceManager;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements the finger server. Uses a "Fingerlet" to do actual processing, a la Servlets
 * <p/>
 * If used as main() will start a Ninglet
 */
public class Finger implements Runnable
{
    private final static Logger log = LoggerFactory.getLogger(Finger.class);

    private final int port;
    private final Fingerlet dir;
    private final AtomicInteger maxQueryWaitTime = new AtomicInteger(5000);
    private final AtomicInteger maxIdleReadTime = new AtomicInteger(1000);
    private final AtomicInteger threadCount = new AtomicInteger(25);

    public Finger(int port, Fingerlet dir)
    {
        this.port = port;
        this.dir = dir;
    }

    public void run()
    {
        SocketAcceptor sa = null;
        try {
            sa = new SocketAcceptor(Runtime.getRuntime().availableProcessors() + 1, Executors.newCachedThreadPool());
            sa.getDefaultConfig().setThreadModel(ThreadModel.MANUAL);
            ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
            ByteBuffer.setUseDirectBuffers(false);

            sa.getFilterChain().addLast("timeout", new TimeoutFilter(maxQueryWaitTime.get(), maxIdleReadTime.get()));
            sa.getFilterChain().addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("ASCII"))));
            if (log.isDebugEnabled()) {
                sa.getFilterChain().addLast("logging", new LoggingFilter());
            }
            // backend uses synchronous IO right now
            sa.getFilterChain().addLast("threads", new ExecutorFilter(Executors.newFixedThreadPool(threadCount.get())));

            sa.bind(new InetSocketAddress(port), new IoHandlerAdapter()
            {
                public void messageReceived(IoSession session, Object object) throws Exception
                {
                    String host = "";
                    String query = String.valueOf(object).trim();
                    final boolean verbose = query.startsWith("/W");
                    if (verbose) {
                        query = query.substring(3, query.length());
                    }

                    if (query.contains("@")) {
                        // proxied
                        String[] parts = query.split("\\@");
                        query = parts[0];
                        host = parts[1];
                    }

                    dir.lookup(new Request(query, host, verbose), new Response(session));
                }
            });
            IoServiceManager iosm = new IoServiceManager(sa);
            ObjectName name = new ObjectName("org.skife.finger:type=IoServiceManager,name=MXFingerD");
            ManagementFactory.getPlatformMBeanServer().registerMBean(iosm, name);
            Thread.currentThread().join();
        }
        catch (InterruptedException e) {
            log.info("Shutting down cleanly");
        }
        catch (Exception e) {
            log.warn("Unexpected exception, shutting down", e);
        }
        finally {
            if (sa != null) {
                sa.unbindAll();
            }
        }
    }

    public void setMaxQueryWaitTime(int maxQueryWaitTime)
    {
        this.maxQueryWaitTime.set(maxQueryWaitTime);
    }

    public void setMaxIdleReadTime(int maxIdleReadTime)
    {
        this.maxIdleReadTime.set(maxIdleReadTime);
    }

    public void setIoThreadCount(int threads)
    {
        threadCount.set(threads);
    }

    public static void main(String[] args) throws Exception
    {
        int port;
        try {
            ServerSocket sock = new ServerSocket(79);
            port = 79;
            sock.close();
            log.info("Binding to port 79");
        }
        catch (Exception e) {
            port = 7979;
            log.info("Binding to port 7979");
        }
        Finger t = new Finger(port, new JMXlet());
        final Thread main = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                main.interrupt();
            }
        });
        t.run();
    }
}
