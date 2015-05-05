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
package io.apiman.gateway.platforms.war;

import io.apiman.common.config.ConfigFileConfiguration;
import io.apiman.common.config.SystemPropertiesConfiguration;
import io.apiman.gateway.engine.IComponent;
import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IEngineConfig;
import io.apiman.gateway.engine.IMetrics;
import io.apiman.gateway.engine.IPluginRegistry;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.policy.IPolicyFactory;
import io.apiman.gateway.platforms.war.i18n.Messages;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * Global access to configuration information.
 *
 * @author eric.wittmann@redhat.com
 */
public class WarEngineConfig implements IEngineConfig {

    public static final String APIMAN_GATEWAY_REGISTRY_CLASS = "apiman-gateway.registry"; //$NON-NLS-1$
    public static final String APIMAN_GATEWAY_PLUGIN_REGISTRY_CLASS = "apiman-gateway.plugin-registry"; //$NON-NLS-1$
    public static final String APIMAN_GATEWAY_CONNECTOR_FACTORY_CLASS = "apiman-gateway.connector-factory"; //$NON-NLS-1$
    public static final String APIMAN_GATEWAY_POLICY_FACTORY_CLASS = "apiman-gateway.policy-factory"; //$NON-NLS-1$
    public static final String APIMAN_GATEWAY_METRICS_CLASS = "apiman-gateway.metrics"; //$NON-NLS-1$

    public static final String APIMAN_GATEWAY_COMPONENT_PREFIX = "apiman-gateway.components."; //$NON-NLS-1$

    public static Configuration config;
    static {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(new SystemPropertiesConfiguration());
        compositeConfiguration.addConfiguration(ConfigFileConfiguration.create("apiman.properties")); //$NON-NLS-1$
        config = compositeConfiguration;
    }

    /**
     * Constructor.
     */
    public WarEngineConfig() {
    }

    /**
     * @return the configuration
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Returns the given configuration property name or the provided default
     * value if not found.
     * @param propertyName the property name
     * @param defaultValue the default value
     * @return the config property
     */
    public String getConfigProperty(String propertyName, String defaultValue) {
        return getConfig().getString(propertyName, defaultValue);
    }

    /**
     * @return the class to use as the {@link IRegistry}
     */
    @Override
    public Class<IRegistry> getRegistryClass() {
        return loadConfigClass(APIMAN_GATEWAY_REGISTRY_CLASS, IRegistry.class);
    }

    /**
     * @return all properties to be passed to the registry
     */
    @Override
    public Map<String, String> getRegistryConfig() {
        return getConfigMap(APIMAN_GATEWAY_REGISTRY_CLASS);
    }

    /**
     * @return the class to use as the {@link IPluginRegistry}
     */
    @Override
    public Class<IPluginRegistry> getPluginRegistryClass() {
        return loadConfigClass(APIMAN_GATEWAY_PLUGIN_REGISTRY_CLASS, IPluginRegistry.class);
    }

    /**
     * @return all properties to be passed to the registry
     */
    @Override
    public Map<String, String> getPluginRegistryConfig() {
        Map<String, String> configMap = getConfigMap(APIMAN_GATEWAY_PLUGIN_REGISTRY_CLASS);
        String pluginsDirOverride = System.getProperty(APIMAN_GATEWAY_PLUGIN_REGISTRY_CLASS + ".pluginsDir"); //$NON-NLS-1$
        if (pluginsDirOverride != null) {
            configMap.put("pluginsDir", pluginsDirOverride); //$NON-NLS-1$
        }
        return configMap;
    }

    /**
     * @return the class to use as the {@link IConnectorFactory}
     */
    @Override
    public Class<IConnectorFactory> getConnectorFactoryClass() {
        return loadConfigClass(APIMAN_GATEWAY_CONNECTOR_FACTORY_CLASS, IConnectorFactory.class);
    }

    /**
     * @return all properties to be passed to the factory
     */
    @Override
    public Map<String, String> getConnectorFactoryConfig() {
        return getConfigMap(APIMAN_GATEWAY_CONNECTOR_FACTORY_CLASS);
    }

    /**
     * @return the class to use as the {@link IPolicyFactory}
     */
    @Override
    public Class<IPolicyFactory> getPolicyFactoryClass() {
        return loadConfigClass(APIMAN_GATEWAY_POLICY_FACTORY_CLASS, IPolicyFactory.class);
    }

    /**
     * @return all properties to be passed to the factory
     */
    @Override
    public Map<String, String> getPolicyFactoryConfig() {
        return getConfigMap(APIMAN_GATEWAY_POLICY_FACTORY_CLASS);
    }

    /**
     * @return the class to use as the {@link IMetrics}
     */
    @Override
    public Class<IMetrics> getMetricsClass() {
        return loadConfigClass(APIMAN_GATEWAY_METRICS_CLASS, IMetrics.class);
    }

    /**
     * @return all properties to be passed to the factory
     */
    @Override
    public Map<String, String> getMetricsConfig() {
        return getConfigMap(APIMAN_GATEWAY_METRICS_CLASS);
    }

    /**
     * @return the class to use for the given component
     */
    @Override
    public <T extends IComponent> Class<T> getComponentClass(Class<T> componentType) {
        return loadConfigClass(APIMAN_GATEWAY_COMPONENT_PREFIX + componentType.getSimpleName(), componentType);
    }

    /**
     * @return all properties to be passed to the factory
     */
    @Override
    public <T extends IComponent> Map<String, String> getComponentConfig(Class<T> componentType) {
        return getConfigMap(APIMAN_GATEWAY_COMPONENT_PREFIX + componentType.getSimpleName());
    }

    /**
     * @return a loaded class
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> loadConfigClass(String property, Class<T> type) {
        String classname = getConfig().getString(property);
        if (classname == null) {
            throw new RuntimeException("No " + type.getSimpleName() + " class configured."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        try {
            Class<T> c = (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(classname);
            return c;
        } catch (ClassNotFoundException e) {
            // Not found via Class.forName() - try other mechanisms.
        }
        try {
            Class<T> c = (Class<T>) Class.forName(classname);
            return c;
        } catch (ClassNotFoundException e) {
            // Not found via Class.forName() - try other mechanisms.
        }
        throw new RuntimeException(Messages.i18n.format("WarEngineConfig.FailedToLoadClass", classname)); //$NON-NLS-1$
    }

    /**
     * Gets all properties in the engine configuration that are prefixed
     * with the given prefix.
     * @param prefix
     * @return all prefixed properties
     */
    private Map<String, String> getConfigMap(String prefix) {
        Map<String, String> rval = new HashMap<>();
        Iterator<?> keys = config.getKeys(prefix);
        while (keys.hasNext()) {
            String key = String.valueOf(keys.next());
            if (key.equals(prefix)) {
                continue;
            }
            String shortKey = key.substring(prefix.length() + 1);
            rval.put(shortKey, config.getString(key));
        }
        return rval;
    }
}
