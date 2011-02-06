/**
 * Copyright (c) 2010, Intalio Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intalio Inc. - initial API and implementation
 */
package org.eclipse.equinox.p2.director.extended;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ecf.core.util.Base64;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.extended.internal.ForkedDirectorApplication;

/**
 * This application extends equinox director application.
 * <p>
 * This application supports loading arguments from a properties file.
 * key value pairs in the properties file are added to the arguments on the command-line.
 * The arguments on the command-line override the ones in the properties file or the other way round.
 * depending on the prority of the loaded properties files.
 * The arguments on the command-line have the priority 10.
 * The argument to point to a properties file is -props&lt;PriorityNumber&gt;.
 * If the priorityNumber is undefined we assume '0'.
 * When the priorityNumber is bigger than 10 the properties file override the arguments on the command-line.
 * <br/>
 * System properties are susbistuted for each one of the arguments using the following synthax:
 * ${sysprop} or ${sysprop,defaultvalue}.
 * </p>
 * <p>
 * This application tries to auto-detect a set of arguments when they are not defined on the command-line:
 * <ul>
 * <li>p2.os, p2.arch and p2.ws; if undefined will be detected from the current OS</li>
 * <li>flavor: set to 'tooling' by default</li>
 * </ul>
 * </p>
 * <p>
 * Also this application add support for looking up the sources of 
 * the bundles that are going to be installed and or the sources of the bundles already installed.
 * Unless we find a better way the sources are located using the convention that the sources
 * of a given bundle have the same symbolic-name with the suffix &quot;.source&quot;.
 * <p>
 * <p>
 * Currently in p2director the upgrade operation is not supported.
 * Instead it is recommended to always make a new installation.
 * There is an automated archiving mode for existing destination folders.
 * The parameter on-existing-destination takes 3 values:
 * <ul>
 * <li>nothing (default): does not do a thing</li>
 * <li>fail:  will fail if the destination folder exists</li>
 * <li>archive: renames the existing destination folder with a timestamp of the installation</li>
 * </ul>
 * </p>
 * 
 * @author hmalphettes
 */
public class DirectorApplication extends ForkedDirectorApplication {

	public static String APP_ID = "org.eclipse.equinox.p2.director.extended";
		
	public void processArguments(String[] args) throws CoreException {
		if (args == null) {
			super.processArguments(args);
			return;
		}

		ArrayList<String> newArgs = ArgumentsLoader.loadArgumentsInPropertiesFiles(args);
		ArgumentsLoader.addComputedOsArchWsArguments(newArgs);
		
		args = newArgs.toArray(new String[newArgs.size()]);
		
		StringBuilder sb = new StringBuilder("Executing director with");
		for (String a : args) {
			sb.append(" ");
			sb.append(a);
		}
		System.err.println(sb);
		super.processArguments(args);
	}
			
}
