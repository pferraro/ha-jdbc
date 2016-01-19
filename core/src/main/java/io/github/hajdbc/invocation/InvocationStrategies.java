package io.github.hajdbc.invocation;

import java.util.SortedMap;

import io.github.hajdbc.Database;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.sql.ProxyFactory;

public enum InvocationStrategies implements InvocationStrategy
{
	INVOKE_ON_ALL(new InvokeOnManyInvocationStrategy(new AllResultsCollector(new StandardExecutorProvider()))),
	INVOKE_ON_ANY(new InvokeOnAnyInvocationStrategy(new InvokeOnOneInvocationStrategy(new NextDatabaseSelector()))),
	INVOKE_ON_EXISTING(new InvokeOnManyInvocationStrategy(new ExistingResultsCollector())),
	INVOKE_ON_NEXT(new InvokeOnOneInvocationStrategy(new NextDatabaseSelector())),
	INVOKE_ON_PRIMARY(new InvokeOnOneInvocationStrategy(new PrimaryDatabaseSelector())),
	TRANSACTION_INVOKE_ON_ALL(new InvokeOnManyInvocationStrategy(new AllResultsCollector(new TransactionalExecutorProvider(false)))),
	END_TRANSACTION_INVOKE_ON_ALL(new InvokeOnManyInvocationStrategy(new AllResultsCollector(new TransactionalExecutorProvider(true)))),
	;
	
	private static final Logger logger = LoggerFactory.getLogger(SimpleInvoker.class);
	
	private final InvocationStrategy strategy;
	
	private InvocationStrategies(InvocationStrategy strategy)
	{
		this.strategy = strategy;
	}
	
	@Override
	public <Z, D extends Database<Z>, T, R, E extends Exception> SortedMap<D, R> invoke(ProxyFactory<Z, D, T, E> map, Invoker<Z, D, T, R, E> invoker) throws E
	{
		logger.log(Level.TRACE, "Invoking {0} using {1} strategy.", invoker, this);
		return this.strategy.invoke(map, invoker);
	}
}