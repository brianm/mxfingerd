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

/**
 *
 */
public class Request
{
    private final String username;
    private final boolean verbose;
    private String proxyHost;

    public Request(String username, String proxyHost, boolean verbose)
    {
        this.username = username;
        this.proxyHost = proxyHost;
        this.verbose = verbose;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public String getUserName()
    {
        return username;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }
}
