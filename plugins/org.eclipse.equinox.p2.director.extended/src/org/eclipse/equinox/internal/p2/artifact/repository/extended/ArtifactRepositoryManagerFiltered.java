package org.eclipse.equinox.internal.p2.artifact.repository.extended;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;

/**
 * Ability to filter out child repositories based on their URIs.
 * Useful during mirror operations where we want to not mirror an entire p2 repository.
 * 
 * @author hmalphettes
 */
public class ArtifactRepositoryManagerFiltered extends ArtifactRepositoryManager {

	public ArtifactRepositoryManagerFiltered(IProvisioningAgent agent) {
		super(agent);
	}

	@Override
	public void addRepository(IArtifactRepository repository) {
		if (!isFiltered(repository)) {
			super.addRepository(repository);
		}
	}
	
	/**
	 * @param repository
	 * @return true if we want to disregard this repository.
	 */
	public boolean isFiltered(IArtifactRepository repository) {
		URI uri = repository.getLocation();
		System.err.println("IsFiltered called for " + uri.toString());
		return false;
	}

	@Override
	protected IRepository<IArtifactKey> factoryCreate(URI location,
			String name, String type, Map<String, String> properties,
			IExtension extension) throws ProvisionException {
		System.err.println("factoryCreate called for " + location.toString() + " type " + type);
		return super.factoryCreate(location, name, type, properties, extension);
	}
	
	/**
	 * Loads and returns a repository using the given repository factory extension. Returns
	 * null if no factory could be found associated with that extension.
	 */
	protected IRepository<IArtifactKey> factoryLoad(URI location, IExtension extension,
			int flags, SubMonitor monitor) throws ProvisionException {
		System.err.println("factoryLoad called for " + location.toString() + " extension " + extension);
		return super.factoryLoad(location, extension, flags, monitor);
	}


}
