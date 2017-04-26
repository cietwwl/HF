package npc.model;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.templates.npc.NpcTemplate;

/**
 * @author pchayka
 */
public class KegorNpcInstance extends NpcInstance
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8496053332954453707L;

	public KegorNpcInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val, Player player)
	{
		String htmlpath = null;
		if (getReflection().isDefault())
			htmlpath = "default/32761-default.htm";
		else
			htmlpath = "default/32761.htm";
		return htmlpath;
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!canBypassCheck(player, this))
			return;

		if (command.equalsIgnoreCase("request_stone"))
		{
			if (player.getInventory().getCountOf(15469) == 0 && player.getInventory().getCountOf(15470) == 0)
				Functions.addItem(player, 15469, 1, "KegorNpcInstance");
			else
				player.sendMessage("You can't take more than 1 Frozen Core.");
		}
		else
			super.onBypassFeedback(player, command);
	}
}