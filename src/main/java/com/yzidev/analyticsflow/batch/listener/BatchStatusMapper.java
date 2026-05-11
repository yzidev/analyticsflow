package com.yzidev.analyticsflow.batch.listener;

import org.springframework.batch.core.BatchStatus;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;

final class BatchStatusMapper {

	private BatchStatusMapper() {
	}

	static EtlJobStatus toEtlStatus(BatchStatus status) {
		if (status == null) {
			return EtlJobStatus.PENDING;
		}
		return switch (status) {
			case STARTING, STARTED -> EtlJobStatus.RUNNING;
			case COMPLETED -> EtlJobStatus.COMPLETED;
			case FAILED, ABANDONED, UNKNOWN -> EtlJobStatus.FAILED;
			case STOPPING, STOPPED -> EtlJobStatus.STOPPED;
		};
	}
}
