/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.preferences;

import kr.pragmatic.openexternal.OpenExternalPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class OpenExternalPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public OpenExternalPreferencePage() {
		super(GRID);
		setPreferenceStore(OpenExternalPlugin.getDefaultPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			addField(new RadioGroupFieldEditor(
					PreferenceIds.WINDOWS_EXPLORER_POLICY,
					"Windows Explorer Policy:", 1, new String[][] {
							{ "Open with folders", PreferenceIds.WINDOWS_EXPLORER_FOLDERS },
							{ "Open without folders", PreferenceIds.WINDOWS_EXPLORER_NO_FOLDERS }},
							getFieldEditorParent(), true));
		}
		
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			addField(new RadioGroupFieldEditor(
					PreferenceIds.MAC_TERMINAL_POLICY,
					"Mac OS X Shell Policy:", 1, new String[][] {
							{ "Open in a new shell", PreferenceIds.MAC_TERMINAL_NEW_SHELL },
							{ "Open in a new tab", PreferenceIds.MAC_TERMINAL_NEW_TAB }},
							getFieldEditorParent(), true));
		}
	}

	public void init(IWorkbench workbench) {
	}
}
