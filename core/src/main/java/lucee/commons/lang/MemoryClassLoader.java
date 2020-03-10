/**
 * Copyright (c) 2015, Lucee Assosication Switzerland. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package lucee.commons.lang;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import lucee.commons.io.SystemUtil;
import lucee.runtime.config.Config;
import lucee.runtime.instrumentation.InstrumentationFactory;
import lucee.transformer.bytecode.util.ClassRenamer;

/**
 * ClassLoader that loads classes in memory that are not stored somewhere physically
 */
public final class MemoryClassLoader extends ExtendableClassLoader {

	private Config config;
	private ClassLoader pcl;
	private long size;

	/**
	 * Constructor of the class
	 * 
	 * @param directory
	 * @param parent
	 * @throws IOException
	 */
	public MemoryClassLoader(Config config, ClassLoader parent) throws IOException {
		super(parent);
		this.pcl = parent;
		this.config = config;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, false);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			// First, check if the class has already been loaded
			Class<?> c = findLoadedClass(name);
			if (c == null) {
				try {
					c = pcl.loadClass(name);// if(name.indexOf("sub")!=-1)print.ds(name);
				}
				catch (Throwable t) {
					ExceptionUtil.rethrowIfNecessary(t);
					c = findClass(name);
				}
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		throw new ClassNotFoundException("class " + name + " is invalid or doesn't exist");
	}

	@Override
	public Class<?> loadClass(String name, byte[] barr) throws UnmodifiableClassException {

		Class<?> clazz = null;

		synchronized (getClassLoadingLock(name)) {
			// new class , not in memory yet
			try {
				clazz = loadClass(name); // we do not load existing class from disk
			}
			catch (ClassNotFoundException cnf) {}
			if (clazz == null) return _loadClass(name, barr);

			Instrumentation instr = InstrumentationFactory.getInstrumentation(config);
			if (instr != null) {
				try {
					synchronized (InstrumentationFactory.lockToken) {
						instr.redefineClasses(new ClassDefinition(clazz, barr));
					}
					return clazz;
				}
				catch (ClassNotFoundException e) {
					// the documentation clearly sais that this exception only exists for backward compatibility and
					// never happen
					throw new RuntimeException(e);
				}
			}
			else {
				return rename(clazz, barr);
			}
		}
	}

	private Class<?> rename(Class<?> clazz, byte[] barr) {
		String newName = clazz.getName() + "$" + PhysicalClassLoader.uid();
		return _loadClass(newName, ClassRenamer.rename(barr, newName));
	}

	private Class<?> _loadClass(String name, byte[] barr) {
		size += barr.length;
		// class not exists yet
		try {
			return defineClass(name, barr, 0, barr.length);
		}
		catch (Throwable t) {
			ExceptionUtil.rethrowIfNecessary(t);
			SystemUtil.sleep(1);
			try {
				return defineClass(name, barr, 0, barr.length);
			}
			catch (Throwable t2) {
				ExceptionUtil.rethrowIfNecessary(t2);
				SystemUtil.sleep(1);
				return defineClass(name, barr, 0, barr.length);
			}
		}
	}

	public long getSize() {
		return size;
	}
}