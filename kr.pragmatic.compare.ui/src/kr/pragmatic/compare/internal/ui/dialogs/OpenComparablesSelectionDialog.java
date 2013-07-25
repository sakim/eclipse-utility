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
		// TODO workspace match 아래 새로운 separator를 추가해서 selected items 섹션으로 넣어서 필터링해도 남아있도록 처리하기 ...
		// 현재는 History에 강제 삽입해서 히스토리 목록으로 선택해서 비교할 수 있도록 해주는 정도임
		computeResult();
	}
}
