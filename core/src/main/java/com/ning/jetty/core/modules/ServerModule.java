/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.jetty.core.modules;

import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;
import com.ning.jersey.metrics.TimedResourceModule;
import com.ning.jetty.core.CoreConfig;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.guice.InstrumentationModule;
import com.yammer.metrics.reporting.guice.MetricsServletModule;
import org.codehaus.jackson.jaxrs.Annotations;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.MBeanModule;

import java.util.Properties;

public class ServerModule extends ServletModule
{
    private final Properties props;

    protected Multibinder<HealthCheck> healthChecksBinder;

    public ServerModule()
    {
        this(System.getProperties());
    }

    public ServerModule(final Properties props)
    {
        this.props = props;
    }

    @Override
    public void configureServlets()
    {
        configureConfig();

        installJackson();
        installJMX();
        installStats();
    }

    protected void configureConfig()
    {
        final CoreConfig coreConfig = new ConfigurationObjectFactory(props).build(CoreConfig.class);
        bind(CoreConfig.class).toInstance(coreConfig);
    }

    protected void installJackson()
    {
        final ObjectMapper mapper = getJacksonProvider().get();

        bind(ObjectMapper.class).toInstance(mapper);
        bind(JacksonJsonProvider.class)
            .toInstance(new JacksonJsonProvider(mapper, new Annotations[]{Annotations.JACKSON, Annotations.JAXB}));
    }

    /**
     * Override this method to provide your own Jackson provider.
     *
     * @return ObjectMapper provider for Jackson
     */
    protected Provider<ObjectMapper> getJacksonProvider()
    {
        return new Provider<ObjectMapper>()
        {
            @Override
            public ObjectMapper get()
            {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.disable(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS);
                return mapper;
            }
        };
    }

    protected void installJMX()
    {
        install(new MBeanModule());
    }

    protected void installStats()
    {
        // Codahale's metrics
        install(new InstrumentationModule());

        // Healthchecks
        healthChecksBinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
        install(new MetricsServletModule("/1.0/healthcheck", "/1.0/metrics", "/1.0/ping", "/1.0/threads"));

        // Metrics/Jersey integration
        install(new TimedResourceModule());
    }
}
