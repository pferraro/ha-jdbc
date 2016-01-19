/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2016  Paul Ferraro
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
package io.github.hajdbc.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

/**
 * @author Paul Ferraro
 *
 */
public class ReadWriteLockManager implements LockManager
{
	private final ConcurrentMap<String, ReadWriteLock> lockMap = new ConcurrentHashMap<>();

	private final Supplier<ReadWriteLock> factory;
	
	public ReadWriteLockManager(Supplier<ReadWriteLock> factory)
	{
		this.factory = factory;
	}
	
	/**
	 * @see io.github.hajdbc.lock.LockManager#readLock(java.lang.String)
	 */
	@Override
	public Lock readLock(String object)
	{
		Lock lock = this.getReadWriteLock(null).readLock();
		
		return (object == null) ? lock : new GlobalLock(lock, this.getReadWriteLock(object).readLock());
	}
	
	/**
	 * @see io.github.hajdbc.lock.LockManager#writeLock(java.lang.String)
	 */
	@Override
	public Lock writeLock(String object)
	{
		ReadWriteLock readWriteLock = this.getReadWriteLock(null);
		
		return (object == null) ? readWriteLock.writeLock() : new GlobalLock(readWriteLock.readLock(), this.getReadWriteLock(object).writeLock());
	}
	
	private ReadWriteLock getReadWriteLock(String object)
	{
		// CHM cannot use a null key
		String key = (object != null) ? object : "";
		
		return this.lockMap.computeIfAbsent(key, (String id) -> this.factory.get());
	}
	
	private static class GlobalLock implements Lock
	{
		private Lock globalLock;
		private Lock lock;
		
		GlobalLock(Lock globalLock, Lock lock)
		{
			this.globalLock = globalLock;
			this.lock = lock;
		}
		
		@Override
		public void lock()
		{
			this.globalLock.lock();
			this.lock.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException
		{
			this.globalLock.lockInterruptibly();
			
			try
			{
				this.lock.lockInterruptibly();
			}
			catch (InterruptedException e)
			{
				this.globalLock.unlock();
				throw e;
			}
		}

		@Override
		public boolean tryLock()
		{
			if (this.globalLock.tryLock())
			{
				if (this.lock.tryLock())
				{
					return true;
				}

				this.globalLock.unlock();
			}

			return false;
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
		{
			if (this.globalLock.tryLock(time, unit))
			{
				if (this.lock.tryLock(time, unit))
				{
					return true;
				}

				this.globalLock.unlock();
			}

			return false;
		}

		@Override
		public void unlock()
		{
			this.lock.unlock();
			this.globalLock.unlock();
		}

		@Override
		public Condition newCondition()
		{
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void start()
	{
		// Do nothing
	}

	@Override
	public void stop()
	{
		// Do nothing
	}
}
