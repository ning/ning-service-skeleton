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

import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;
import com.ning.jersey.metrics.TimedResourceModule;
import com.ning.jetty.core.CoreConfig;
import com.ning.jetty.core.DaoConfig;
import com.ning.jetty.core.healthchecks.DBIHealthCheck;
import com.ning.jetty.core.log4j.Log4JMBean;
import com.ning.jetty.core.providers.DBIProvider;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.guice.InstrumentationModule;
import com.yammer.metrics.reporting.guice.MetricsServletModule;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.Properties;

public class ServerModule extends ServletModule
{
    private final Properties props;

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
        final ExportBuilder builder = MBeanModule.newExporter(binder());

        configureConfig();

        installJackson();
        installJMX();
        installLog4jLogging(builder);
        installStats();
        installDBI();
        installHealthChecks();
    }

    protected void configureConfig()
    {
        final CoreConfig config = new ConfigurationObjectFactory(props).build(CoreConfig.class);
        bind(CoreConfig.class).toInstance(config);

        final DaoConfig daoConfig = new ConfigurationObjectFactory(props).build(DaoConfig.class);
        bind(DaoConfig.class).toInstance(daoConfig);
    }

    protected void installJackson()
    {
        bind(JacksonJsonProvider.class).asEagerSingleton();
    }

    protected void installJMX()
    {
        install(new MBeanModule());
    }

    public void installLog4jLogging(final ExportBuilder builder)
    {
        bind(Log4JMBean.class).asEagerSingleton();
        builder.export(Log4JMBean.class).as("com.ning.jetty.core:name=Log4JMBean");
    }

    protected void installStats()
    {
        // Codahale's metrics
        install(new InstrumentationModule());

        // Healthchecks
        install(new MetricsServletModule("/1.0/healthcheck", "/1.0/metrics", "/1.0/ping", "/1.0/threads"));

        // Metrics/Jersey integration
        install(new TimedResourceModule());
    }

    protected void installDBI()
    {
        binder().bind(DBI.class).toProvider(DBIProvider.class).asEagerSingleton();
    }

    protected void installHealthChecks()
    {
        final Multibinder<HealthCheck> healthChecksBinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
        healthChecksBinder.addBinding().to(DBIHealthCheck.class).asEagerSingleton();
    }
}