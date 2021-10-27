/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.datamanager.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//config file is needed (even empty) to enable messageService
@Configuration
@EnableTransactionManagement
@EnableElasticsearchRepositories(basePackages = { "edu.unifi.disit.datamanager.datamodel.elasticdb" })
public class ElasticDBDbConfig {

	private static final Logger logger = LogManager.getLogger();

	@Value("${elasticsearch.protocol}")
	private String esProtocol;

	@Value("${elasticsearch.hosts}")
	private String[] esHosts;

	@Value("${elasticsearch.port}")
	private int esPort;

	@Value("${elasticsearch.clustername}")
	private String esClusterName;

	@Value("${elasticsearch.keystoretype}")
	private String keystoretype; // or .p12

	@Value("${elasticsearch.keystorefile}")
	private String keystoreFile;

	@Value("${elasticsearch.keystorepass}")
	private String keystorePass;

       	@Value("${elasticsearch.truststorefile}")
	private String truststoreFile;

	@Value("${elasticsearch.truststorepass}")
	private String truststorePass;

        @Value("${elasticsearch.keypass}")
	private String keypass;

	@Value("${elasticsearch.username}")
	private String esUsername;

	@Value("${elasticsearch.password}")
	private String esPassword;

	@Bean(destroyMethod = "close")
	public RestHighLevelClient elasticsearchClient() {

		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esUsername, esPassword));

		try {
			SSLContext sslContext;
                        KeyStore ks;
                        
                        if(truststoreFile != null && !truststoreFile.trim().isEmpty()) {
                                try {
                                    sslContext = SSLContexts.custom().loadTrustMaterial(new File(truststoreFile), truststorePass.toCharArray())
					.build();
                                } catch(IOException | CertificateException e) {
                                     logger.warn("Truststore "+truststoreFile+" problem", e);
                                     sslContext = null;
                                }
                        } else if((ks = readKeyStore(keystoreFile, keystoretype, keystorePass)) != null) {
                                sslContext = SSLContexts.custom().loadKeyMaterial(ks, keypass.toCharArray())
					.build();
                        } else if(esProtocol.equals("https")) {
                                sslContext = SSLContext.getDefault();
                        } else {
                                sslContext = null;
                        }
                        
                        final SSLContext sslCtx = sslContext;
			RestClientBuilder builder;
			if (!esUsername.equals("") && !esPassword.equals("")) {
				builder = RestClient
						.builder(Arrays.stream(esHosts).map(e -> new HttpHost(e, esPort, esProtocol))
								.toArray(HttpHost[]::new))
						.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
								.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslCtx));
			} else {
				builder = RestClient
						.builder(Arrays.stream(esHosts).map(e -> new HttpHost(e, esPort, esProtocol))
								.toArray(HttpHost[]::new))
						.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslCtx));
			}

			return new RestHighLevelClient(builder);
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.warn("Certificates Problem", e);
		}
		return null;

	}

	@Bean
	public ElasticsearchRestTemplate elasticsearchTemplate() {
		return new ElasticsearchRestTemplate(elasticsearchClient());
	}

	private KeyStore readKeyStore(String ksFile, String ksType, String ksPass) {
                if(ksFile == null || ksFile.isEmpty())
                  return null;
                try (InputStream keyStoreStream = new FileInputStream(ksFile)) {
			KeyStore keyStore = KeyStore.getInstance(ksType); // or "PKCS12"
			keyStore.load(keyStoreStream, ksPass.toCharArray());
			return keyStore;
                } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
                        try (InputStream keyStoreStream = this.getClass().getResourceAsStream(ksFile)) {
                                KeyStore keyStore = KeyStore.getInstance(ksType); // or "PKCS12"
                                keyStore.load(keyStoreStream, ksPass.toCharArray());
                                return keyStore;
                        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ee) {
                                logger.warn("Certificates Problem", ee);
                        }
                }
		return null;
	}

}
