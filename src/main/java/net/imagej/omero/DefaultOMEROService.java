/*
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

import io.scif.services.DatasetIOService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.omero.roi.ROICache;
import net.imglib2.roi.MaskPredicate;

import org.scijava.Optional;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.table.Table;
import org.scijava.table.TableDisplay;
import org.scijava.util.TreeNode;
import org.scijava.util.Types;

import omero.gateway.model.ROIData;

/**
 * Default implementation of {@link OMEROService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultOMEROService extends AbstractService implements
	OMEROService, Optional
{

	// START HERE
	// 0. Fix compilation errors - how to expose ROICache?
	// 1. Update unit tests to match assumptions of current main codebase.
	// 2. Get tests passing. These don't require OMERO actually running.
	// 3. Get Docker-based OMERO up and running with omero-test-infra et al.
	// 4. Get ITs working with that Docker-based OMERO.
	// 5. Fix OMERO-server-side functionality also to work again.
	// - This might not actually be (5), if the ITs are testing this
	// (which they *should* be, and if they aren't, let's do that).

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private ConvertService convertService;

//-- Fields --

	private ROICache roiCache = new ROICache();
	private final HashMap<OMEROServer, OMEROSession> sessions = new HashMap<>();
	private final ThreadLocal<List<OMEROSession>> localSessions =
		new ThreadLocal<List<OMEROSession>>()
		{

			@Override
			public List<OMEROSession> initialValue() {
				return new ArrayList<>();
			}
		};

	// -- OMEROService methods --

	@Override
	public OMEROSession session(final OMEROServer server) throws OMEROException {
		return session(server, null);
	}

	@Override
	public OMEROSession session(final OMEROServer server,
		final OMEROCredentials credentials) throws OMEROException
	{
		// Have a cache hit, use it
		if (sessions.containsKey(server)) {
			OMEROSession session = sessions.get(server);
			session.restore(credentials);
			return session;
		}
		synchronized (sessions) {
			// For a cache miss we need credentials to authenticate with the server
			if (credentials != null) {
				if (!sessions.containsKey(server)) {
					final OMEROSession session = createSession(server, credentials);
					sessions.put(server, session);
				}
				return session(server);
			}
		}
		throw new IllegalStateException("No active session for server " + server);
	}

	@Override
	public OMEROSession createSession(final OMEROServer server,
		final OMEROCredentials credentials) throws OMEROException
	{
		return new OMEROSession(this, server, credentials);
	}

	@Override
	public OMEROSession session() {
		List<OMEROSession> sessionList = localSessions.get();
		if (sessionList.isEmpty()) {
			throw new IllegalStateException(
				"No active OMEROSession. OMEROService.pushSession must be called first.");
		}
		return sessionList.get(sessionList.size() - 1);
	}

	@Override
	public void pushSession(final OMEROSession omeroSession) {
		localSessions.get().add(omeroSession);
	}

	@Override
	public void popSession() {
		List<OMEROSession> sessionList = localSessions.get();
		sessionList.remove(sessionList.size() - 1);
	}

	@Override
	public omero.grid.Param getJobParam(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		final List<?> choices = item.getChoices();
		if (choices != null && !choices.isEmpty()) {
			param.values = (omero.RList) OMERO.rtype(choices);
		}
		final Object min = item.getMinimumValue();
		if (min != null) param.min = OMERO.rtype(min);
		final Object max = item.getMaximumValue();
		if (max != null) param.max = OMERO.rtype(max);
		return param;
	}

	@Override
	public ConvertService convert() {
		return convertService;
	}

	@Override
	public ImageDisplayService imageDisplay() {
		return imageDisplayService;
	}

	@Override
	public DatasetIOService datasetIO() {
		return datasetIOService;
	}

	@Override
	public ObjectService object() {
		return objectService;
	}

	@Override
	public DisplayService display() {
		return displayService;
	}

	@Override
	public ROICache roiCache() {
		return roiCache;
	}

	@Override
	public void addROIMapping(final Object roi, final ROIData shape) {
		roiCache().addROIMapping(roi, shape);
	}

	@Override
	public ROIData getROIMapping(final Object key) {
		return roiCache().getROIMapping(key);
	}

	@Override
	public void removeROIMapping(final Object key) {
		roiCache().removeROIMapping(key);
	}

	@Override
	@SuppressWarnings("deprecation")
	public omero.RType prototype(final Class<?> type) {
		// image types
		if (Dataset.class.isAssignableFrom(type) || DatasetView.class
			.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type) ||
			(convertService.supports(type, Dataset.class) && convertService.supports(
				Dataset.class, type)))
		{
			// use an image ID
			return omero.rtypes.rlong(0);
		}

		// table
		if (Table.class.isAssignableFrom(type) || TableDisplay.class
			.isAssignableFrom(type) || (convertService.supports(Table.class, type) &&
				convertService.supports(type, Table.class)))
		{
			// table file ID
			return omero.rtypes.rlong(0);
		}

		// ROI
		// When requesting a TreeNode it is assumed that the number provided is
		// an image ID and you want all the ROIs associated with that image.
		if (TreeNode.class.isAssignableFrom(type) || (convertService.supports(
			TreeNode.class, type) && convertService.supports(type, TreeNode.class)))
			return omero.rtypes.rlong(0);

		if (MaskPredicate.class.isAssignableFrom(type) || (convertService.supports(
			MaskPredicate.class, type) && convertService.supports(type,
				MaskPredicate.class))) return omero.rtypes.rlong(0);

		// primitive types
		final Class<?> saneType = Types.box(type);
		if (Boolean.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rbool(false);
		}
		if (Double.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rdouble(Double.NaN);
		}
		if (Float.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rfloat(Float.NaN);
		}
		if (Integer.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rint(0);
		}
		if (Long.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rlong(0L);
		}

		// data structure types
		if (type.isArray()) {
			return omero.rtypes.rarray();
		}
		if (List.class.isAssignableFrom(type)) {
			return omero.rtypes.rlist();
		}
		if (Map.class.isAssignableFrom(type)) {
			return omero.rtypes.rmap();
		}
		if (Set.class.isAssignableFrom(type)) {
			return omero.rtypes.rset();
		}

		// default case: convert to string
		// works for many types, including but not limited to:
		// - char
		// - java.io.File
		// - java.lang.Character
		// - java.lang.String
		// - java.math.BigDecimal
		// - java.math.BigInteger
		// - org.scijava.util.ColorRGB
		return omero.rtypes.rstring("");
	}

	// -- Disposable methods --

	@Override
	public void dispose() {
		for (final OMEROSession s : sessions.values()) {
			s.close();
		}
		sessions.clear();
	}
}
