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
package io.apiman.gateway.engine.ispn;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.components.IDataStoreComponent;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * Data Store component backed by an ISPN cache. This allows the data store
 * to be easily clusterable and the data evictable.
 *
 * @author rubenrm1@gmail.com
 */
public class InfinispanDataStoreComponent implements IDataStoreComponent {

    private static final String DEFAULT_CACHE_CONTAINER = "java:jboss/infinispan/container/apiman-gateway"; //$NON-NLS-1$
    private static final String DEFAULT_CACHE = "data-store"; //$NON-NLS-1$

    private String cacheContainer;
    private String cacheName;
    
    private Cache<Object, Object> cache;
    
    /**
     * Constructor.
     */
    public InfinispanDataStoreComponent() {
        cacheContainer = DEFAULT_CACHE_CONTAINER;
        cacheName = DEFAULT_CACHE;
    }

    /**
     * Constructor.
     * @param config
     */
    public InfinispanDataStoreComponent(Map<String, String> config) {
        cacheContainer = DEFAULT_CACHE_CONTAINER;
        cacheName = DEFAULT_CACHE;
        
        if (config.containsKey("cache-container")) { //$NON-NLS-1$
            cacheContainer = config.get("cache-container"); //$NON-NLS-1$
        }
        if (config.containsKey("cache")) { //$NON-NLS-1$
            cacheName = config.get("cache"); //$NON-NLS-1$
        }
    }

    /**
     * @see io.apiman.gateway.engine.components.IDataStoreComponent#hasProperty(java.lang.String, java.lang.String)
     */
    @Override
    public <T> boolean hasProperty(String namespace, String propertyName) {
        QName qname = new QName(namespace, propertyName);
        return getCache().containsKey(qname);
    }
    
    /**
     * @see io.apiman.gateway.engine.components.IDataStoreComponent#getProperty(java.lang.String, java.lang.String, java.lang.Object, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> void getProperty(String namespace, String propertyName, T defaultValue,
            IAsyncResultHandler<T> handler) {
        QName qname = new QName(namespace, propertyName);
        if (getCache().containsKey(qname)) {
            try {
                T rval = (T) getCache().get(qname);
                handler.handle(AsyncResultImpl.create(rval));
            } catch (Exception e) {
                handler.handle(AsyncResultImpl.<T>create(e));
            }
            
        } else {
            handler.handle(AsyncResultImpl.create(defaultValue));
        }
    }

    /**
     * @see io.apiman.gateway.engine.components.IDataStoreComponent#setProperty(java.lang.String, java.lang.String, java.lang.Object, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public <T> void setProperty(String namespace, String propertyName, T value, IAsyncResultHandler<T> handler) {
        setProperty(namespace, propertyName, value, null, handler);
    }

    /**
     * @see io.apiman.gateway.engine.components.IDataStoreComponent#setProperty(java.lang.String, java.lang.String, java.lang.Object, java.lang.Long, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> void setProperty(String namespace, String propertyName, T value, Long expiration, IAsyncResultHandler<T> handler) {
        QName qname = new QName(namespace, propertyName);
        try {
            T oldValue = null;
            if(expiration == null) {
                oldValue = (T) getCache().put(qname, value);
            } else {
                oldValue = (T) getCache().put(qname, value, expiration, TimeUnit.SECONDS);
            }
            handler.handle(AsyncResultImpl.create(oldValue));
        } catch (Exception e) {
            handler.handle(AsyncResultImpl.<T>create(e));
        }
    }

    /**
     * @see io.apiman.gateway.engine.components.IDataStoreComponent#clearProperty(java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> void clearProperty(String namespace, String propertyName, IAsyncResultHandler<T> handler) {
        QName qname = new QName(namespace, propertyName);
        try {
            T oldValue = (T) getCache().remove(qname);
            handler.handle(AsyncResultImpl.create(oldValue));
        } catch (Exception e) {
            handler.handle(AsyncResultImpl.<T>create(e));
        }
    }

    /**
     * @return gets the registry cache
     */
    private Cache<Object, Object> getCache() {
        if (cache != null) {
            return cache;
        }
        
        try {
            InitialContext ic = new InitialContext();
            CacheContainer container = (CacheContainer) ic.lookup(cacheContainer);
            cache = container.getCache(cacheName);
            return cache;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}