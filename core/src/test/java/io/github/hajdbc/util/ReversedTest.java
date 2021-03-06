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
package io.github.hajdbc.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.github.hajdbc.util.Reversed;

/**
 * @author Paul Ferraro
 *
 */
public class ReversedTest
{
	@Test
	public void test()
	{
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<Integer> result = new ArrayList<>(3);
		
		for (Integer i: new Reversed<>(list))
		{
			result.add(i);
		}
		
		java.util.Collections.reverse(result);
		
		assertEquals(list, result);
	}
}
