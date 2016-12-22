/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2015  Paul Ferraro
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
package io.github.hajdbc.durability;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.github.hajdbc.util.Builder;
import io.github.hajdbc.util.SimpleStaticRegistry;
import io.github.hajdbc.util.StaticRegistry;

/**
 * @author Paul Ferraro
 */
public class DurabilityPhaseRegistryBuilder implements Builder<StaticRegistry<Method, Durability.Phase>>
{
	private final Map<Method, Durability.Phase> phases = new HashMap<>();

	public DurabilityPhaseRegistryBuilder phase(Durability.Phase phase, Method... methods)
	{
		for (Method method : methods)
		{
			this.phases.put(method, phase);
		}
		return this;
	}

	@Override
	public StaticRegistry<Method, Durability.Phase> build()
	{
		return new SimpleStaticRegistry<>(this.phases);
	}
}
