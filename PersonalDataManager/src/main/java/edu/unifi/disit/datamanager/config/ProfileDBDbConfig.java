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
package edu.unifi.disit.datamanager.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//config file is needed (even empty) to enable messageService
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		entityManagerFactoryRef = "profiledbEntityManagerFactory",
		transactionManagerRef = "profiledbTransactionManager",
		basePackages = { "edu.unifi.disit.datamanager.datamodel.profiledb" })
public class ProfileDBDbConfig {

	@Primary
	@Bean(name = "profiledbDataSource")
	@ConfigurationProperties(prefix = "profiledb.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().build();
	}

	@Primary
	@Bean(name = "profiledbEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier("profiledbDataSource") DataSource dataSource) {
		return builder
				.dataSource(dataSource)
				.packages("edu.unifi.disit.datamanager.datamodel.profiledb")
				// .properties(jpaProperties())
				.persistenceUnit("Data")
				.persistenceUnit("Consent")
				.persistenceUnit("Delegation")
				.persistenceUnit("Ownership")
				.persistenceUnit("Activity")
				.persistenceUnit("ActivityViolation")
				.build();
	}

	@Primary
	@Bean(name = "profiledbTransactionManager")
	public PlatformTransactionManager transactionManager(
			@Qualifier("profiledbEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

	// private Map<String, Object> jpaProperties() {
	// Map<String, Object> props = new HashMap<>();
	// props.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
	// props.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
	// return props;
	// }
}