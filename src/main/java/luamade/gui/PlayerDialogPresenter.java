package luamade.gui;

import api.network.packets.PacketUtil;
import luamade.network.PacketCSPlayerDialogResponse;
import luamade.network.PacketSCPlayerDialogRequest;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side presenter for {@code player.confirm()} / {@code player.message()}
 * dialog requests streamed from the server. This is the exact same
 * {@link PlayerGameOkCancelInput} dialog {@code luamade.lua.player.Player}
 * used to open directly when scripts ran client-side — only the trigger
 * changed (a received packet instead of an in-process call), and the result
 * now goes back over the network instead of completing a local future.
 */
public final class PlayerDialogPresenter {

	private static final AtomicInteger windowCounter = new AtomicInteger(0);

	private PlayerDialogPresenter() {
	}

	public static void present(PacketSCPlayerDialogRequest request) {
		GameClientState client = GameClientState.instance;
		if(client == null) {
			return;
		}

		String windowId = "LOGI_DIALOG_" + windowCounter.incrementAndGet();
		PlayerGameOkCancelInput dialog = new PlayerGameOkCancelInput(windowId, client, request.getTitle(), request.getBody()) {
			@Override
			public void pressedOK() {
				PacketUtil.sendPacketToServer(new PacketCSPlayerDialogResponse(request.getRequestId(), true));
				deactivate();
			}

			@Override
			public void onDeactivate() {
				// Covers Cancel, Esc, and window close. If pressedOK already replied,
				// the server has already completed the request and this reply is
				// harmless — PlayerDialogRequests.complete() no-ops on an unknown id.
				PacketUtil.sendPacketToServer(new PacketCSPlayerDialogResponse(request.getRequestId(), false));
			}
		};
		dialog.activate();
	}
}
