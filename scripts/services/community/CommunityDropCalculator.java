package services.community;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import l2f.gameserver.Config;
import l2f.gameserver.cache.ImagesCache;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.data.xml.holder.NpcHolder;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.instancemanager.SpawnManager;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.olympiad.Olympiad;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.model.reward.CalculateRewardChances;
import l2f.gameserver.network.serverpackets.RadarControl;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.taskmanager.AutoImageSenderManager;
import l2f.gameserver.templates.item.ItemTemplate;
import l2f.gameserver.templates.npc.NpcTemplate;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import actions.RewardListInfo;

/**
 * Community Board page containing Drop Calculator
 */
public class CommunityDropCalculator implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CommunityDropCalculator.class);

	@Override
	public void onLoad()
	{
		if(Config.COMMUNITYBOARD_ENABLED)
		{
			_log.info("CommunityBoard: Drop Calculator service loaded.");
			CommunityBoardManager.getInstance().registerHandler(this);
		}
	}

	@Override
	public void onReload()
	{
		if(Config.COMMUNITYBOARD_ENABLED)
			CommunityBoardManager.getInstance().removeHandler(this);
	}

	@Override
	public void onShutdown()
	{}

	@Override
	public String[] getBypassCommands()
	{
		return new String[] {
				"_friendlist_",
				"_dropCalc",
				"_dropItemsByName",
				"_dropMonstersByItem",
				"_dropMonstersByName",
				"_dropMonsterDetailsByItem",
				"_dropMonsterDetailsByName" };
	}

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		//Checking if all required images were sent to the player, if not - not allowing to pass
		if(!AutoImageSenderManager.wereAllImagesSent(player))
		{
			player.sendPacket(new Say2(player.getObjectId(), ChatType.CRITICAL_ANNOUNCE, "CB", "Community wasn't loaded yet, try again in few seconds."));
			return;
		}
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		player.setSessionVar("add_fav", null);

		if(!Config.ALLOW_DROP_CALCULATOR)
		{
			String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropCalcOff.htm", player);
			ShowBoard.separateAndSend(html, player);
			return;
		}

		if("dropCalc".equals(cmd) || "friendlist".equals(cmd))
		{
			showMainPage(player);
			return;
		}
		else if("dropItemsByName".equals(cmd))
		{
			if(!st.hasMoreTokens())
			{
				showMainPage(player);
				return;
			}
			String itemName = st.nextToken().trim();
			int itemsPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showDropItemsByNamePage(player, itemName, itemsPage);
			return;
		}
		else if("dropMonstersByItem".equals(cmd))
		{
			int itemId = Integer.parseInt(st.nextToken());
			int monstersPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showDropMonstersByItem(player, itemId, monstersPage);
			return;
		}
		else if("dropMonsterDetailsByItem".equals(cmd))
		{
			int monsterId = Integer.parseInt(st.nextToken());
			showdropMonsterDetailsByItem(player, monsterId);

			if(st.hasMoreTokens())
				manageButton(player, Integer.parseInt(st.nextToken()), monsterId);
			return;
		}
		else if("dropMonstersByName".equals(cmd))
		{
			if(!st.hasMoreTokens())
			{
				showMainPage(player);
				return;
			}
			String monsterName = st.nextToken().trim();
			int monsterPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showDropMonstersByName(player, monsterName, monsterPage);
			return;
		}
		else if("dropMonsterDetailsByName".equals(cmd))
		{
			int chosenMobId = Integer.parseInt(st.nextToken());
			showDropMonsterDetailsByName(player, chosenMobId);

			if(st.hasMoreTokens())
				manageButton(player, Integer.parseInt(st.nextToken()), chosenMobId);
			return;
		}

	}

	private static void showMainPage(Player player)
	{
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropCalcMain.htm", player);
		ShowBoard.separateAndSend(html, player);
	}

	private static void showDropItemsByNamePage(Player player, String itemName, int page)
	{
		player.addQuickVar("DCItemName", itemName);
		player.addQuickVar("DCItemsPage", page);
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropItemsByName.htm", player);
		html = replaceItemsByNamePage(html, itemName, page);
		ShowBoard.separateAndSend(html, player);
	}

	private static String replaceItemsByNamePage(String html, String itemName, int page)
	{
		String newHtml = html;

		List<ItemTemplate> itemsByName = ItemHolder.getInstance().getItemsByNameContainingString(itemName, true);

		itemsByName = sortItems(itemsByName, itemName);

		int itemIndex = 0;

		for(int i = 0; i < 12; i++)
		{
			itemIndex = i + (page - 1) * 12;
			ItemTemplate item = itemsByName.size() > itemIndex ? itemsByName.get(itemIndex) : null;

			newHtml = newHtml.replace("<?name_" + i + "?>", item != null ? getName(item.getName()) : "...");
			newHtml = newHtml.replace("<?drop_" + i + "?>", item != null ? String.valueOf(CalculateRewardChances.getDroplistsCountByItemId(item.getItemId(), true)) : "...");
			newHtml = newHtml.replace("<?spoil_" + i + "?>", item != null ? String.valueOf(CalculateRewardChances.getDroplistsCountByItemId(item.getItemId(), false)) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", item != null ? "<button value=\"show\" action=\"bypass _dropMonstersByItem_" + item.getItemId() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}

		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _dropItemsByName_" + itemName + "_" + (page - 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", itemsByName.size() > itemIndex + 1 ? "<button action=\"bypass _dropItemsByName_" + itemName + "_" + (page + 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");

		newHtml = newHtml.replace("<?search?>", itemName);
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(itemsByName.size()));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));

		return newHtml;
	}

	private static void showDropMonstersByItem(Player player, int itemId, int page)
	{
		player.addQuickVar("DCItemId", itemId);
		player.addQuickVar("DCMonstersPage", page);
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropMonstersByItem.htm", player);
		html = replaceMonstersByItemPage(player, html, itemId, page);
		ShowBoard.separateAndSend(html, player);
	}

	private static String replaceMonstersByItemPage(Player player, String html, int itemId, int page)
	{
		String newHtml = html;

		List<CalculateRewardChances.NpcTemplateDrops> templates = CalculateRewardChances.getNpcsByDropOrSpoil(itemId);

		templates = sortItemChances(player, templates, itemId);

		int npcIndex = 0;

		for(int i = 0; i < 12; i++)
		{
			npcIndex = i + (page - 1) * 12;
			CalculateRewardChances.NpcTemplateDrops drops = templates.size() > npcIndex ? templates.get(npcIndex) : null;
			NpcTemplate npc = templates.size() > npcIndex ? templates.get(npcIndex).template : null;

			newHtml = newHtml.replace("<?name_" + i + "?>", npc != null ? getName(npc.getName()) : "...");
			newHtml = newHtml.replace("<?level_" + i + "?>", npc != null ? String.valueOf(npc.level) : "...");
			newHtml = newHtml.replace("<?type_" + i + "?>", npc != null ? drops.dropNoSpoil ? "Drop" : "Spoil" : "...");
			newHtml = newHtml.replace("<?count_" + i + "?>", npc != null ? String.valueOf(getDropCount(player, npc, itemId, drops.dropNoSpoil)) : "...");
			newHtml = newHtml.replace("<?chance_" + i + "?>", npc != null ? String.valueOf(getDropChance(player, npc, itemId, drops.dropNoSpoil)) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", npc != null ? "<button value=\"show\" action=\"bypass _dropMonsterDetailsByItem_" + npc.getNpcId() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}

		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _dropMonstersByItem_" + itemId + "_" + (page - 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", templates.size() > npcIndex + 1 ? "<button action=\"bypass _dropMonstersByItem_" + itemId + "_" + (page + 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");

		newHtml = newHtml.replace("<?search?>", player.getQuickVarS("DCItemName"));
		newHtml = newHtml.replace("<?item?>", ItemHolder.getInstance().getTemplate(itemId).getName());
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(templates.size()));
		newHtml = newHtml.replace("<?back?>", String.valueOf(player.getQuickVarI("DCItemsPage")));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		return newHtml;
	}

	private static void showdropMonsterDetailsByItem(Player player, int monsterId)
	{
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropMonsterDetailsByItem.htm", player);
		html = replaceMonsterDetails(player, html, monsterId);

		ShowBoard.separateAndSend(html, player);
	}

	private static String replaceMonsterDetails(Player player, String html, int monsterId)
	{
		String newHtml = html;

		int itemId = player.getQuickVarI("DCItemId");
		NpcTemplate template = NpcHolder.getInstance().getTemplate(monsterId);
		ItemTemplate item = itemId > -1 ? ItemHolder.getInstance().getTemplate(itemId) : null;

		newHtml = newHtml.replace("<?name?>", String.valueOf(player.getQuickVarS("DCMonsterName")));
		newHtml = newHtml.replace("<?monster_name?>", template.getName());
		newHtml = newHtml.replace("<?item?>", item != null ? item.getName() : "...");
		newHtml = newHtml.replace("<?item_id?>", String.valueOf(itemId));
		newHtml = newHtml.replace("<?back?>", String.valueOf(player.getQuickVarI("DCMonstersPage")));
		newHtml = newHtml.replace("<?monster?>", String.valueOf(monsterId));
		newHtml = newHtml.replace("<?level?>", String.valueOf(template.level));
		newHtml = newHtml.replace("<?aggro?>", Util.boolToString(template.aggroRange > 0));

		newHtml = newHtml.replace("<?hp?>", Util.formatAdena((int) template.baseHpMax));
		newHtml = newHtml.replace("<?mp?>", Util.formatAdena((int) template.baseMpMax));

		newHtml = newHtml.replace("<?drop?>", itemId > -1 ? String.valueOf(getDropChance(player, template, itemId, true)) : "...");
		newHtml = newHtml.replace("<?spoil?>", itemId > -1 ? String.valueOf(getDropChance(player, template, itemId, false)) : "...");

		newHtml = newHtml.replace("<?droping?>", Util.formatAdena(CalculateRewardChances.getDrops(template, true, false).size()));
		newHtml = newHtml.replace("<?spoiling?>", Util.formatAdena(CalculateRewardChances.getDrops(template, false, true).size()));

		if(!AutoImageSenderManager.isImageAutoSendable(monsterId))
			ImagesCache.getInstance().sendImageToPlayer(player, monsterId);

		return newHtml;
	}

	private static void showDropMonstersByName(Player player, String monsterName, int page)
	{
		player.addQuickVar("DCMonsterName", monsterName);
		player.addQuickVar("DCMonstersPage", page);
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropMonstersByName.htm", player);
		html = replaceMonstersByName(html, monsterName, page);
		ShowBoard.separateAndSend(html, player);
	}

	private static String replaceMonstersByName(String html, String monsterName, int page)
	{
		String newHtml = html;

		List<NpcTemplate> npcTemplates = CalculateRewardChances.getNpcsContainingString(monsterName);

		npcTemplates = sortMonsters(npcTemplates, monsterName);

		int npcIndex = 0;

		for(int i = 0; i < 12; i++)
		{
			npcIndex = i + (page - 1) * 12;
			NpcTemplate npc = npcTemplates.size() > npcIndex ? npcTemplates.get(npcIndex) : null;

			newHtml = newHtml.replace("<?name_" + i + "?>", npc != null ? getName(npc.getName()) : "...");
			newHtml = newHtml.replace("<?drop_" + i + "?>", npc != null ? Util.formatAdena(CalculateRewardChances.getDrops(npc, true, false).size()) : "...");
			newHtml = newHtml.replace("<?spoil_" + i + "?>", npc != null ? Util.formatAdena(CalculateRewardChances.getDrops(npc, false, true).size()) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", npc != null ? "<button value=\"show\" action=\"bypass _dropMonsterDetailsByName_" + npc.getNpcId() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}

		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _dropMonstersByName_" + monsterName + "_" + (page - 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", npcTemplates.size() > npcIndex + 1 ? "<button action=\"bypass _dropMonstersByName_" + monsterName + "_" + (page + 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");

		newHtml = newHtml.replace("<?search?>", monsterName);
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(npcTemplates.size()));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));

		return newHtml;
	}

	private static void showDropMonsterDetailsByName(Player player, int monsterId)
	{
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_dropMonsterDetailsByName.htm", player);
		html = replaceMonsterDetails(player, html, monsterId);

		ShowBoard.separateAndSend(html, player);
	}

	private static void manageButton(Player player, int buttonId, int monsterId)
	{
		switch(buttonId)
		{
			case 1:
				player.sendPacket(new RadarControl(2, 2, 0, 0, 0));
				break;
			case 2://Show Drops
				RewardListInfo.showInfo(player, NpcHolder.getInstance().getTemplate(monsterId), false, false, 1.0);
				break;
			case 3://Teleport To Monster
				if(!canTeleToMonster(player, monsterId, true))
					return;

				NpcInstance aliveInstance = getAliveNpc(monsterId);
				if(aliveInstance != null)
					player.teleToLocation(aliveInstance.getLoc());
				else
					player.sendMessage("Monster isn't alive!");
				break;
			case 4://Show Monster on Map
				player.sendPacket(new Say2(player.getObjectId(), ChatType.COMMANDCHANNEL_ALL, "Info", "Open Map to see Locations"));

				for(Location loc : SpawnManager.getInstance().getRandomSpawnsByNpc(monsterId))
				{
					player.sendPacket(new RadarControl(0, 1, loc));
				}
				break;
		}
	}

	private static boolean canTeleToMonster(Player player, int monsterId, boolean sendMessage)
	{
		if(!player.isInZonePeace())
		{
			if(sendMessage)
				player.sendMessage("You can do it only in safe zone!");
			return false;
		}
		else if(Olympiad.isRegistered(player) || player.isInOlympiadMode())
		{
			if(sendMessage)
				player.sendMessage("You cannot do it while being registered in Olympiad Battle!");
			return false;
		}

		for(int id : Config.DROP_CALCULATOR_DISABLED_TELEPORT)
		{
			if(id == monsterId)
			{
				if(sendMessage)
					player.sendMessage("You cannot teleport to this Npc!");
				return false;
			}
		}
		return true;
	}

	private static String getName(String name)
	{
		if(name.length() > 23)
			return name.substring(0, 22) + "...";
		return name;
	}

	private static String getDropCount(Player player, NpcTemplate monster, int itemId, boolean drop)
	{
		long[] counts = CalculateRewardChances.getDropCounts(player, monster, drop, itemId);
		String formattedCounts = "[" + counts[0] + "..." + counts[1] + ']';

		return formattedCounts;
	}

	private static String getDropChance(Player player, NpcTemplate monster, int itemId, boolean drop)
	{
		String chance = CalculateRewardChances.getDropChance(player, monster, drop, itemId);
		return formatDropChance(String.valueOf(cutOff(chance, 2)));
	}

	private static double cutOff(String num, int pow)
	{
		double count = Double.parseDouble(num);
		return ((int) (count * Math.pow(10, pow))) / Math.pow(10, pow);
	}

	public static String formatDropChance(String chance)
	{
		String realChance = chance;

		if(realChance.endsWith(".0"))
			realChance = realChance.substring(0, realChance.length() - 2);

		return realChance + '%';
	}

	private static NpcInstance getAliveNpc(int npcId)
	{
		List<NpcInstance> instances = GameObjectsStorage.getAllByNpcId(npcId, true, true);
		return instances.isEmpty() ? null : instances.get(0);
	}

	private static List<ItemTemplate> sortItems(List<ItemTemplate> itemsByName, String search)
	{
		Collections.sort(itemsByName, new ItemComparator(search));
		return itemsByName;
	}

	private static class ItemComparator implements Comparator<ItemTemplate>, Serializable
	{
		private static final long serialVersionUID = -6389059445439769861L;
		private final String search;

		private ItemComparator(String search)
		{
			this.search = search;
		}

		@Override
		public int compare(ItemTemplate o1, ItemTemplate o2)
		{
			if(o1.equals(o2))
				return 0;
			if(o1.getName().equalsIgnoreCase(search))
				return -1;
			if(o2.getName().equalsIgnoreCase(search))
				return 1;

			return Integer.compare(CalculateRewardChances.getDroplistsCountByItemId(o2.getItemId(), true), CalculateRewardChances.getDroplistsCountByItemId(o1.getItemId(), true));
		}
	}

	private static List<CalculateRewardChances.NpcTemplateDrops> sortItemChances(Player player, List<CalculateRewardChances.NpcTemplateDrops> monsters, int itemId)
	{
		Collections.sort(monsters, new ItemChanceComparator(player, itemId));
		return monsters;
	}

	private static class ItemChanceComparator implements Comparator<CalculateRewardChances.NpcTemplateDrops>, Serializable
	{
		private static final long serialVersionUID = 6323413829869254438L;
		private final int itemId;
		private final Player player;

		private ItemChanceComparator(Player player, int itemId)
		{
			this.itemId = itemId;
			this.player = player;
		}

		@Override
		public int compare(CalculateRewardChances.NpcTemplateDrops o1, CalculateRewardChances.NpcTemplateDrops o2)
		{
			BigDecimal maxDrop1 = BigDecimal.valueOf(CalculateRewardChances.getDropCounts(player, o1.template, o1.dropNoSpoil, itemId)[1]);
			BigDecimal maxDrop2 = BigDecimal.valueOf(CalculateRewardChances.getDropCounts(player, o2.template, o2.dropNoSpoil, itemId)[1]);
			BigDecimal chance1 = new BigDecimal(CalculateRewardChances.getDropChance(player, o1.template, o1.dropNoSpoil, itemId));
			BigDecimal chance2 = new BigDecimal(CalculateRewardChances.getDropChance(player, o2.template, o2.dropNoSpoil, itemId));

			int compare = chance2.multiply(maxDrop2).compareTo(chance1.multiply(maxDrop1));
			if(compare == 0)
				return o2.template.getName().compareTo(o1.template.getName());
			return compare;
		}
	}

	private static List<NpcTemplate> sortMonsters(List<NpcTemplate> npcTemplates, String monsterName)
	{
		Collections.sort(npcTemplates, new MonsterComparator(monsterName));
		return npcTemplates;
	}

	private static class MonsterComparator implements Comparator<NpcTemplate>, Serializable
	{
		private static final long serialVersionUID = 2116090903265145828L;
		private final String search;

		private MonsterComparator(String search)
		{
			this.search = search;
		}

		@Override
		public int compare(NpcTemplate o1, NpcTemplate o2)
		{
			if(o1.equals(o2))
				return 0;
			if(o1.getName().equalsIgnoreCase(search))
				return 1;
			if(o2.getName().equalsIgnoreCase(search))
				return -1;

			return o2.getName().compareTo(o2.getName());
		}
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{}
}