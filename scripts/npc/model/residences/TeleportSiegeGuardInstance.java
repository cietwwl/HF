package npc.model.residences;

import l2f.gameserver.model.Player;
import l2f.gameserver.templates.npc.NpcTemplate;

/**
 * @author VISTALL
 * @date 17:49/13.07.2011
 */
public class TeleportSiegeGuardInstance extends SiegeGuardInstance
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9215565859722499907L;

	public TeleportSiegeGuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!canBypassCheck(player, this))
			return;

	}
}
