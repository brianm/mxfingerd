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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class TimeoutFilter extends IoFilterAdapter
{
    private final static ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private final int maxTime;
    private final int maxIdle;
    private final AtomicBoolean queryReceived = new AtomicBoolean(false);

    public TimeoutFilter(int maxTime, int maxIdle)
    {

        this.maxTime = maxTime;
        this.maxIdle = maxIdle;
    }


    public void sessionCreated(NextFilter nextFilter, final IoSession session) throws Exception
    {
        session.setIdleTime(IdleStatus.READER_IDLE, maxIdle);
        TIMER.schedule(new Callable<Void>()
        {
            public Void call() throws Exception
            {
                if (!queryReceived.get() &&  session.isConnected()) {
                    session.write("Connection closed due to timeout");
                    session.close();
                }
                return null;
            }
        }, maxTime, TimeUnit.MILLISECONDS);
        nextFilter.sessionCreated(session);
    }

    public void messageReceived(NextFilter nextFilter, IoSession ioSession, Object object) throws Exception
    {
        queryReceived.set(true);
        nextFilter.messageReceived(ioSession, object);
    }

    public void sessionIdle(IoSession session, IdleStatus idleStatus) throws Exception
    {
        if (!queryReceived.get()) {
            session.write("Connection closed due to idle timeout");
            session.close();
        }
    }

}
