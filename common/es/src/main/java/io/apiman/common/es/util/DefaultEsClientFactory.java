/*
 * Copyright 2015 JBoss Inc
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
package io.apiman.common.es.util;

import io.apiman.common.config.options.GenericOptionsParser;
import io.apiman.common.es.util.builder.index.EsIndexProperties;
import io.apiman.common.logging.ApimanLoggerFactory;
import io.apiman.common.logging.IApimanLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import static io.apiman.common.config.options.GenericOptionsParser.keys;

/**
 * Factory for creating elasticsearch clients.
 *
 * @author eric.wittmann@redhat.com
 */
public class DefaultEsClientFactory extends AbstractClientFactory implements IEsClientFactory {

    private static final int POLL_INTERVAL_SECS = 5;
    private static final IApimanLogger LOGGER = ApimanLoggerFactory.getLogger(DefaultEsClientFactory.class);

    /**
     * Creates a client from information in the config map.
     *
     * @param config             the configuration
     * @param esIndices          the ES index definitions
     * @param defaultIndexPrefix the default index prefix to use if not specified in the config
     * @return the ES client
     */
    @Override
    public RestHighLevelClient createClient(Map<String, String> config,
        Map<String, EsIndexProperties> esIndices,
        String defaultIndexPrefix) {
        ApimanEsClientOptionsParser parser = new ApimanEsClientOptionsParser(config, defaultIndexPrefix);
        return this.createEsClient(parser, esIndices);
    }

    /**
     * Creates an HTTP/REST client from a configuration map.
     *
     * @return the ES client
     */
    private RestHighLevelClient createEsClient(ApimanEsClientOptionsParser opts,
        Map<String, EsIndexProperties> esIndexes) {

        String protocol = opts.getProtocol();
        String host = opts.getHost();
        int port = opts.getPort();
        String indexNamePrefix = opts.getIndexNamePrefix();
        int timeout = opts.getTimeout();

        LOGGER.info("Building an elasticsearch-client for {0}://{1}:{2} for index prefix {3}",
            protocol, host, port, indexNamePrefix);

        synchronized (clients) {
            String clientKey = "es:" + host + ':' + port + '/' + indexNamePrefix;

            RestHighLevelClient client;

            if (clients.containsKey(clientKey)) {
                client = clients.get(clientKey);
                LOGGER.info("Use cached elasticsearch-client with client key " + clientKey);
            } else {
                RestClientBuilder clientBuilder = RestClient.builder(new HttpHost(host, port, protocol))
                    .setRequestConfigCallback(builder -> builder.setConnectTimeout(timeout)
                    .setSocketTimeout(timeout));

                HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClientBuilder.create();

                opts.getUsernameAndPassword().ifPresent(creds -> {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                    credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(creds.getUsername(), creds.getPasswordAsString())
                    );

                    asyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });

                if ("https".equalsIgnoreCase(protocol)) {
                    updateSslConfig(asyncClientBuilder, opts);
                }

                clientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> asyncClientBuilder);
                client = new RestHighLevelClient(clientBuilder);

                try {
                    this.waitForElasticsearch(client, opts.getPollingTime());
                    // put client to list if polling is successful
                    clients.put(clientKey, client);
                    LOGGER.info("Created new elasticsearch-client for {0}://{1}:{2} for index prefix {3}",
                            protocol, host, port, indexNamePrefix);
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }

            if (opts.isInitialize()) { //$NON-NLS-1$
                this.initializeIndices(client, esIndexes, indexNamePrefix);
            }

            return client;
        }
    }

    private void waitForElasticsearch(RestHighLevelClient client, long pollingTime) {
        EsConnectionPoller poller = new EsConnectionPoller(client, 0, POLL_INTERVAL_SECS,
            Math.toIntExact(pollingTime));
        poller.blockUntilReady();
    }

    /**
     * Configures the SSL connection to use certificates by setting the keystores
     *
     * @param asyncClientBuilder the client builder
     * @param config             the configuration
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/_encrypted_communication.html">Elasticsearch-Docs</>
     */
    @SuppressWarnings("nls")
    private void updateSslConfig(HttpAsyncClientBuilder asyncClientBuilder, GenericOptionsParser config) {
        boolean allowSelfSigned = config.getBool(keys("client.allowSelfSigned"), false);
        boolean allowAnyHost = config.getBool(keys("client.allowAnyHost"), false);

        try {
            String clientKeystorePath = config.getRequiredString(keys("client.keystore"), f -> Files.exists(Paths.get(f)), "not found at provided path");
            String clientKeystorePassword = config.getRequiredString(keys("client.keystore.password"), s -> true, null);
            String trustStorePath = config.getRequiredString(keys("client.truststore"), f -> Files.exists(Paths.get(f)), "not found at provided path");
            String trustStorePassword = config.getRequiredString(keys("client.truststore.password"), s -> true, null);

            // TODO -- needs to still work when no configuration is provided and the system default stores are being used
            // also needs to still support JKS and not just PKCS12.
            // we may be able to roll this into the TLSOptions, but there's the annoyance of the JSON issue (alias might be OK).


            Path trustStorePathObject = Paths.get(trustStorePath);
            Path keyStorePathObject = Paths.get(clientKeystorePath);
            KeyStore truststore = KeyStore.getInstance("pkcs12");
            KeyStore keyStore = KeyStore.getInstance("pkcs12");

            try (InputStream is = Files.newInputStream(trustStorePathObject)) {
                truststore.load(is, trustStorePassword.toCharArray());
            }
            try (InputStream is = Files.newInputStream(keyStorePathObject)) {
                keyStore.load(is, clientKeystorePassword.toCharArray());
            }

            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            if (allowSelfSigned) {
                sslContextBuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
            } else {
                sslContextBuilder.loadTrustMaterial(truststore, null);
                sslContextBuilder.loadKeyMaterial(keyStore, clientKeystorePassword.toCharArray());
            }
            SSLContext sslContext = sslContextBuilder.build();

            HostnameVerifier hostnameVerifier =
                allowAnyHost ? NoopHostnameVerifier.INSTANCE : new DefaultHostnameVerifier();

            SchemeIOSessionStrategy httpsIOSessionStrategy = new SSLIOSessionStrategy(sslContext,
                hostnameVerifier);
            asyncClientBuilder.setSSLStrategy(httpsIOSessionStrategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
