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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
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

			final SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(readStore(), keypass.toCharArray())
					.build();
			RestClientBuilder builder = null;
			if (!esUsername.equals("") && !esPassword.equals("")) {
				builder = RestClient
						.builder(Arrays.stream(esHosts).map(e -> new HttpHost(e, esPort, esProtocol))
								.toArray(HttpHost[]::new))
						.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
								.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext));
			} else {
				builder = RestClient
						.builder(Arrays.stream(esHosts).map(e -> new HttpHost(e, esPort, esProtocol))
								.toArray(HttpHost[]::new))
						.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext));
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

	private KeyStore readStore() {
		try (InputStream keyStoreStream = this.getClass().getResourceAsStream(keystoreFile)) {
			KeyStore keyStore = KeyStore.getInstance(keystoretype); // or "PKCS12"
			keyStore.load(keyStoreStream, keystorePass.toCharArray());
			return keyStore;
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			logger.warn("Certificates Problem", e);
		}
		return null;
	}

}
