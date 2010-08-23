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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
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
 * 
 * @author hmalphettes
 */
public class DirectorApplication extends ForkedDirectorApplication {

	public void processArguments(String[] args) throws CoreException {
		if (args == null) {
			super.processArguments(args);
			return;
		}

		boolean p2Os=false, p2arch=false, p2ws=false;
		Map<String,String> props = null;
		
		for (int i = 0; i < args.length; i++) {
			String opt = args[i];
			if (opt.equals("-p2.os")) {
				p2Os = true;
			} else if (opt.equals("-p2.arch")) {
				p2arch = true;
			} else if (opt.equals("-p2.ws")) {
				p2ws = true;
			} else if (opt.startsWith("-props") && i+1 < args.length) {
				if (props == null) {
					props = new LinkedHashMap<String, String>();
				}
				props.put(opt, args[i+1]);
				i++;
			}
		}
		if (!p2Os || !p2arch || !p2ws || props != null) {
			ArrayList<String> newArgs = new ArrayList<String>();
			for (int i = 0; i < args.length; i++) {
				String opt = args[i];
				if (opt.startsWith("-props")) {
					i = i+2;
				} else {
					newArgs.add(opt);
				}
			}
			if (!p2Os) {
				newArgs.add("-p2.os");
				String os = Platform.getOS();
				if (os == null || os.equals(Platform.OS_UNKNOWN)) {
					System.err.println("Warning unable to identify the OS");
					if ("\\".equals(System.getProperty("file.separator"))) {
						os = Platform.OS_WIN32;
					} else {
						os = Platform.OS_LINUX;
					}
				}
				newArgs.add(os);
			}
			if (!p2arch) {
				newArgs.add("-p2.arch");
				String arch = Platform.getOSArch();
				if (arch == null) {
					arch = System.getProperty("os.arch");
				}
				newArgs.add(arch);
			}
			if (!p2ws) {
				newArgs.add("-p2.ws");
				String ws = Platform.getWS();
				if (ws == null || ws.equals(Platform.WS_UNKNOWN)) {
					String os = Platform.getOS();
					if (os != null) {
						if (os.equals(Platform.OS_LINUX)) {
							ws = Platform.WS_GTK;
						} else if (os.equals(Platform.OS_WIN32)) {
							ws = Platform.WS_WIN32;
						} else if (os.equals(Platform.OS_MACOSX)) {
							ws = Platform.WS_COCOA;
						}
					}
					System.err.println("Warning unable to identify the Windows System (aka 'ws')");
				}
				newArgs.add(ws);
			}
			if (props != null) {
				newArgs = loadArgumentsInPropertiesFiles(newArgs, props);
			}
			args = newArgs.toArray(new String[newArgs.size()]);
		}
		StringBuilder sb = new StringBuilder("Executing director with");
		for (String a : args) {
			sb.append(" ");
			sb.append(a);
		}
		System.err.println(sb);
		super.processArguments(args);
	}
	
	/**
	 * 
	 * @param args
	 * @param props
	 * @return
	 */
	private ArrayList<String> loadArgumentsInPropertiesFiles(ArrayList<String> args, Map<String,String> props) 
	throws CoreException {
		//put the args on the command line in a map.
		LinkedHashMap<String, String> cmdArgs = new LinkedHashMap<String, String>();
		for (int i = 0; i < args.size(); i++) {
			String opt = args.get(i);
			if (opt.startsWith("-")) {
				if (i+1 < args.size()) {
					String v = args.get(i+1);
					if (v.startsWith("-")) {
						cmdArgs.put(opt, "");
					} else {
						cmdArgs.put(opt, v);
					}
				}
			}
		}
		//the props that are overriding the command-line
		for (Entry<String,String> entry : props.entrySet()) {
			String propsArg = entry.getKey();
			String uris = entry.getValue();
			int num = parseNumber(propsArg);
			if (num > 10) {
				Properties propsLoaded = loadProperties(uris);
				for (Object key : propsLoaded.keySet()) {
					String keyStr = (String)key;
					String value = propsLoaded.getProperty(keyStr);
					cmdArgs.put(keyStr, value);
				}
			}
		}
		//the props that are not overriding the command-line
		for (Entry<String,String> entry : props.entrySet()) {
			String propsArg = entry.getKey();
			String uris = entry.getValue();
			int num = parseNumber(propsArg);
			if (num <= 10) {
				Properties propsLoaded = loadProperties(uris);
				for (Object key : propsLoaded.keySet()) {
					String keyStr = (String)key;
					String value = propsLoaded.getProperty(keyStr);
					if (!cmdArgs.containsKey(keyStr)) {
						cmdArgs.put(keyStr, value);
					}
				}
			}
		}
		args = new ArrayList<String>(cmdArgs.size()*2);
		for (Entry<String,String> en : cmdArgs.entrySet()) {
			args.add(en.getKey());
			if (en.getValue().length() != 0) {
				args.add(resolvePropertyValue(en.getValue()));
			}
		}
		return args;
	}
	
	private int parseNumber(String propsArgName) {
		if (propsArgName.length() > "-props".length()) {
			try {
				String suffix = propsArgName.substring("-props".length());
				int num = Integer.parseInt(suffix);
				return num;
			} catch (NumberFormatException nfe) {
				
			}
		}
		return 0;
	}
	
	private Properties loadProperties(String uri) throws CoreException {
		Properties p = new Properties();
		InputStream inStream = null;
		try {
			URI ur = new URI(uri.startsWith("/") ? ("file:" + uri) : uri);
			if (!ur.isAbsolute()) {
				ur = ur.resolve(new File(".").toURI());
			}
			inStream = ur.toURL().openStream();
			p.load(inStream);
		} catch (Throwable t) {
			throw new ProvisionException("Invalid uri '" + uri + "'. " +
					"Expecting either an absolute file path or an absolute uri", t);
		} finally {
			if (inStream != null) try { inStream.close(); } catch (IOException ioe) {}
		}
		return p;
	}
	
	/**
	 * Substitute the ${sysprop} by their actual system property.
	 * ${sysprop,defaultvalue} will use 'defaultvalue' as the value if no sysprop is defined.
	 * Not the most efficient code but we are shooting for simplicity and speed of devlopement here.
	 * 
	 * @param value
	 * @return
	 */
	private static String resolvePropertyValue(String value) {
		
		int ind = value.indexOf("${");
		if (ind == -1) {
			return value;
		}
		int ind2 = value.indexOf('}', ind);
		if (ind2 == -1) {
			return value;
		}
		String sysprop = value.substring(ind+2, ind2);
		String defaultValue = null;
		int comma = sysprop.indexOf(',');
		if (comma != -1 && comma+1 != sysprop.length()) {
			defaultValue = sysprop.substring(comma+1);
			defaultValue = resolvePropertyValue(defaultValue);
			sysprop = sysprop.substring(0,comma);
		} else {
			defaultValue = "${" + sysprop + "}";
		}
		
		String v = System.getProperty(sysprop);
		String reminder = value.length() > ind2 + 1 ? value.substring(ind2+1) : "";
		reminder = resolvePropertyValue(reminder);
		if (v != null) {
			return value.substring(0, ind) + v + reminder;
		} else {
			return value.substring(0, ind) + defaultValue + reminder;
		}
	}
		
}
