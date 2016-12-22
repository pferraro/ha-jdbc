package org.hajdbc.invocation;

import java.util.SortedMap;

import org.hajdbc.Database;
import org.hajdbc.sql.ProxyFactory;

public interface InvocationStrategy
{
	<Z, D extends Database<Z>, T, R, E extends Exception> SortedMap<D, R> invoke(ProxyFactory<Z, D, T, E> proxy, Invoker<Z, D, T, R, E> invoker) throws E;
}
