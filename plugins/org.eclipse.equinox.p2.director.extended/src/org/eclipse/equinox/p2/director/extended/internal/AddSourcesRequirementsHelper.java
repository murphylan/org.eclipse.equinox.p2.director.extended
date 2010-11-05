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
package org.eclipse.equinox.p2.director.extended.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.eclipse.equinox.p2.director.extended.DirectorApplication;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

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

    /**
     * Review the available IUs.
     * Change the current IProfileChangeRequest.
     * @param availableIUs
     * @return true if something changed.
     */
    public static Set<IRequirement> reviewAvailableIInstallableUnits(IInstallableUnit[] availableIUs,
            IQueryable<IInstallableUnit> toInstallIUs, Set<IInstallableUnit> allIUsToInstallCollector) {
        Set<IRequirement> sourceRequirements = new HashSet<IRequirement>();
        
        List<IInstallableUnit> runtimeBundles = new LinkedList<IInstallableUnit>();
        Map<String,Map<String,IInstallableUnit>> sourceBundles = new HashMap<String, Map<String,IInstallableUnit>>();
        for (IInstallableUnit iu : availableIUs) {
            IArtifactKey aKey = getBundleArtifactKey(iu);
            if (aKey == null) {
                continue;
            }
            if (aKey.getId().endsWith(SOURCE_SUFFIX)) {
            	String runtimeId = aKey.getId().substring(0, aKey.getId().length() - SOURCE_SUFFIX.length());
            	Map<String,IInstallableUnit> ius = sourceBundles.get(runtimeId);
            	if (ius == null) {
            		ius = new HashMap<String, IInstallableUnit>();
            	}
            	ius.put(String.valueOf(iu.getVersion().getOriginal()), iu);
            	sourceBundles.put(runtimeId, ius);
            } else {
                runtimeBundles.add(iu);
            }
        }
        Iterator<IInstallableUnit> toBeInstalledIt = toInstallIUs.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).iterator();
        int runBundlesCounter = 0;
        int srcBundlesCounter = 0;
        while (toBeInstalledIt.hasNext()) {
            runBundlesCounter++;
            IInstallableUnit runtimeBundleIU = toBeInstalledIt.next();
            allIUsToInstallCollector.add(runtimeBundleIU);
            Map<String,IInstallableUnit> srcIUs = sourceBundles.get(runtimeBundleIU.getId());
            if (srcIUs == null) {
            	continue;
            }
            IInstallableUnit sourceBundle = srcIUs.get(String.valueOf(runtimeBundleIU.getVersion().getOriginal()));
            if (sourceBundle == null) {
            	continue;
            }
            //create a new optional requirement for an osgi.bundle with the same version range, and the same
            //same id suffixed by .source
            if (sourceBundle != null) {
                srcBundlesCounter++;
                allIUsToInstallCollector.add(sourceBundle);
                IRequirement source = createOptionalSourceRequirement(runtimeBundleIU, sourceBundle);
                sourceRequirements.add(source);
            }
        }
        Activator.getFrameworkLog().log(new FrameworkLogEntry(DirectorApplication.APP_ID, 
                FrameworkLogEntry.WARNING, 0,
                ".. Installing " + runBundlesCounter + " runtime bundles and " + srcBundlesCounter + " source bundles.", 0, null, null));
        System.err.println(".. Installing " + runBundlesCounter + " runtime bundles and " + srcBundlesCounter + " source bundles.");
        return sourceRequirements;
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

    private static IRequirement createOptionalSourceRequirement(IInstallableUnit runtimeIU, IInstallableUnit sourceIU) {
        //debug: make one of the source req mandatory.
        boolean mandatory = false;//iu.getId().equals("org.eclipse.jetty.server") && iuIdSuffix.equals(SOURCE_SUFFIX);
        
        return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, sourceIU.getId(),
                new VersionRange(runtimeIU.getVersion(), true, runtimeIU.getVersion(), true), null,
                !mandatory, //optional
                false,
                true);
    }

}
