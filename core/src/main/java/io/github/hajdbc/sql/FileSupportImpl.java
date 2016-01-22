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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import io.github.hajdbc.ExceptionFactory;
import io.github.hajdbc.util.Files;

/**
 * @author  Paul Ferraro
 * @since   1.0
 */
public class FileSupportImpl<E extends Exception> implements FileSupport<E>
{
	private static final String TEMP_FILE_SUFFIX = ".lob";
	private static final int BUFFER_SIZE = 8192;
	
	private final List<File> files = new LinkedList<>();
	private final ExceptionFactory<E> exceptionFactory;
	
	public FileSupportImpl(ExceptionFactory<E> exceptionFactory)
	{
		this.exceptionFactory = exceptionFactory;
	}
	
	@Override
	public File createFile(InputStream inputStream) throws E
	{
		try
		{
			File file = this.createTempFile();
			
			try (FileOutputStream output = new FileOutputStream(file))
			{
				FileChannel fileChannel = output.getChannel();
				ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
				
				ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				
				while (inputChannel.read(buffer) > 0)
				{
					buffer.flip();
					fileChannel.write(buffer);
					buffer.compact();
				}
				
				return file;
			}
		}
		catch (IOException e)
		{
			throw this.exceptionFactory.createException(e);
		}
	}
	
	@Override
	public File createFile(Reader reader) throws E
	{
		try
		{
			File file = this.createTempFile();
			
			try (Writer writer = new FileWriter(file))
			{
				CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
				
				while (reader.read(buffer) > 0)
				{
					buffer.flip();
					writer.append(buffer);
					buffer.clear();
				}
				
				return file;
			}
		}
		catch (IOException e)
		{
			throw this.exceptionFactory.createException(e);
		}
	}
	
	@Override
	public Reader getReader(File file) throws E
	{
		try
		{
			return new BufferedReader(new FileReader(file), BUFFER_SIZE);
		}
		catch (IOException e)
		{
			throw this.exceptionFactory.createException(e);
		}
	}
	
	@SuppressWarnings("resource")
	@Override
	public InputStream getInputStream(File file) throws E
	{
		try
		{
			return Channels.newInputStream(new FileInputStream(file).getChannel());
		}
		catch (IOException e)
		{
			throw this.exceptionFactory.createException(e);
		}
	}
	
	/**
	 * Creates a temp file and stores a reference to it so that it can be deleted later.
	 * @return a temp file
	 * @throws SQLException if an IO error occurs
	 */
	private File createTempFile() throws IOException
	{
		File file = Files.createTempFile(TEMP_FILE_SUFFIX);
		
		this.files.add(file);
		
		return file;
	}
	
	@Override
	public void close()
	{
		for (File file: this.files)
		{
			if (!file.delete())
			{
				file.deleteOnExit();
			}
		}
		this.files.clear();
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		
		super.finalize();
	}
}
