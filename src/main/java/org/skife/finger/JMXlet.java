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

import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Array;

/**
 *
 */
public class JMXlet implements Fingerlet
{
    private static final Logger log = Logger.getLogger(JMXlet.class);

    public void lookup(Request req, Response res)
    {
        JMXConnector c = null;
        try {
            String bean_name = req.getUserName();
            String port = "8989";
            String host = req.getProxyHost();
            if (host.contains(":")) {
                String[] parts = host.split(":");
                host = parts[0];
                port = parts[1];
            }

            String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", host, port);
            c = JMXConnectorFactory.connect(new JMXServiceURL(url));
            c.connect();
            MBeanServerConnection mbsc = c.getMBeanServerConnection();

            String[] parts = bean_name.split("#");
            bean_name = parts[0];
            if (req.isVerbose()) {
                res.sendLine(bean_name);
            }
            if (parts.length == 2) {
                // get single attribute
                String attribute = parts[1];
                Object attr = mbsc.getAttribute(new ObjectName(bean_name), attribute);

                res.send(attribute, String.valueOf(attr));
            }
            else {

                if (bean_name.length() == 0) {
                    // list beans
                    Set s_names = mbsc.queryNames(null, null);
                    List<String> names = new ArrayList<String>();
                    for (Object s_name : s_names) {
                        names.add(s_name.toString());
                    }

                    Collections.sort(names);
                    for (Object name : names) {
                        res.sendLine(name.toString());
                    }
                }

                else {
                    // list all attributes
                    MBeanInfo info = mbsc.getMBeanInfo(new ObjectName(bean_name));
                    for (MBeanAttributeInfo attr_info : info.getAttributes()) {
                        String attr_name = attr_info.getName();
                        Object attr;
                        try {
                            attr = mbsc.getAttribute(new ObjectName(bean_name), attr_name);
                        }
                        catch (Exception e) {
                            attr = e.getMessage();
                        }

                        res.send(attr_name, String.valueOf(attr));
                    }
                }
            }
        }
        catch (Exception e) {
            log.warn(e.getMessage(), e);
            res.sendLine(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                res.sendLine(element.toString());
            }
        }
        finally {
            res.finish();
            if (c != null) {
                try {
                    c.close();
                }
                catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }
}
