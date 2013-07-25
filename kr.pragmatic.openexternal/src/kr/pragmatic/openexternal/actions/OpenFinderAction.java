/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.actions;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.pragmatic.openexternal.OpenExternalPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;


/**
 * Open finder (file browser) on mac
 * 
 * @author sakim
 *
 */
public class OpenFinderAction extends AbstractOpenAction {

	private static final String CMD = "open";

	public OpenFinderAction() {
	}

	@Override
	public void run(IAction action) {
		@SuppressWarnings("unchecked")
		List<Object> list = getSelection().toList();
		Set<String> opendPathSet = new HashSet<String>();

		for (Object resource : list) {
			String path = getPath(resource, true);
			
			if (path == null)
				continue;
			
			try {
				if (!opendPathSet.contains(path)) {
					new ProcessBuilder(CMD, path).start();
					opendPathSet.add(path);
				}
			} catch (IOException exception) {
				ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, 
						OpenExternalPlugin.ID, 
						IStatus.ERROR, "Cannot open the Finder.", exception));
			}
		}
	}

}
