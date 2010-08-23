/*
 * Copyright (c) 2010, Intalio Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intalio Inc. - initial API and implementation
 */
package org.eclipse.equinox.p2.director.extended.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;

/**
 * Generate a set of optional requirement for the source bundles.
 * Use naming conventions of the symbolic name to point to the source bundle.
 * All source bundles are marked optional.
 * 
 * @author hmalphettes
 */
public class AddSourcesRequirementsHelper {
//implements IProfileChangeRequestRequirementsReviewer {

	private static final String SOURCE_SUFFIX = ".source";
	private static final String SOURCES_SUFFIX = ".sources";

	/**
	 * Review the available IUs.
	 * Change the current IProfileChangeRequest.
	 * @param availableIUs
	 * @return true if something changed.
	 */
	public static Set<IRequirement> reviewAvailableIInstallableUnits(IInstallableUnit[] availableIUs) {
		Set<IRequirement> sourceRequirements = new HashSet<IRequirement>();
		for (IInstallableUnit iu : availableIUs) {
			addSourceBundleRequiredCapability(sourceRequirements, iu);
		}
		return sourceRequirements;
	}
	
	/**
	 * If the iu is a runtime osgi.bundle (name does not end with .source or .sources)
	 * then add a new optional required capability that points to the source bundle
	 * that might be present.
	 * 
	 * @param iu
	 */
	private static void addSourceBundleRequiredCapability(Set<IRequirement> sourceRequirements, IInstallableUnit iu) {
		IArtifactKey aKey = getBundleArtifactKey(iu);
		if (aKey == null) {
			return;
		}
		if (aKey.getId().endsWith(SOURCE_SUFFIX) || aKey.getId().endsWith(SOURCES_SUFFIX)) {
			return;
		}
		//create a new optional requirement for an osgi.bundle with the same version range, and the same
		//same id suffixed by .source or .sources.
		IRequirement source = createOptionalSourceRequirement(iu, SOURCE_SUFFIX);
		IRequirement sources = createOptionalSourceRequirement(iu, SOURCES_SUFFIX);
		sourceRequirements.add(source);
		sourceRequirements.add(sources);
	}
	
	/**
	 * @param iu
	 * @return null if this iu is not a bundle or the (first) artifact key that describes it as a bundle.
	 */
	private static IArtifactKey getBundleArtifactKey(IInstallableUnit iu) {
		Iterator<IArtifactKey> it = iu.getArtifacts().iterator();
		while (it.hasNext()) {
			IArtifactKey key = it.next();
			if ("osgi.bundle".equals(key.getClassifier())) {
				return key;
			}
		}
		return null;
	}

	private static IRequirement createOptionalSourceRequirement(IInstallableUnit iu, String iuIdSuffix) {
		//debug: make one of the source req mandatory.
		boolean mandatory = false;//iu.getId().equals("org.eclipse.jetty.server") && iuIdSuffix.equals(SOURCE_SUFFIX);
		
		return MetadataFactory.createRequirement(/*"osgi.bundle"*/IInstallableUnit.NAMESPACE_IU_ID, iu.getId() + iuIdSuffix,
				new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null,
				!mandatory, //optional
				false,
				true);
	}

}
