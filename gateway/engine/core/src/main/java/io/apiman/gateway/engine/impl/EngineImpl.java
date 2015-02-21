/*
 * Copyright 2014 JBoss Inc
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
package io.apiman.gateway.engine.impl;

import io.apiman.gateway.engine.IComponentRegistry;
import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.IEngineResult;
import io.apiman.gateway.engine.IPluginRegistry;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.IServiceRequestExecutor;
import io.apiman.gateway.engine.Version;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.policy.IPolicyFactory;
import io.apiman.gateway.engine.policy.PolicyContextImpl;

/**
 * The implementation of the API Management runtime engine.
 *
 * @author eric.wittmann@redhat.com
 */
public class EngineImpl implements IEngine {

    private IRegistry registry;
    private IPluginRegistry pluginRegistry;
    private IComponentRegistry componentRegistry;
    private IConnectorFactory connectorFactory;
    private IPolicyFactory policyFactory;

    /**
     * Constructor.
     * @param registry
     * @param pluginRegistry
     * @param componentRegistry
     * @param connectorFactory
     * @param policyFactory
     */
    public EngineImpl(final IRegistry registry, final IPluginRegistry pluginRegistry,
            final IComponentRegistry componentRegistry, final IConnectorFactory connectorFactory,
            final IPolicyFactory policyFactory) {
        setRegistry(registry);
        setPluginRegistry(pluginRegistry);
        setComponentRegistry(componentRegistry);
        setConnectorFactory(connectorFactory);
        setPolicyFactory(policyFactory);
        
        policyFactory.setPluginRegistry(pluginRegistry);
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#getVersion()
     */
    @Override
    public String getVersion() {
        return Version.get().getVersionString();
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#request()
     */
    @Override
    public IServiceRequestExecutor executor(ServiceRequest request, final IAsyncResultHandler<IEngineResult> resultHandler) {
        return new ServiceRequestExecutorImpl(request, 
                resultHandler,
                registry,
                new PolicyContextImpl(getComponentRegistry()),
                policyFactory,
                getConnectorFactory());
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#getRegistry()
     */
    @Override
    public IRegistry getRegistry() {
        return registry;
    }

    /**
     * @param registry the registry to set
     */
    public void setRegistry(final IRegistry registry) {
        this.registry = registry;
    }

    /**
     * @return the connectorFactory
     */
    public IConnectorFactory getConnectorFactory() {
        return connectorFactory;
    }

    /**
     * @param connectorFactory the connectorFactory to set
     */
    public void setConnectorFactory(final IConnectorFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    /**
     * @return the policyFactory
     */
    public IPolicyFactory getPolicyFactory() {
        return policyFactory;
    }

    /**
     * @param policyFactory the policyFactory to set
     */
    public void setPolicyFactory(IPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    /**
     * @return the componentRegistry
     */
    public IComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    /**
     * @param componentRegistry the componentRegistry to set
     */
    public void setComponentRegistry(IComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    /**
     * @return the pluginRegistry
     */
    public IPluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    /**
     * @param pluginRegistry the pluginRegistry to set
     */
    public void setPluginRegistry(IPluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

}
