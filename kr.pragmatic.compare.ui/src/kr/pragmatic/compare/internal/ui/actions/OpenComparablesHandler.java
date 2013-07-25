package kr.pragmatic.compare.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import kr.pragmatic.compare.internal.ui.dialogs.OpenComparablesSelectionDialog;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author sakim
 *
 */
public class OpenComparablesHandler extends TeamAction {
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		IResource[] selectedResources = queryFileResource();

		ITypedElement ancestor = null;
		ITypedElement left = null;
		ITypedElement right = null;

		if (selectedResources.length == 2) {
			if (selectedResources[0] != null)
				left = getElementFor(selectedResources[0]);

			if (selectedResources[1] != null)
				right = getElementFor(selectedResources[1]);

		} else if (selectedResources.length == 3) {
			// prompt for ancestor
			SelectAncestorDialog dialog = new SelectAncestorDialog(getShell(),
					selectedResources);
			int code = dialog.open();
			if (code == Window.CANCEL)
				return;

			ancestor = getElementFor(dialog.ancestorResource);
			left = getElementFor(dialog.leftResource);
			right = getElementFor(dialog.rightResource);
		} else {
			return;
		}
		openInCompare(ancestor, left, right);
	}
	
	/**
	 * Query the user for the resources that should be opened
	 * 
	 * @return the resource that should be opened.
	 */
	private final IFile[] queryFileResource() {
		final IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		final Shell parent = window.getShell();
		final IContainer input = ResourcesPlugin.getWorkspace().getRoot();
		final OpenComparablesSelectionDialog dialog = 
			new OpenComparablesSelectionDialog(parent, input);
		
		final int resultCode = dialog.open();
		if (resultCode != Window.OK) {
			return null;
		}

		final Object[] result = dialog.getResult();
		List<IFile> resultToReturn = new ArrayList<IFile>();
		
		for (int i = 0; i < result.length; i++) {
			if (result[i] instanceof IFile) {
				resultToReturn.add((IFile) result[i]);
			}
		}
		
		IFile[] files = new IFile[resultToReturn.size()]; 
		files = resultToReturn.toArray(files);

		return files;
	}

	private void openInCompare(ITypedElement ancestor, ITypedElement left,
			ITypedElement right) {
		IWorkbenchPage workBenchPage = getTargetPage();
		CompareEditorInput input = new SaveablesCompareEditorInput(ancestor,
				left, right, workBenchPage);
		IEditorPart editor = CompareRevisionAction.findReusableCompareEditor(
				input, workBenchPage);
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				workBenchPage.activate(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				workBenchPage.activate(editor);
			}
		} else {
			CompareUI.openCompareEditor(input);
		}
	}

	public boolean isEnabled() {
		return true;
	}

	private ITypedElement getElementFor(IResource resource) {
		return SaveablesCompareEditorInput.createFileElement((IFile) resource);
	}

	// see
	// org.eclipse.compare.internal.ResourceCompareInput.SelectAncestorDialog
	private class SelectAncestorDialog extends MessageDialog {
		private IResource[] theResources;
		IResource ancestorResource;
		IResource leftResource;
		IResource rightResource;

		private Button[] buttons;

		public SelectAncestorDialog(Shell parentShell, IResource[] theResources) {
			super(parentShell, TeamUIMessages.SelectAncestorDialog_title, null,
					TeamUIMessages.SelectAncestorDialog_message,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.theResources = theResources;
		}

		protected Control createCustomArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			buttons = new Button[3];
			for (int i = 0; i < 3; i++) {
				buttons[i] = new Button(composite, SWT.RADIO);
				buttons[i].addSelectionListener(selectionListener);
				buttons[i].setText(NLS.bind(
						TeamUIMessages.SelectAncestorDialog_option,
						theResources[i].getFullPath().toPortableString()));
				buttons[i].setFont(parent.getFont());
				// set initial state
				buttons[i].setSelection(i == 0);
			}
			pickAncestor(0);
			return composite;
		}

		private void pickAncestor(int i) {
			ancestorResource = theResources[i];
			leftResource = theResources[i == 0 ? 1 : 0];
			rightResource = theResources[i == 2 ? 1 : 2];
		}

		private SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button selectedButton = (Button) e.widget;
				if (!selectedButton.getSelection())
					return;
				for (int i = 0; i < 3; i++)
					if (selectedButton == buttons[i])
						pickAncestor(i);
			}
		};
	}
}