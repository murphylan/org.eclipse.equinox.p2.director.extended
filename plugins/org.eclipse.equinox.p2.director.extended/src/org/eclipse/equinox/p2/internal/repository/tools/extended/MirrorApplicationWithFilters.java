/**
 * Copyright (c) 2011, Intalio Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intalio Inc. - initial API and implementation
 */
package org.eclipse.equinox.p2.internal.repository.tools.extended;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.eclipse.equinox.p2.director.extended.ArgumentsLoader;
import org.eclipse.equinox.p2.internal.repository.mirroring.Mirroring;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.ExpressionQuery;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Extends the p2 mirror application and the mirroring itself to filter the artifacts that are mirrored.
 * Use a naive synthax to define exclusions.
 * <p>
 * Extends the MirrorApplication to support partial mirroring just like in the ant task.
 * The IUs to exclude from the mirror are selected from the -exclude argument(s)
 * to be a p2 SimplePattern (as documented here: http://wiki.eclipse.org/Query_Language_for_p2).
 * It will be matched against the artifact ids that are mirrored.
 * </p>
 * <p>
 * The union of all the p2-query defined from those arguments is the resulting
 * query that selects the artifacts to exclude from the mirror.
 * </p>
 * 
 * @author hmalphettes
 *
 */
public class MirrorApplicationWithFilters extends MirrorApplicationForked {

	/**
	 * @see QueryUtil#createIUQuery
	 */
	//private static final IExpression regexpMatchIU_ID = ExpressionUtil.parse("artifactKey.id ~= $0");
	private static IExpression simplePatternAgainstArtifactID(boolean isArtifactDescriptor, String... simplePatterns) {
		StringBuilder sb = new StringBuilder();
		String member = isArtifactDescriptor ? "artifactKey.id" : "id";
		for (String simplePattern : simplePatterns) {
			if (sb.length() != 0) {
				sb.append(" || ");
			}
			sb.append(member + " ~= /" + simplePattern + "/");
		}
		return ExpressionUtil.parse(sb.toString());
	}

	public static final IQuery<IArtifactDescriptor> NO_UNITS =
		new ExpressionQuery<IArtifactDescriptor>(IArtifactDescriptor.class, "limit(0)");

	protected IQuery<IArtifactDescriptor> excludingQuery;
	protected IQuery<IArtifactKey> includingQuery;
	
	/**
	 * Support for the -exclude argument
	 */
	@Override
	public void initializeFromArguments(String[] args) throws Exception {
		
		ArrayList<String> newArgs = ArgumentsLoader.loadArgumentsInPropertiesFiles(args);
		LinkedList<String> excludes = new LinkedList<String>();
		LinkedList<String> includes = new LinkedList<String>();
		ListIterator<String> it = newArgs.listIterator();
		while (it.hasNext()) {
			String arg = it.next();
			if ((arg.equals("-exclude") || arg.equals("-include")) && it.hasNext()) {
				boolean isExclude = arg.equals("-exclude");
				it.remove();
				String filter = it.next();
				it.remove();
				List<String> filters = processIncludeExclude(filter);
				if (filters != null) {
					if (isExclude) {
						excludes.addAll(filters);
					} else {
						includes.addAll(filters);
					}
				}
			}
		}
		args = newArgs.toArray(new String[newArgs.size()]);
		
		super.initializeFromArguments(args);
		if (excludes.size() != 0) {
			excludingQuery = createExcludingQuery(
				excludes.toArray(new String[excludes.size()]));
		}
		if (includes.size() != 0) {
			includingQuery = createIncludingQuery(
				includes.toArray(new String[includes.size()]));
		}
		StringBuilder sb = new StringBuilder("Executing mirror with");
		for (String a : args) {
			sb.append(" ");
			sb.append(a);
		}
		for (String excl : excludes) {
			sb.append(" -exclude " + excl);
		}
		for (String incl : includes) {
			sb.append(" -include " + incl);
		}
		System.err.println(sb);

	}
	
	/**
	 * @param value
	 * @return The same value stripped of the enclosing quotes if there were such thing.
	 */
	private static List<String> processIncludeExclude(String value) {
		value = value.trim();
		if ((value.startsWith("\"") || value.startsWith("'"))
				&& (value.endsWith("\"") || value.endsWith("'"))) {
			value = value.substring(1,value.length()-2);
		}
		if (value.length() == 0) {
			return null;
		}
		List<String> res = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(value, "\n\t\r \"'", false);
		while (tokenizer.hasMoreElements()) {
			res.add(tokenizer.nextToken());
		}
		return res;
	}
	
	/**
	 * Creates a query that will match any {@link IInstallableUnit} if its id matches
	 * the passed regexp.
	 * Also matches the version if no version is passed then any version will do.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match nothing
	 * @return a query that matches IU's by id
	 */
	public static IQuery<IArtifactDescriptor> createExcludingQuery(String... idRegexp) {
		return idRegexp == null ? NO_UNITS
				: new ExpressionMatchQuery<IArtifactDescriptor>(
						IArtifactDescriptor.class, simplePatternAgainstArtifactID(true, idRegexp));
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} if its id matches
	 * the passed regexp.
	 * Also matches the version if no version is passed then any version will do.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match nothing
	 * @return a query that matches IU's by id
	 */
	public static IQuery<IArtifactKey> createIncludingQuery(String... simplePatterns) {
		return new ExpressionMatchQuery<IArtifactKey>(
						IArtifactKey.class, simplePatternAgainstArtifactID(false, simplePatterns));
	}

	/**
	 * Take into account the exclusions:
	 * 
	 */
	@Override
	protected void internalSetMirroring(Mirroring mirror,
			ArrayList<IArtifactKey> keys) {
		if (excludingQuery == null && includingQuery == null) {
			super.internalSetMirroring(mirror, keys);
			return;
		}
		if (keys == null || keys.size() == 0) {
			//if no keys were passed then select all.
			IQueryResult<IArtifactKey> result = getCompositeArtifactRepository()
					.query(includingQuery == null 
							? ArtifactKeyQuery.ALL_KEYS
							: includingQuery, null);
			keys = new ArrayList<IArtifactKey>(result.toSet());
		}
		
		//descriptor queryable must be called after the artifact keys have been queried
		//otherwise nothing will be there.
		IQueryResult<IArtifactDescriptor> exclusions = getCompositeArtifactRepository()
			.descriptorQueryable().query(excludingQuery, null);

		for (IArtifactDescriptor d : exclusions.toSet()) {
			//this could be optimized although it probably won't make a big difference.
			ListIterator<IArtifactKey> kIt = keys.listIterator();
			while (kIt.hasNext()) {
				IArtifactKey kI = kIt.next();
				if (kI.getId().equals(d.getArtifactKey().getId())) {
					kIt.remove();
					break;
				}
			}
		}
		super.internalSetMirroring(mirror, keys);
	}
	
	
	
}
