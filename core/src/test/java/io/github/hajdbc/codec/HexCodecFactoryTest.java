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
package io.github.hajdbc.codec;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import io.github.hajdbc.codec.Codec;
import io.github.hajdbc.codec.CodecFactory;
import io.github.hajdbc.codec.hex.HexCodecFactory;

/**
 * @author Paul Ferraro
 */
public class HexCodecFactoryTest
{
	private CodecFactory factory = new HexCodecFactory();
	
	@Test
	public void encode() throws SQLException
	{
		Codec codec = this.factory.createCodec(null);
		
		Assert.assertEquals("70617373776F7264", codec.encode("password"));
	}
	
	@Test
	public void decode() throws SQLException
	{
		Codec codec = this.factory.createCodec(null);
		
		Assert.assertEquals("password", codec.decode("70617373776F7264"));
	}
}
