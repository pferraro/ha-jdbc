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

import io.github.hajdbc.lock.LockManager;
import io.github.hajdbc.lock.LockManagerFactory;
import io.github.hajdbc.lock.ReadWriteLockManager;

public class SemaphoreLockManagerFactory implements LockManagerFactory
{
	private static final long serialVersionUID = -1330668107554832289L;

	private boolean fair;
	
	public void setFair(boolean fair)
	{
		this.fair = fair;
	}
	
	public boolean isFair()
	{
		return this.fair;
	}

	@Override
	public String getId()
	{
		return "semaphore";
	}
	
	@Override
	public LockManager createLockManager()
	{
		return new ReadWriteLockManager(() -> new SemaphoreReadWriteLock(new Semaphore(Integer.MAX_VALUE, this.fair)));
	}
}
