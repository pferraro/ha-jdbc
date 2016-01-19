package io.github.hajdbc.invocation;

import java.util.concurrent.ExecutorService;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.invocation.AllResultsCollector.ExecutorProvider;

public class StandardExecutorProvider implements ExecutorProvider
{
	@Override
	public <Z, D extends Database<Z>> ExecutorService getExecutor(DatabaseCluster<Z, D> cluster)
	{
		return cluster.getExecutor();
	}
}