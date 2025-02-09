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

import net.imagej.axis.Axes;
import net.imagej.axis.TypedAxis;
import net.imglib2.roi.RealMaskRealInterval;

import omero.gateway.model.ShapeData;

/**
 * {@link RealMaskRealInterval } wrapper for OMERO {@link ShapeData} object.
 *
 * @author Alison Walter
 * @param <S> The type of {@link ShapeData} being wrapped
 */
public interface OMERORealMaskRealInterval<S extends ShapeData> extends
	OMERORealMask<S>, RealMaskRealInterval
{

	// TODO: Consider generalizing this to an "opinionated ROI" interface.
	@Override
	default boolean testPosition(final TypedAxis axis, final double position) {
		if (axis == Axes.X) return position <= realMax(0) && position >= realMin(0);
		if (axis == Axes.Y) return position <= realMax(1) && position >= realMin(1);
		if (axis == Axes.Z) return getShape().getZ() == -1 ? true : getShape()
			.getZ() == position;
		if (axis == Axes.TIME) return getShape().getT() == -1 ? true : getShape()
			.getT() == position;
		if (axis == Axes.CHANNEL) return getShape().getC() == -1 ? true : getShape()
			.getC() == position;
		return true;
	}

}
