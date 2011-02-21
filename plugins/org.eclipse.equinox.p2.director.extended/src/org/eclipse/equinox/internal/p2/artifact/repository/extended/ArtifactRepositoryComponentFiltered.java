package org.eclipse.equinox.internal.p2.artifact.repository.extended;

import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

/**
 * Factory for the ArtifactRepositoryManagerFiltered
 * 
 * @author hmalphettes
 */
public class ArtifactRepositoryComponentFiltered implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		return new ArtifactRepositoryManagerFiltered(agent);
	}

}
