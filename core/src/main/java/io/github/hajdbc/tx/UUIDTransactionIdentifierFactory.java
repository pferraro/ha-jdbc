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
package io.github.hajdbc.tx;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Transaction identifier factory that generates random UUIDs.
 * This implementation is safe for <distributable/> clusters.
 * @author Paul Ferraro
 */
public class UUIDTransactionIdentifierFactory implements TransactionIdentifierFactory<UUID>
{
	@Override
	public UUID createTransactionIdentifier()
	{
		return UUID.randomUUID();
	}

	@Override
	public byte[] serialize(UUID transactionId)
	{
		return ByteBuffer.allocate(this.size()).putLong(transactionId.getMostSignificantBits()).putLong(transactionId.getLeastSignificantBits()).array();
	}

	@Override
	public UUID deserialize(byte[] bytes)
	{
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		return new UUID(buffer.getLong(), buffer.getLong());
	}

	@Override
	public int size()
	{
		return Long.SIZE * 2;
	}
}
