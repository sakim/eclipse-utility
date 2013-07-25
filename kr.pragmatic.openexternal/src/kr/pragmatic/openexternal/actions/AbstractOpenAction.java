/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.actions;

import kr.pragmatic.openexternal.OpenExternalPlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;


public abstract class AbstractOpenAction extends ActionDelegate implements IObjectActionDelegate {

	private IStructuredSelection selection;
	private IWorkbenchPart targetPart;

	public AbstractOpenAction() {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}

	public IStructuredSelection getSelection() {
		return selection;
	}
	
	protected Shell getShell() {
		if (targetPart != null)
			return targetPart.getSite().getShell();
		return OpenExternalPlugin.getActivePage().getActivePart().getSite().getShell();
	}
	
	/**
	 * @param object 
	 * 					a target resource
	 * @param directory 
	 * 					if object is a file, then return its parent path.
	 * @return
	 */
	protected String getPath(Object object, boolean directory) {
		
		if (object instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) object).getAdapter(IResource.class);
			if (resource == null)
				return null;
			
			if (directory) {
				if (resource.getType() == IResource.FILE)
					return resource.getParent().getLocation().toOSString();
			}

			return resource.getLocation().toOSString();
		}
		
		return null;
	}
	
	protected String getPath(Object object) {
		return getPath(object, false);
	}
}
