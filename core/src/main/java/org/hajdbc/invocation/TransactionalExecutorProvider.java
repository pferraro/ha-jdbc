package org.hajdbc.invocation;

import java.util.concurrent.ExecutorService;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.invocation.AllResultsCollector.ExecutorProvider;

public class TransactionalExecutorProvider implements ExecutorProvider
{
	private final boolean end;
	
	public TransactionalExecutorProvider(boolean end)
	{
		this.end = end;
	}
	
	@Override
	public <Z, D extends Database<Z>> ExecutorService getExecutor(DatabaseCluster<Z, D> cluster)
	{
		return cluster.getTransactionMode().getTransactionExecutor(cluster.getExecutor(), this.end);
	}
}