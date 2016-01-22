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
package io.github.hajdbc.lock.semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * An implementation of {@link java.util.concurrent.locks.Lock} using a binary semaphore.
 * Unlike the {@link java.util.concurrent.locks.ReentrantLock} this lock can be locked and unlocked by different threads.
 * Conditions are not supported.
 * 
 * @author Paul Ferraro
 */
public class SemaphoreLock implements Lock
{
	private final Semaphore semaphore;
	
	public SemaphoreLock(Semaphore semaphore)
	{
		this.semaphore = semaphore;
	}
	
	@Override
	public void lock()
	{
		this.semaphore.acquireUninterruptibly();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException
	{
		this.semaphore.acquire();
	}

	@Override
	public Condition newCondition()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean tryLock()
	{
		return this.semaphore.tryAcquire();
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
	{
		return this.semaphore.tryAcquire(time, unit);
	}

	@Override
	public void unlock()
	{
		this.semaphore.release();
	}
}
