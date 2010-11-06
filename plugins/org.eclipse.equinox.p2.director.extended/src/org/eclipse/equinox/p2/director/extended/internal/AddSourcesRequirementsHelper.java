/**
 * Copyright (c) 2010, EclipseSource Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     EclipseSource - initial API and implementation
 */
package org.eclipse.equinox.p2.director.extended.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * Generate a set of optional requirement for the source bundles.
 * Use naming conventions of the symbolic name to point to the source bundle.
 * All source bundles are marked optional.
 * 
 * @author Jeff McAfer
 * @author hmalphettes
 */
public class AddSourcesRequirementsHelper {
//implements IProfileChangeRequestRequirementsReviewer {

    //copied from PDE contributed by EclipseSOurce https://bugs.eclipse.org/bugs/show_bug.cgi?id=328929
	/**
	 * Constant ID for a root installable unit that is installed into the profile if {@link #fIncludeSource} is set
	 * to <code>true</code>.  The source units found in the repository will be set as required IUs on the root unit.
	 */
	private static final String SOURCE_IU_ID = "org.eclipse.pde.core.target.source.bundles"; //$NON-NLS-1$

    
	/** run a second pass of the planner to add in the source bundles for everything that's
	 * in the current profile.
	 */
	public static IProvisioningPlan planInSourceBundles(IProfile profile, ProvisioningContext context, IProgressMonitor monitor,
			IProfileRegistry profileRegistry, IEngine engine, IPlanner planner) throws CoreException {
//		if (!fIncludeSource)
//			return;

		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(/*Messages.IUBundleContainer_ProvisioningSourceBundles*/"Provisioning source bundles", 200);

		// create an IU that optionally and greedily requires the related source bundles.
		// Completely replace any source IU that may already be in place
		IInstallableUnit currentSourceIU = getCurrentSourceIU(profile);

		// determine the new version number.  start at 1
		Version sourceVersion = Version.createOSGi(1, 0, 0);
		if (currentSourceIU != null) {
			Integer major = (Integer) currentSourceIU.getVersion().getSegment(0);
			sourceVersion = Version.createOSGi(major.intValue() + 1, 0, 0);
		}
		IInstallableUnit sourceIU = createSourceIU(profile, sourceVersion);

		// call the planner again to add in the new source IU and all available source bundles
//		IPlanner planner = P2TargetUtils.getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		if (currentSourceIU != null)
			request.remove(currentSourceIU);
		request.add(sourceIU);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new SubProgressMonitor(subMonitor, 20));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		if (subMonitor.isCanceled()) {
			return null;
		}
		return plan;
		/*
		long oldTimestamp = profile.getTimestamp();

		// execute the provisioning plan
		IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE, PhaseSetFactory.PHASE_UNINSTALL});
//		IEngine engine = P2TargetUtils.getEngine();
//		plan.setProfileProperty(P2TargetUtils.PROP_PROVISION_MODE, TargetDefinitionPersistenceHelper.MODE_PLANNER);
//		plan.setProfileProperty(P2TargetUtils.PROP_ALL_ENVIRONMENTS, Boolean.toString(false));
		IStatus result = engine.perform(plan, phases, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return null;
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// remove the old (intermediate) profile version now we have a new one with source.
		profileRegistry.removeProfile(profile.getProfileId(), oldTimestamp);
		subMonitor.worked(10);
		subMonitor.done();*/
	}


	// Create and return an IU that has optional and greedy requirements on all source bundles
	// related to bundle IUs in the given queryable. 
	/**
	 * Creates and returns an IU that has optional and greedy requirements on all source bundles
	 * related to bundle IUs in the given queryable.
	 * @param queryable location to search for source bundle IUs
	 * @param iuVersion version to set on the returned installable unit
	 * @return a new installable unit with requirements on the available source IUs
	 */
	private static IInstallableUnit createSourceIU(IQueryable queryable, Version iuVersion) {
		// compute the set of source bundles we could possibly need for the bundles in the profile
		IRequirement bundleRequirement = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false, false); //$NON-NLS-1$ //$NON-NLS-2$
		IQueryResult profileIUs = queryable.query(QueryUtil.createIUAnyQuery(), null);
		ArrayList requirements = new ArrayList();
		for (Iterator i = profileIUs.iterator(); i.hasNext();) {
			IInstallableUnit profileIU = (IInstallableUnit) i.next();
			if (profileIU.satisfies(bundleRequirement)) {
				String id = profileIU.getId() + ".source"; //$NON-NLS-1$
				Version version = profileIU.getVersion();
				VersionRange range = new VersionRange(version, true, version, true);
				IRequirement sourceRequirement = MetadataFactory.createRequirement("osgi.bundle", id, range, null, true, false, true); //$NON-NLS-1$
				requirements.add(sourceRequirement);
			}
		}

		InstallableUnitDescription sourceDescription = new MetadataFactory.InstallableUnitDescription();
		sourceDescription.setSingleton(true);
		sourceDescription.setId(SOURCE_IU_ID);
		sourceDescription.setVersion(iuVersion);
		sourceDescription.addRequirements(requirements);
		IProvidedCapability capability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, SOURCE_IU_ID, iuVersion);
		sourceDescription.setCapabilities(new IProvidedCapability[] {capability});
		return MetadataFactory.createInstallableUnit(sourceDescription);
	}

	/**
	 * Lookup and return the source IU in the given queryable or <code>null</code> if not found.
	 * @param queryable location to look for source IUs
	 * @return the source IU or <code>null</code>
	 */
	private static IInstallableUnit getCurrentSourceIU(IQueryable queryable) {
		IQuery query = QueryUtil.createIUQuery(SOURCE_IU_ID);
		IQueryResult list = queryable.query(query, null);
		IInstallableUnit currentSourceIU = null;
		if (!list.isEmpty())
			currentSourceIU = (IInstallableUnit) list.iterator().next();
		return currentSourceIU;
	}

}
