/******************************************************************************* 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package kr.pragmatic.openexternal.preferences;

import kr.pragmatic.openexternal.OpenExternalPlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = OpenExternalPlugin.getDefaultPreferenceStore();
		store.setDefault(PreferenceIds.WINDOWS_EXPLORER_POLICY, PreferenceIds.WINDOWS_EXPLORER_FOLDERS);
		store.setDefault(PreferenceIds.MAC_TERMINAL_POLICY, PreferenceIds.MAC_TERMINAL_NEW_SHELL);
	}
}
