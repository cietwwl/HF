package npc.model;

import instances.Frintezza;
import l2f.gameserver.model.Party;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.templates.npc.NpcTemplate;
import l2f.gameserver.utils.ItemFunctions;
import l2f.gameserver.utils.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author pchayka
 */

public final class FrintezzaGatekeeperInstance extends NpcInstance
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8021119802595935918L;
	private static final int INSTANCE_ID = 136;
	private static final int QUEST_ITEM_ID = 8073;

	public FrintezzaGatekeeperInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!canBypassCheck(player, this))
			return;

		if (command.equalsIgnoreCase("request_frintezza"))
		{
			Reflection r = player.getActiveReflection();
			if (r != null)
			{
				if (player.canReenterInstance(INSTANCE_ID))
					player.teleToLocation(r.getTeleportLoc(), r);
			}
			else if (player.canEnterInstance(INSTANCE_ID))
			{
				Collection<Player> playersToJoin = getPlayersToJoin(player);
				if (checkReqiredItem(player, playersToJoin))
				{
					deleteRequiredItems(playersToJoin);
					ReflectionUtils.enterReflection(player, new Frintezza(), INSTANCE_ID);
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private static Collection<Player> getPlayersToJoin(Player player)
	{
		Collection<Party> parties = new ArrayList<Party>();
		if (player.getParty().getCommandChannel() != null)
		{
			parties.addAll(player.getParty().getCommandChannel().getParties());
		}
		else
		{
			parties.add(player.getParty());
		}
		Collection<Player> players = new ArrayList<Player>();
		for (Party party : parties)
			players.addAll(party.getPartyMembers());
		return players;
	}
	
	private static boolean checkReqiredItem(Player leader, Iterable<Player> allPlayers)
	{
		boolean canJoin = true;
		for (Player playerToJoin : allPlayers)
			if (playerToJoin.getInventory().getCountOf(QUEST_ITEM_ID) < 1L)
			{
				if (!leader.equals(playerToJoin))
					leader.sendMessage(playerToJoin.getName() + " doesn't have required item!");
				playerToJoin.sendMessage("You don't have required item!");
				canJoin = false;
			}
		
		return canJoin;
	}
	
	private static void deleteRequiredItems(Iterable<Player> players)
	{
		for (Player player : players)
			ItemFunctions.removeItem(player, QUEST_ITEM_ID, 1L, true, "FrintezzaGatekeeper");
	}
}