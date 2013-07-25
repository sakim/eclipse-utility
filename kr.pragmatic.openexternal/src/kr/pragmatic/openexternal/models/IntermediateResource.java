/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.models;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IActionFilter;

public class IntermediateResource implements IAdaptable, IActionFilter {
	private IResource resource;
	
	public IntermediateResource(IResource resource) {
		this.resource = resource;
	}
	
	public IResource getResource() {
		return resource;
	}

	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("platform")) {
			return Platform.getOS().equals(value);
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		
		return getResource().getAdapter(adapter);
	}
}
