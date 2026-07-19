package com.example.coffeeordersystem.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Map;

/** Primary 쓰기 원본과 Replica 읽기 원본을 Spring 트랜잭션 경계에 맞춰 연결한다. */
@Configuration(proxyBeanMethods = false)
public class ReplicationDataSourceConfig {

	@Bean
	@ConfigurationProperties("app.datasource.primary")
	DataSourceProperties primaryDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties("app.datasource.primary.hikari")
	HikariDataSource primaryDataSource(
			@Qualifier("primaryDataSourceProperties") DataSourceProperties properties
	) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	@Bean
	@ConfigurationProperties("app.datasource.replica")
	DataSourceProperties replicaDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties("app.datasource.replica.hikari")
	HikariDataSource replicaDataSource(
			@Qualifier("replicaDataSourceProperties") DataSourceProperties properties
	) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	@Bean
	ReplicationRoutingDataSource replicationRoutingDataSource(
			@Qualifier("primaryDataSource") DataSource primaryDataSource,
			@Qualifier("replicaDataSource") DataSource replicaDataSource
	) {
		ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
		routingDataSource.setDefaultTargetDataSource(primaryDataSource);
		routingDataSource.setTargetDataSources(Map.of(
				ReplicationDataSourceType.PRIMARY, primaryDataSource,
				ReplicationDataSourceType.REPLICA, replicaDataSource
		));
		routingDataSource.afterPropertiesSet();
		return routingDataSource;
	}

	@Bean
	@Primary
	DataSource dataSource(ReplicationRoutingDataSource replicationRoutingDataSource) {
		return new LazyConnectionDataSourceProxy(replicationRoutingDataSource);
	}
}
