package l2f.rGuard.hwidmanager;

import  l2f.gameserver.handler.admincommands.IAdminCommandHandler;
import  l2f.gameserver.model.GameObject;
import  l2f.gameserver.model.Player;
import  l2f.rGuard.ConfigProtection;

public class HWIDAdminBan implements IAdminCommandHandler
{

	private static enum Commands
	{
		admin_hwid_ban
	}

        @Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player player)
	{

		if (!ConfigProtection.ALLOW_GUARD_SYSTEM)
		{
			return false;
		}
		if (player == null)
		{
			return false;
		}
		if (!fullString.startsWith("admin_hwid"))
		{
			return false;
		}
		if (fullString.startsWith("admin_hwid_ban"))
		{

			GameObject playerTarger = player.getTarget();
			if (playerTarger == null && !(playerTarger instanceof Player))
			{
				player.sendMessage("Target is empty");
				return false;
			}
			Player target = (Player) playerTarger;
			if (target != null)
			{
				HWIDBan.addHWIDBan(target.getNetConnection());
				player.sendMessage(target.getName() + " banned in HWID");
			}
		}
		return true;
	}

	@SuppressWarnings("rawtypes")
        @Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}