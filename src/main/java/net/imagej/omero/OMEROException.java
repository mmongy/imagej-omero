/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2022 Open Microscopy Environment:
 * 	- Board of Regents of the University of Wisconsin-Madison
 * 	- Glencoe Software, Inc.
 * 	- University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package net.imagej.omero;

/**
 * A checked exception encompassing the various things that can go wrong when
 * communicating with an OMERO server. Check the exception cause for more
 * details on the specific issue.
 *
 * @author Curtis Rueden
 */
public class OMEROException extends Exception {

	public OMEROException(final String message) {
		super(message);
	}

	public OMEROException(final Throwable cause) {
		super(cause);
	}

	public OMEROException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
