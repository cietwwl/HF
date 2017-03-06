package l2f.rGuard.network.clientpackets;

import  l2f.gameserver.network.GameClient;
import  l2f.gameserver.network.clientpackets.L2GameClientPacket;

public class GameGuardReply extends L2GameClientPacket
{
	private int _dx;

	@Override
	protected void readImpl()
	{
		_dx = readC();
	}

	@Override
	protected void runImpl()
	{
		GameClient client = getClient();
		if (_dx == 104)
			client.setGameGuardOk(true);
		else
			client.setGameGuardOk(false);

	}

	@Override
	public String getType()
	{
		return "[C] CB GameGuardReply";
	}
}