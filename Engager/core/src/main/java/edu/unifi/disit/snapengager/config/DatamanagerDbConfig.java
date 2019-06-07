/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.snapengager.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		entityManagerFactoryRef = "datamanagerdbEntityManagerFactory",
		transactionManagerRef = "datamanagerdbTransactionManager",
		basePackages = { "edu.unifi.disit.snapengager.datamodel.datamanagerdb" })
public class DatamanagerDbConfig {

	@Value("${datasource.validationQuery}")
	private String validationQuery;

	@Value("${datasource.removeAbandonedTimeout}")
	private Integer removeAbandonedTimeout;

	@Value("${datasource.maxActive}")
	private Integer maxActive;

	@Value("${datasource.maxIdle}")
	private Integer maxIdle;

	@Value("${datasource.maxWait}")
	private Integer maxWait;

	@Bean(name = "datamanagerdbDataSource")
	@ConfigurationProperties(prefix = "datamanagerdb.datasource")
	public DataSource dataSource() {
		org.apache.tomcat.jdbc.pool.DataSource d = (org.apache.tomcat.jdbc.pool.DataSource) DataSourceBuilder.create().build();

		d.setValidationQuery(validationQuery);
		d.setRemoveAbandonedTimeout(removeAbandonedTimeout);
		d.setRemoveAbandoned(true);
		d.setTestOnBorrow(true);
		d.setTestOnConnect(true);
		d.setTestWhileIdle(true);
		d.setLogAbandoned(true);
		d.setMaxActive(maxActive);
		d.setMaxIdle(maxIdle);
		d.setMaxWait(maxWait);

		return d;
	}

	@Bean(name = "datamanagerdbEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier("datamanagerdbDataSource") DataSource dataSource) {
		return builder
				.dataSource(dataSource)
				.packages("edu.unifi.disit.snapengager.datamodel.datamanagerdb")
				.persistenceUnit("KPIValue")
				.build();
	}

	@Bean(name = "datamanagerdbTransactionManager")
	public PlatformTransactionManager transactionManager(
			@Qualifier("datamanagerdbEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

}