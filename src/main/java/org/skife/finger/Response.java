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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFutureListener;

/**
 *
 */
public class Response
{
    private final IoSession session;
    private final StringBuffer b = new StringBuffer();

    public Response(IoSession session)
    {
        this.session = session;
    }

    public void send(String key, String value)
    {
        sendLine(new StringBuilder(key).append(": ").append(value).toString());
    }

    public void sendLine(String line)
    {
        b.append(line).append("\r\n");
    }

    public void finish()
    {
        session.write(b.substring(0, b.length() - 2)).addListener(IoFutureListener.CLOSE);
    }
}
