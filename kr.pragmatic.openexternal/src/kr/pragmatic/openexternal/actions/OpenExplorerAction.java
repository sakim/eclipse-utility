/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.pragmatic.openexternal.OpenExternalPlugin;
import kr.pragmatic.openexternal.preferences.PreferenceIds;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;


/**
 * Open Windows Explorer
 * 
 * @author sakim
 *
 */
public class OpenExplorerAction extends AbstractOpenAction {

	private static final String EXPLORER = "explorer";
	private static final String FOLDERS_OPTION = "/e,";
	private static final String SELECT_OPTION = "/select,";

	public OpenExplorerAction() {
	}
	
	@Override
	public void run(IAction action) {
		@SuppressWarnings("unchecked")
		List<Object> list = getSelection().toList();
		Set<String> opendPathSet = new HashSet<String>();

		List<String> cmdList = new ArrayList<String>();
		cmdList.add(EXPLORER);
		cmdList.add(SELECT_OPTION);
		if (openWithFolders())
			cmdList.add(FOLDERS_OPTION);
		
		for (Object resource : list) {
			String path = getPath(resource);
			
			if (path == null)
				continue;
			
			try {
				cmdList.add(path);
				
				if (!opendPathSet.contains(path)) {
					new ProcessBuilder(cmdList).start();
					opendPathSet.add(path);
				}
			} catch (IOException exception) {
				ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, 
						OpenExternalPlugin.ID, IStatus.ERROR, "Cannot open the Windows Explorer.", exception));
			} finally {
				cmdList.remove(path);
			}
		}
	}
	
	private boolean openWithFolders() {
		String policy = OpenExternalPlugin.getDefaultPreferenceStore().getString(PreferenceIds.WINDOWS_EXPLORER_POLICY);

		return policy.equals(PreferenceIds.WINDOWS_EXPLORER_FOLDERS);
	}

}
