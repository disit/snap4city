/* Snap4City Engager (SE)
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
package edu.unifi.disit.snapengager.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
public class MultiHttpSecurityConfig {

	@Autowired
	private AccessTokenAuthenticationFilter myAccessTokenAuthenticationFilter;

	@Bean // to avoid double instantiantion in filter chain
	public FilterRegistrationBean filterRegistrationBean() {
		FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
		filterRegistrationBean.setEnabled(false);
		filterRegistrationBean.setFilter(myAccessTokenAuthenticationFilter);
		return filterRegistrationBean;
	}

	@Configuration
	@Order(1)
	public class TestSecurityConfigV1 extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
					.antMatcher("/api/test").antMatcher("/api/v1/sensortype/list")
					.authorizeRequests()
					/**/.anyRequest().permitAll();
		}
	}

	@Configuration
	@Order(2)
	public class RestSecurityConfigV1 extends WebSecurityConfigurerAdapter {

		@Autowired
		private MyCorsFilter myCorsFilter;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
					.csrf().disable()
					.addFilterBefore(myCorsFilter, ChannelProcessingFilter.class)
					.addFilterBefore(myAccessTokenAuthenticationFilter, BasicAuthenticationFilter.class)
					.antMatcher("/api/v*/**")
					.authorizeRequests()
					/**/.anyRequest().hasRole("USER");
		}
	}
}