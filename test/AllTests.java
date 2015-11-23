/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package test;

import test.mujmail.RMSStorageTest;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

/**
 * Runs all unit tests.
 * 
 * @author David Hauzar
 */
public class AllTests extends TestCase
{	
        /**
	 * Creates a test suite containing all J2MEUnit tests. 
	 *
	 * @return A new test suite
	 */
	public j2meunit.framework.Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new RMSStorageTest().suite());
                //suite.addTest(new BackgroundTaskTest().suite());

		return suite;
	}
}
