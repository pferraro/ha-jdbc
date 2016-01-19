/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2012  Paul Ferraro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.hajdbc.pool.generic;

import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.pool.Pool;
import io.github.hajdbc.pool.PoolFactory;
import io.github.hajdbc.pool.PoolProvider;

/**
 * Adapter for a <a href="http://commons.apache.org/pool">commons-pool</a> GenericObjectPool.
 * 
 * @author Paul Ferraro
 */
public class GenericObjectPoolFactory implements PoolFactory
{
	static final Logger logger = LoggerFactory.getLogger(GenericObjectPoolFactory.class);
	
	private final GenericObjectPool.Config config;
	
	public GenericObjectPoolFactory(GenericObjectPoolConfiguration config)
	{
		this.config = config.toConfig();
	}
	
	@Override
	public <T, E extends Exception> Pool<T, E> createPool(final PoolProvider<T, E> provider)
	{
		PoolableObjectFactory<T> factory = new PoolableObjectFactory<T>()
		{
			@Override
			public void destroyObject(T object)
			{
				provider.close(object);
			}

			@Override
			public T makeObject() throws Exception
			{
				return provider.create();
			}

			@Override
			public boolean validateObject(T object)
			{
				return provider.isValid(object);
			}
			
			@Override
			public void activateObject(T object)
			{
			}

			@Override
			public void passivateObject(T object)
			{
			}
		};

		final ObjectPool<T> pool = new GenericObjectPool<>(factory, this.config);
		
		return new Pool<T, E>()
		{
			@Override
			public void close()
			{
				try
				{
					pool.close();
				}
				catch (Exception e)
				{
					logger.log(Level.WARN, e, e.getMessage());
				}
			}

			@Override
			public void release(T item)
			{
				try
				{
					pool.returnObject(item);
				}
				catch (Exception e)
				{
					logger.log(Level.WARN, e, e.getMessage());
				}
			}

			@Override
			public T take() throws E
			{
				try
				{
					return pool.borrowObject();
				}
				catch (NoSuchElementException e)
				{
					return provider.create();
				}
				catch (IllegalStateException e)
				{
					throw e;
				}
				catch (RuntimeException | Error e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw provider.getExceptionClass().cast(e);
				}
			}
		};
	}
}
