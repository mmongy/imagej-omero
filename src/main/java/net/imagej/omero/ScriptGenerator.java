/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
 * Wisconsin-Madison and University of Dundee.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import net.imagej.ImageJ;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.Identifiable;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.UIDetails;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;

/**
 * Generates Jython stubs for running ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptGenerator extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptGenerator() {
		this(new ImageJ());
	}

	public ScriptGenerator(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptGenerator(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Generates OMERO script stubs for all available ImageJ modules. */
	public int generateAll(final File omeroDir, final boolean headlessOnly)
		throws IOException
	{
		final File scriptsDir = new File(new File(omeroDir, "lib"), "scripts");
		if (!scriptsDir.exists()) {
			System.err.println("OMERO scripts directory not found: " + scriptsDir);
			return 1;
		}

		final File dir = new File(scriptsDir, "imagej");
		if (dir.exists()) {
			System.err.println("Path already exists: " + dir);
			System.err.println("Please remove it if you wish to generate scripts.");
			return 2;
		}

		// create the directory
		final boolean success = dir.mkdirs();
		if (!success) {
			System.err.println("Could not create directory: " + dir);
			return 3;
		}

		// generate the scripts
		for (final ModuleInfo info : ij.module().getModules()) {
			if (isValid(info, headlessOnly)) generate(info, dir);
		}
		return 0;
	}

	/** Generates an OMERO script stub for the given ImageJ module. */
	public void generate(final ModuleInfo info, final File dir)
		throws IOException
	{
		// validate arguments
		if (!(info instanceof Identifiable)) {
			throw new IllegalArgumentException("Unidentifiable module: " + info);
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Invalid directory: " + dir);
		}

		// we will execute ImageJ.app/lib/run-script
		final File baseDir = ij.app().getApp().getBaseDirectory();
		final File libDir = new File(baseDir, "lib");
		final File runScript = new File(libDir, "run-script");
		final String exec = runScript.getAbsolutePath();

		// sanitize identifier
		final String id = ((Identifiable) info).getIdentifier();
		final String escapedID = id.replaceAll("\n", "\\\\n");

		// write the stub
		final File stubFile = formatFilename(dir, info);
		stubFile.getParentFile().mkdirs();
		final PrintWriter out = new PrintWriter(new FileWriter(stubFile));
		// Someday, we can perhaps improve OMERO to call the ImageJ
		// launcher directly rather than using Jython in this silly way.
		out.println("#!/usr/bin/env jython");
		out.println("from subprocess import call");
		out.println("exec = \"" + exec + "\"");
		out.println("id = \"" + escapedID + "\"");
		out.println("subprocess.call([exec, id])");
		out.close();
	}

	// -- Main method --

	/** Entry point for generating OMERO script stubs. */
	public static void main(final String... args) throws Exception {
		// parse arguments
		boolean headlessOnly = true;
		File dir = null;
		for (final String arg : args) {
			if ("--all".equals(arg)) headlessOnly = false;
			else dir = new File(arg);
		}

		System.err.println(new Date() + ": generating scripts");

		// NB: Make ImageJ startup less verbose.
		System.setProperty("scijava.log.level", "warn");

		// generate script stubs
		final ScriptGenerator scriptGenerator = new ScriptGenerator();
		int result = scriptGenerator.generateAll(dir, headlessOnly);

		// clean up resources
		scriptGenerator.getContext().dispose();

		System.err.println(new Date() + ": generation completed");

		// shut down the JVM, "just in case"
		System.exit(result);
	}

	// -- Helper methods --

	private File formatFilename(File dir, final ModuleInfo info) {
		final MenuPath menuPath = info.getMenuPath();

		if (menuPath == null || menuPath.isEmpty())
		{
			return new File(dir, sanitize(info.getTitle()) + ".jy");
		}

		File rv = null;
		for (final MenuEntry menu : menuPath) {
			final String n = sanitize(menu.getName());
			if (rv == null) {
				rv = new File(dir, n);
			}
			else {
				rv = new File(rv, n);
			}
		}
		return rv == null ? null : new File(rv.getParent(), rv.getName() + ".jy");
	}

	private String sanitize(final String str) {
		// replace undesirable characters (space, slash and backslash)
		String s = str.replaceAll("[ /\\\\]", "_");

		// remove ellipsis if present
		if (s.endsWith("...")) s = s.substring(0, s.length() - 3);

		return s;
	}

	private boolean isValid(final ModuleInfo info, final boolean headlessOnly) {
		if (!(info instanceof Identifiable)) return false;
		if (headlessOnly && !info.canRunHeadless()) return false;
		if (!UIDetails.APPLICATION_MENU_ROOT.equals(info.getMenuRoot())) return false;
		return true;
	}

}