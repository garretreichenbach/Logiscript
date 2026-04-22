package luamade.gui;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialogPanel;
import luamade.network.ClientVaultCache;
import luamade.network.PacketCSVaultDeposit;
import luamade.network.PacketCSVaultWithdraw;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

/**
 * Interact UI for a {@link luamade.element.block.Vault} block.
 *
 * <p>The dialog is opened in response to {@link luamade.network.PacketSCVaultView},
 * which carries the current balance, the player's credits, and the access level
 * the server has determined for this player. Deposit and Withdraw buttons open a
 * text-input sub-dialog that sends {@link PacketCSVaultDeposit} /
 * {@link PacketCSVaultWithdraw} — the server re-validates access before applying
 * any change.
 *
 * <p>A static {@link #activeDialog} reference lets the incoming view packet
 * refresh an already-open dialog rather than stacking a new one.
 */
public class VaultDialog extends PlayerInput {

	private static VaultDialog activeDialog;

	private final VaultPanel panel;

	public VaultDialog(ClientVaultCache.View initialView) {
		super(GameClient.getClientState());
		panel = new VaultPanel(getState(), this, initialView);
	}

	public static void onViewReceived(ClientVaultCache.View view, String statusMessage) {
		if(activeDialog != null && activeDialog.panel != null && activeDialog.panel.view.uuid.equals(view.uuid)) {
			activeDialog.panel.refresh(view, statusMessage);
			return;
		}
		if(activeDialog != null) activeDialog.deactivate();
		VaultDialog dlg = new VaultDialog(view);
		dlg.panel.pendingStatus = statusMessage;
		dlg.activate();
	}

	@Override
	public VaultPanel getInputPanel() {
		return panel;
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
		if(isOccluded() || !mouseEvent.pressedLeftMouse()) return;
		Object ptr = callingElement.getUserPointer();
		if(!(ptr instanceof String)) return;
		String tag = (String) ptr;
		switch(tag) {
			case "X":
			case "CANCEL":
			case "OK":
			case "CLOSE":
				deactivate();
				break;
			case "DEPOSIT":
				promptAmount("Deposit", "Enter amount to deposit:", true);
				break;
			case "WITHDRAW":
				promptAmount("Withdraw", "Enter amount to withdraw:", false);
				break;
			default:
				break;
		}
	}

	private void promptAmount(String title, String description, boolean isDeposit) {
		final int entityId = panel.view.entityId;
		final long absIndex = panel.view.absIndex;
		new PlayerGameTextInput("LUAMADE_VAULT_AMOUNT", (GameClientState) getState(), 20, title, description) {
			@Override
			public boolean onInput(String entry) {
				if(entry == null) return false;
				String trimmed = entry.trim();
				long amt;
				try {
					amt = Long.parseLong(trimmed);
				} catch(NumberFormatException ex) {
					setErrorMessage("Not a number");
					return false;
				}
				if(amt <= 0) {
					setErrorMessage("Amount must be positive");
					return false;
				}
				if(isDeposit) {
					PacketUtil.sendPacketToServer(new PacketCSVaultDeposit(entityId, absIndex, amt));
				} else {
					PacketUtil.sendPacketToServer(new PacketCSVaultWithdraw(entityId, absIndex, amt));
				}
				return true;
			}

			@Override
			public void onDeactivate() {
			}

			@Override
			public void onFailedTextCheck(String msg) {
				setErrorMessage(msg);
			}

			@Override
			public String handleAutoComplete(String s, org.schema.schine.common.TextCallback cb, String prefix) {
				return "";
			}

			@Override
			public String[] getCommandPrefixes() {
				return null;
			}
		}.activate();
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
	}

	@Override
	public void onDeactivate() {
		if(activeDialog == this) activeDialog = null;
	}

	@Override
	public void activate() {
		activeDialog = this;
		super.activate();
	}

	static final class VaultPanel extends GUIInputDialogPanel {

		ClientVaultCache.View view;
		String pendingStatus = "";

		private GUITextOverlay balanceText;
		private GUITextOverlay creditsText;
		private GUITextOverlay accessText;
		private GUITextOverlay statusText;

		private GUITextButton depositButton;
		private GUITextButton withdrawButton;
		private GUITextButton closeButton;

		VaultPanel(InputState state, GUICallback callback, ClientVaultCache.View initialView) {
			super(state, "LUAMADE_VAULT", "Vault", "Credit Vault", 480, 280, callback);
			this.view = initialView;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIAncor root = contentPane.getContent(0);

			balanceText = new GUITextOverlay(440, 20, FontLibrary.FontSize.MEDIUM, getState());
			balanceText.setPos(16, 16, 0);
			balanceText.onInit();
			root.attach(balanceText);

			creditsText = new GUITextOverlay(440, 20, FontLibrary.FontSize.MEDIUM, getState());
			creditsText.setPos(16, 42, 0);
			creditsText.onInit();
			root.attach(creditsText);

			accessText = new GUITextOverlay(440, 20, FontLibrary.FontSize.MEDIUM, getState());
			accessText.setPos(16, 68, 0);
			accessText.onInit();
			root.attach(accessText);

			statusText = new GUITextOverlay(440, 20, FontLibrary.FontSize.SMALL, getState());
			statusText.setPos(16, 96, 0);
			statusText.setColor(1.0f, 0.85f, 0.4f, 1.0f);
			statusText.onInit();
			root.attach(statusText);

			depositButton = new GUITextButton(getState(), 120, 24, GUITextButton.ColorPalette.OK, "DEPOSIT", getCallback());
			depositButton.setUserPointer("DEPOSIT");
			depositButton.setMouseUpdateEnabled(true);
			depositButton.setPos(16, 140, 0);
			depositButton.onInit();
			root.attach(depositButton);

			withdrawButton = new GUITextButton(getState(), 120, 24, GUITextButton.ColorPalette.FRIENDLY, "WITHDRAW", getCallback());
			withdrawButton.setUserPointer("WITHDRAW");
			withdrawButton.setMouseUpdateEnabled(true);
			withdrawButton.setPos(144, 140, 0);
			withdrawButton.onInit();
			root.attach(withdrawButton);

			closeButton = new GUITextButton(getState(), 120, 24, GUITextButton.ColorPalette.CANCEL, "CLOSE", getCallback());
			closeButton.setUserPointer("CLOSE");
			closeButton.setMouseUpdateEnabled(true);
			closeButton.setPos(272, 140, 0);
			closeButton.onInit();
			root.attach(closeButton);

			applyView();
			if(pendingStatus != null && !pendingStatus.isEmpty()) {
				statusText.setTextSimple(pendingStatus);
				pendingStatus = "";
			}
		}

		void refresh(ClientVaultCache.View newView, String statusMessage) {
			this.view = newView;
			if(balanceText == null) {
				// onInit hasn't run yet — stash the status for applyView to pick up.
				pendingStatus = statusMessage == null ? "" : statusMessage;
				return;
			}
			applyView();
			statusText.setTextSimple(statusMessage == null ? "" : statusMessage);
		}

		private void applyView() {
			balanceText.setTextSimple("Vault Balance: " + formatCredits(view.balance));
			creditsText.setTextSimple("Your Credits:  " + formatCredits(view.playerCredits));
			accessText.setTextSimple("Access: " + humanAccess(view.accessLevel));
			boolean canDeposit = !"NONE".equalsIgnoreCase(view.accessLevel);
			boolean canWithdraw = canDeposit && !"PUBLIC_DEPOSIT".equalsIgnoreCase(view.accessLevel);
			if(depositButton != null) depositButton.setActive(canDeposit);
			if(withdrawButton != null) withdrawButton.setActive(canWithdraw);
		}

		private static String formatCredits(long c) {
			return String.format("%,d", c);
		}

		private static String humanAccess(String level) {
			if(level == null) return "none";
			switch(level.toUpperCase()) {
				case "OWNER": return "owner (faction match)";
				case "FACTION": return "faction (via permission module)";
				case "PASSWORD": return "password-authed";
				case "PUBLIC_DEPOSIT": return "public (deposit only)";
				default: return "no access";
			}
		}
	}
}
