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

package net.imagej.omero.roi;

import net.imagej.axis.TypedAxis;
import net.imagej.space.TypedSpace;

import org.scijava.util.TreeNode;

/**
 * Wraps an {@link OMERORealMask} which can be projected into a given space.
 *
 * @author Alison Walter
 */
public interface OMEROROIElement extends TreeNode<OMERORealMask<?>> {

	OMERORealMask<?> projectIntoSpace(
		final TypedSpace<? extends TypedAxis> space);
}
