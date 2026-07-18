package com.example.coffeeordersystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 읽기 전용 트랜잭션의 실제 Connection 획득이 Replica로 늦춰지는지 검증한다. */
class ReplicationRoutingDataSourceTest {

	@Test
	void 쓰기_트랜잭션은_Primary_Connection을_획득한다() {
		DataSource dataSource = routingDataSource();
		TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

		String url = transaction.execute(status -> connectionUrl(dataSource));

		assertThat(url).contains("routing-primary");
	}

	@Test
	void 읽기_전용_트랜잭션은_Replica_Connection을_획득한다() {
		DataSource dataSource = routingDataSource();
		TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
		transaction.setReadOnly(true);

		String url = transaction.execute(status -> connectionUrl(dataSource));

		assertThat(url).contains("routing-replica");
	}

	@Test
	void 트랜잭션_밖의_조회는_안전하게_Primary_Connection을_획득한다() {
		assertThat(connectionUrl(routingDataSource())).contains("routing-primary");
	}

	private DataSource routingDataSource() {
		DriverManagerDataSource primary = new DriverManagerDataSource("jdbc:h2:mem:routing-primary;DB_CLOSE_DELAY=-1", "sa", "");
		DriverManagerDataSource replica = new DriverManagerDataSource("jdbc:h2:mem:routing-replica;DB_CLOSE_DELAY=-1", "sa", "");
		ReplicationRoutingDataSource routing = new ReplicationRoutingDataSource();
		routing.setDefaultTargetDataSource(primary);
		routing.setTargetDataSources(Map.of(
				ReplicationDataSourceType.PRIMARY, primary,
				ReplicationDataSourceType.REPLICA, replica
		));
		routing.afterPropertiesSet();
		return new LazyConnectionDataSourceProxy(routing);
	}

	private String connectionUrl(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			return connection.getMetaData().getURL();
		} catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}
}
