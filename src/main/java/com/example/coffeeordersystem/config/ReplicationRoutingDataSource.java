package com.example.coffeeordersystem.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 활성 트랜잭션의 읽기 전용 여부로 데이터 원본을 선택한다.
 *
 * 트랜잭션이 없거나 쓰기 가능한 트랜잭션은 최신 원본이 필요한 경로로 보아 Primary를 사용한다.
 */
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		if (TransactionSynchronizationManager.isActualTransactionActive()
				&& TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			return ReplicationDataSourceType.REPLICA;
		}
		return ReplicationDataSourceType.PRIMARY;
	}
}
