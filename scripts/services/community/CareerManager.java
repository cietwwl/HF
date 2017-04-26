package services.community;

import l2f.gameserver.Config;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.ClassId;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.templates.item.ItemTemplate;
import l2f.gameserver.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

public class CareerManager implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CareerManager.class);
	
	@Override
	public void onLoad()
	{
		if (Config.COMMUNITYBOARD_ENABLED/* && Config.BBS_PVP_CB_ENABLED*/)
		{
			_log.info("CommunityBoard: Manage Career service loaded.");
			CommunityBoardManager.getInstance().registerHandler(this);
		}
	}

	@Override
	public void onReload()
	{
		if (Config.COMMUNITYBOARD_ENABLED/* && Config.BBS_PVP_CB_ENABLED*/)
			CommunityBoardManager.getInstance().removeHandler(this);
	}

	@Override
	public void onShutdown()
	{}

	@Override
	public String[] getBypassCommands()
	{
		return new String[] { "_bbscareer;", "_bbscareer;sub;", "_bbscareer;classmaster;change_class;" };
	}

	@Override
	public void onBypassCommand(Player activeChar, String command)
	{
		if (!CheckCondition(activeChar))
			return;

		if (command.startsWith("_bbscareer;"))
		{
			ClassId classId = activeChar.getClassId();
			int jobLevel = classId.getLevel();
			int level = activeChar.getLevel();
			StringBuilder html = new StringBuilder();
			html.append("<br>");
			html.append("<table width=600>");
			html.append("<tr><td>");
			if (Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() || !Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
				jobLevel = 4;

			if ((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
			{
				ItemTemplate item = ItemHolder.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
				html.append("You have to pay: <font color=\"LEVEL\">");
				html.append(Util.formatAdena(Config.CLASS_MASTERS_PRICE_LIST[jobLevel])).append("</font> <font color=\"LEVEL\">").append(item.getName()).append("</font> to change profession<br>");
				html.append("<center><table width=600><tr>");
				for (ClassId cid : ClassId.values())
				{
					if (cid == ClassId.inspector)
						continue;
					if (cid.childOf(classId) && cid.level() == classId.level() + 1)
						html.append("<td><center><button value=\"").append(cid.name()).append("\" action=\"bypass _bbscareer;classmaster;change_class;").append(cid.getId()).append(";").append(Config.CLASS_MASTERS_PRICE_LIST[jobLevel]).append("\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td>");

				}
				html.append("</tr></table></center>");
				html.append("</td>");
				html.append("</tr>");
				html.append("</table>");
			}
			String content = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "pages/career.htm", activeChar);
			content = content.replace("%career%", html.toString());
			ShowBoard.separateAndSend(content, activeChar);
		}
		if (command.startsWith("_bbscareer;classmaster;change_class;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			st.nextToken();
			short val = Short.parseShort(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			ItemTemplate item = ItemHolder.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			ItemInstance pay = activeChar.getInventory().getItemByItemId(item.getItemId());
			if (pay != null && pay.getCount() >= price)
			{
				activeChar.getInventory().destroyItem(pay, (long) price, "Class Changer");
				changeClass(activeChar, val);
				onBypassCommand(activeChar, "_bbscareer;");
			}
			else if (Config.CLASS_MASTERS_PRICE_ITEM == 57)
			{
				activeChar.sendPacket(new SystemMessage2(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage2(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			}
		}
	}

	private static void changeClass(Player player, int val)
	{
		if (player.getClassId().getLevel() == 3)
			player.sendPacket(SystemMsg.CONGRATULATIONS__YOUVE_COMPLETED_YOUR_THIRDCLASS_TRANSFER_QUEST); // ??? 3 ?????
		else
			player.sendPacket(SystemMsg.CONGRATULATIONS__YOUVE_COMPLETED_A_CLASS_TRANSFER); // ??? 1 ? 2 ?????

		player.setClassId(val, false, false);
		player.broadcastUserInfo(true);
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{}

	private static boolean CheckCondition(Player player)
	{
		if (player == null)
            return false;

		if (!Config.USE_BBS_PROF_IS_COMBAT && (player.getPvpFlag() != 0 || player.isInDuel() || player.isInCombat() || player.isAttackingNow()))
		{
			player.sendMessage("During combat, you can not use this feature.");
			return false;
		}

		return true;
	}
}