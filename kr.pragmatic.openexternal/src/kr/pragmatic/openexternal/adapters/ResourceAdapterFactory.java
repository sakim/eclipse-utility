/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.adapters;

import kr.pragmatic.openexternal.models.IntermediateResource;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;


public class ResourceAdapterFactory implements IAdapterFactory {
	@SuppressWarnings("unchecked")
	private static Class[] TYPES = new Class[] { IntermediateResource.class };
	
	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		IResource resource = null;
		
		if (adaptableObject instanceof IAdaptable) {
			resource = (IResource) 
				((IAdaptable) adaptableObject).getAdapter(IResource.class);
		}
		
		if (IntermediateResource.class.equals(adapterType) && resource != null) {
			return new IntermediateResource(resource);
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return TYPES ;
	}
}
