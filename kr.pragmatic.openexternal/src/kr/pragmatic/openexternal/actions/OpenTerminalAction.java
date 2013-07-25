/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
 * Open terminal app on mac
 * 
 * @author sakim
 *
 */
public class OpenTerminalAction extends AbstractOpenAction {

	public OpenTerminalAction() {
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
					File script = writeScript(path, openInNewTab());
					Process process = new ProcessBuilder("/usr/bin/osascript", script.getAbsolutePath()).start();
					int exitCode = process.waitFor();
					
					if (exitCode != 0)
						throw new RuntimeException();
					script.delete();

					opendPathSet.add(path);
				}
			} catch (Exception exception) {
				exception.printStackTrace();
				ErrorDialog.openError(getShell(), null, null, new Status(
						IStatus.ERROR, OpenExternalPlugin.ID, IStatus.ERROR,
						"Cannot open the Terminal.", exception));
			}
		}
	}
	
	/**
	 * Write applescript file to open terminal.
	 * 
	 * @param terminalPath
	 * @param useTab
	 * @return 
	 * @throws IOException 
	 * 					Occurred when fail to script file to user temp directory
	 */
	private File writeScript(String terminalPath, boolean useTab) throws IOException {
		File file = new File(System.getProperty("java.io.tmpdir"), "eclipse.applescript");
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter(new FileWriter(file));
			
			String script = "	do script \"cd '" + terminalPath + "';/usr/bin/clear\"";
			
			writer.println("tell application \"System Events\"");
			writer.println("	set windowCount to count(processes whose name is \"Terminal\")");
			writer.println("end tell");
			
			writer.println("tell application \"Terminal\"");
			writer.println("	activate");
	
			if (useTab) {
				writer.println("    if windowCount is greater than 0 then");
				writer.println("        tell application \"System Events\" to tell process \"Terminal\" to keystroke \"t\" using command down");
				writer.println("    end if");
				writer.println(script + " in window 1");
			} else {
				writer.println("    if windowCount is equal to 0 then");
				writer.println(script + " in window 1");
				writer.println("    else");
				writer.println(script);
				writer.println("    end if");
			}
			writer.println("end tell");
		} finally {
			if (writer != null)
				writer.close();
		}
		
		return file;
	}
	
	/**
	 * whether open in a tab or a window
	 * 
	 * @return   
	 */
	private boolean openInNewTab() {
		String policy = OpenExternalPlugin.getDefaultPreferenceStore().getString(PreferenceIds.MAC_TERMINAL_POLICY);
		return policy.equals(PreferenceIds.MAC_TERMINAL_NEW_TAB);
	}
}