package kr.pragmatic.compare.internal.ui.dialogs;

import kr.pragmatic.compare.internal.ui.CompareUIMessages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

public class OpenComparablesSelectionDialog extends FilteredResourcesSelectionDialog {

	public OpenComparablesSelectionDialog(Shell shell, IContainer container) {
		super(shell, true, container, IResource.FILE);
		setTitle(CompareUIMessages.OpenComparablesAction_dialogTitle);
		setMessage(CompareUIMessages.OpenComparablesAction_dialogMessage);
	}
	
	@Override
	protected void updateButtonsEnableState(IStatus status) {
		Button okButton = getOkButton();
		if (okButton != null && !okButton.isDisposed()) {
			okButton.setEnabled(!status.matches(IStatus.ERROR) && getSelectedItems().size() >= 2);
		}
	}
	
	@Override
	protected void handleDoubleClick() {
		// TODO workspace match �Ʒ� ���ο� separator�� �߰��ؼ� selected items �������� �־ ���͸��ص� �����ֵ��� ó���ϱ� ...
		// ����� History�� ���� �����ؼ� �����丮 ������� �����ؼ� ���� �� �ֵ��� ���ִ� ������
		computeResult();
	}
}
