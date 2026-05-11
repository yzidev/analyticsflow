package com.yzidev.analyticsflow.batch.staging;

import java.util.List;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

@FunctionalInterface
public interface StagingEntityMapper<T extends BaseStagingEntity> {

	T map(List<String> values);
}
