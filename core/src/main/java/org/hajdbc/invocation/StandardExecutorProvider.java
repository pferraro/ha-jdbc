package org.hajdbc.invocation;

import java.util.concurrent.ExecutorService;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.invocation.AllResultsCollector.ExecutorProvider;

public class StandardExecutorProvider implements ExecutorProvider
{
	@Override
	public <Z, D extends Database<Z>> ExecutorService getExecutor(DatabaseCluster<Z, D> cluster)
	{
		return cluster.getExecutor();
	}
}