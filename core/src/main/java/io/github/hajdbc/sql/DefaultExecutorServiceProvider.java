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
package io.github.hajdbc.sql;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import io.github.hajdbc.ExecutorServiceProvider;

/**
 * @author paul
 *
 */
public class DefaultExecutorServiceProvider implements ExecutorServiceProvider, Serializable
{
	private static final long serialVersionUID = 5781743869682086889L;

	@Override
	public ExecutorService getExecutor(ThreadFactory threadFactory)
	{
		return Executors.newCachedThreadPool(threadFactory);
	}

	@Override
	public void release(ExecutorService executor)
	{
		executor.shutdown();
	}
}
