package services.community;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import l2f.commons.dbutils.DbUtils;
import l2f.gameserver.Config;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.tables.ClanTable;
import l2f.gameserver.taskmanager.AutoImageSenderManager;
import l2f.gameserver.utils.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityRanking implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CommunityRanking.class);

	@Override
	public void onLoad()
	{
		if(Config.COMMUNITYBOARD_ENABLED)
		{
			selectRankingPK();
			selectRankingPVP();
			selectRankingRK();
			selectRankingCIS();
			selectRankingCIP();
			_log.info("Ranking in the commynity board has been updated.");
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
	public String[] getBypassCommands()
	{
		return new String[] { "_bbsloc", "_bbsranking" };
	}

	private static class RankingManager
	{
		private String[] RankingPvPName = new String[10];
		private String[] RankingPvPClan = new String[10];
		private int[] RankingPvPClass = new int[10];
		private int[] RankingPvPOn = new int[10];
		private int[] RankingPvP = new int[10];

		private String[] RankingPkName = new String[10];
		private String[] RankingPkClan = new String[10];
		private int[] RankingPkClass = new int[10];
		private int[] RankingPkOn = new int[10];
		private int[] RankingPk = new int[10];

		private String[] RankingRaidName = new String[10];
		private String[] RankingRaidClan = new String[10];
		private int[] RankingRaidClass = new int[10];
		private int[] RankingRaidOn = new int[10];
		private int[] RankingRaid = new int[10];

		private String[] RankingInstanceSoloName = new String[10];
		private String[] RankingInstanceSoloClan = new String[10];
		private int[] RankingInstanceSoloClass = new int[10];
		private int[] RankingInstanceSoloOn = new int[10];
		private int[] RankingInstanceSolo = new int[10];

		private String[] RankingInstancePartyName = new String[10];
		private String[] RankingInstancePartyClan = new String[10];
		private int[] RankingInstancePartyClass = new int[10];
		private int[] RankingInstancePartyOn = new int[10];
		private int[] RankingInstanceParty = new int[10];
	}

	static RankingManager RankingManagerStats = new RankingManager();
	private long update = System.currentTimeMillis() / 1000;
	private int number = 0;
	private int time_update = 60;

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		if(!Config.COMMUNITYBOARD_RANKING_ENABLED)
		{
			String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "off.htm", player);
			ShowBoard.separateAndSend(html, player);
			return;
		}

		//Checking if all required images were sent to the player, if not - not allowing to pass
		if(!AutoImageSenderManager.wereAllImagesSent(player))
		{
			player.sendPacket(new Say2(player.getObjectId(), ChatType.CRITICAL_ANNOUNCE, "CB", "Community wasn't loaded yet, try again in few seconds."));
			return;
		}

		player.setSessionVar("add_fav", null);
		if(update + time_update * 60 < System.currentTimeMillis() / 1000)
		{
			selectRankingPK();
			selectRankingPVP();
			selectRankingRK();
			selectRankingCIS();
			selectRankingCIP();
			update = System.currentTimeMillis() / 1000;
			_log.info("Ranking in the commynity board has been updated.");
		}

		if(bypass.equals("_bbsloc") || bypass.equals("_bbsranking:pk"))
			show(player, 1);
		else if(bypass.equals("_bbsranking:pvp"))
			show(player, 2);
		else if(bypass.equals("_bbsranking:rk"))
			show(player, 3);
		else if(bypass.equals("_bbsranking:cis"))
			show(player, 4);
		else if(bypass.equals("_bbsranking:cip"))
			show(player, 5);
	}

	private void show(Player player, int page)
	{
		number = 0;
		String html = null;

		if(page == 1)
		{
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/pk.htm", player);
			while(number < 10)
			{
				if(RankingManagerStats.RankingPkName[number] != null)
				{
					html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingPkName[number]);
					html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingPkClan[number] == null ? "<font color=\"B59A75\">No Clan</font>" : RankingManagerStats.RankingPkClan[number]);
					html = html.replace("<?class_" + number + "?>", Util.getFullClassName(RankingManagerStats.RankingPkClass[number]));
					html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingPkOn[number] == 1 ? "<font color=\"66FF33\">Yes</font>" : "<font color=\"B59A75\">No</font>");
					html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingPk[number]));
				}
				else
				{
					html = html.replace("<?name_" + number + "?>", "...");
					html = html.replace("<?clan_" + number + "?>", "...");
					html = html.replace("<?class_" + number + "?>", "...");
					html = html.replace("<?on_" + number + "?>", "...");
					html = html.replace("<?count_" + number + "?>", "...");
				}

				number++;
			}
		}
		else if(page == 2)
		{
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/pvp.htm", player);
			while(number < 10)
			{
				if(RankingManagerStats.RankingPvPName[number] != null)
				{
					html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingPvPName[number]);
					html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingPvPClan[number] == null ? "<font color=\"B59A75\">No Clan</font>" : RankingManagerStats.RankingPvPClan[number]);
					html = html.replace("<?class_" + number + "?>", Util.getFullClassName(RankingManagerStats.RankingPvPClass[number]));
					html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingPvPOn[number] == 1 ? "<font color=\"66FF33\">Yes</font>" : "<font color=\"B59A75\">No</font>");
					html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingPvP[number]));
				}
				else
				{
					html = html.replace("<?name_" + number + "?>", "...");
					html = html.replace("<?clan_" + number + "?>", "...");
					html = html.replace("<?class_" + number + "?>", "...");
					html = html.replace("<?on_" + number + "?>", "...");
					html = html.replace("<?count_" + number + "?>", "...");
				}
				number++;
			}
		}
		else if(page == 3)
		{
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/rk.htm", player);
			while(number < 10)
			{
				if(RankingManagerStats.RankingRaidName[number] != null)
				{
					html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingRaidName[number]);
					html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingRaidClan[number] == null ? "<font color=\"B59A75\">No Clan</font>" : RankingManagerStats.RankingRaidClan[number]);
					html = html.replace("<?class_" + number + "?>", Util.getFullClassName(RankingManagerStats.RankingRaidClass[number]));
					html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingRaidOn[number] == 1 ? "<font color=\"66FF33\">Yes</font>" : "<font color=\"B59A75\">No</font>");
					html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingRaid[number]));
				}
				else
				{
					html = html.replace("<?name_" + number + "?>", "...");
					html = html.replace("<?clan_" + number + "?>", "...");
					html = html.replace("<?class_" + number + "?>", "...");
					html = html.replace("<?on_" + number + "?>", "...");
					html = html.replace("<?count_" + number + "?>", "...");
				}
				number++;
			}
		}
		else if(page == 4)
		{
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/cis.htm", player);
			while(number < 10)
			{
				if(RankingManagerStats.RankingInstanceSoloName[number] != null)
				{
					html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingInstanceSoloName[number]);
					html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingInstanceSoloClan[number] == null ? "<font color=\"B59A75\">No Clan</font>" : RankingManagerStats.RankingInstanceSoloClan[number]);
					html = html.replace("<?class_" + number + "?>", Util.getFullClassName(RankingManagerStats.RankingInstanceSoloClass[number]));
					html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingInstanceSoloOn[number] == 1 ? "<font color=\"66FF33\">Yes</font>" : "<font color=\"B59A75\">No</font>");
					html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingInstanceSolo[number]));
				}
				else
				{
					html = html.replace("<?name_" + number + "?>", "...");
					html = html.replace("<?clan_" + number + "?>", "...");
					html = html.replace("<?class_" + number + "?>", "...");
					html = html.replace("<?on_" + number + "?>", "...");
					html = html.replace("<?count_" + number + "?>", "...");
				}
				number++;
			}
		}
		else if(page == 5)
		{
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/cip.htm", player);
			while(number < 10)
			{
				if(RankingManagerStats.RankingInstancePartyName[number] != null)
				{
					html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingInstancePartyName[number]);
					html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingInstancePartyClan[number] == null ? "<font color=\"B59A75\">No Clan</font>" : RankingManagerStats.RankingInstancePartyClan[number]);
					html = html.replace("<?class_" + number + "?>", Util.getFullClassName(RankingManagerStats.RankingInstancePartyClass[number]));
					html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingInstancePartyOn[number] == 1 ? "<font color=\"66FF33\">Yes</font>" : "<font color=\"B59A75\">No</font>");
					html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingInstanceParty[number]));
				}
				else
				{
					html = html.replace("<?name_" + number + "?>", "...");
					html = html.replace("<?clan_" + number + "?>", "...");
					html = html.replace("<?class_" + number + "?>", "...");
					html = html.replace("<?on_" + number + "?>", "...");
					html = html.replace("<?count_" + number + "?>", "...");
				}
				number++;
			}
		}
		else
		{
			_log.warn("Unknown page: " + page + " - " + player.getName());
			return;
		}

		html = html.replace("<?update?>", String.valueOf(time_update));
		html = html.replace("<?last_update?>", String.valueOf(time(update)));
		html = html.replace("<?ranking_menu?>", HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "ranking/menu.htm", player));
		ShowBoard.separateAndSend(html, player);
	}

	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private static String time(long time)
	{
		return TIME_FORMAT.format(new Date(time * 1000));
	}

	private void selectRankingPVP()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		number = 0;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, class_id, clanid, online, pvpkills FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id) WHERE cs.isBase=1 ORDER BY pvpkills DESC LIMIT " + 10);
			rset = statement.executeQuery();

			while(rset.next())
			{
				if(!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingPvPName[number] = rset.getString("char_name");
					int clan_id = rset.getInt("clanid");
					Clan clan = clan_id == 0 ? null : ClanTable.getInstance().getClan(clan_id);
					RankingManagerStats.RankingPvPClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingPvPClass[number] = rset.getInt("class_id");
					RankingManagerStats.RankingPvPOn[number] = rset.getInt("online");
					RankingManagerStats.RankingPvP[number] = rset.getInt("pvpkills");
				}
				else
				{
					RankingManagerStats.RankingPvPName[number] = null;
					RankingManagerStats.RankingPvPClan[number] = null;
					RankingManagerStats.RankingPvPClass[number] = 0;
					RankingManagerStats.RankingPvPOn[number] = 0;
					RankingManagerStats.RankingPvP[number] = 0;
				}
				number++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return;
	}

	private void selectRankingPK()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		number = 0;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, class_id, clanid, online, pkkills FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id) WHERE cs.isBase=1 ORDER BY pkkills DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while(rset.next())
			{
				if(!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingPkName[number] = rset.getString("char_name");
					int clan_id = rset.getInt("clanid");
					Clan clan = clan_id == 0 ? null : ClanTable.getInstance().getClan(clan_id);
					RankingManagerStats.RankingPkClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingPkClass[number] = rset.getInt("class_id");
					RankingManagerStats.RankingPkOn[number] = rset.getInt("online");
					RankingManagerStats.RankingPk[number] = rset.getInt("pkkills");
				}
				else
				{
					RankingManagerStats.RankingPkName[number] = null;
					RankingManagerStats.RankingPkClan[number] = null;
					RankingManagerStats.RankingPkClass[number] = 0;
					RankingManagerStats.RankingPkOn[number] = 0;
					RankingManagerStats.RankingPk[number] = 0;
				}
				number++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingRK()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		number = 0;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, class_id, clanid, online, raidkills FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id) WHERE cs.isBase=1 ORDER BY raidkills DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while(rset.next())
			{
				if(!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingRaidName[number] = rset.getString("char_name");
					int clan_id = rset.getInt("clanid");
					Clan clan = clan_id == 0 ? null : ClanTable.getInstance().getClan(clan_id);
					RankingManagerStats.RankingRaidClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingRaidClass[number] = rset.getInt("class_id");
					RankingManagerStats.RankingRaidOn[number] = rset.getInt("online");
					RankingManagerStats.RankingRaid[number] = rset.getInt("raidkills");
				}
				else
				{
					RankingManagerStats.RankingRaidName[number] = null;
					RankingManagerStats.RankingRaidClan[number] = null;
					RankingManagerStats.RankingRaidClass[number] = 0;
					RankingManagerStats.RankingRaidOn[number] = 0;
					RankingManagerStats.RankingRaid[number] = 0;
				}
				number++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingCIS()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		number = 0;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, class_id, clanid, online, soloinstance FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id) WHERE cs.isBase=1 ORDER BY soloinstance DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while(rset.next())
			{
				if(!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingInstanceSoloName[number] = rset.getString("char_name");
					int clan_id = rset.getInt("clanid");
					Clan clan = clan_id == 0 ? null : ClanTable.getInstance().getClan(clan_id);
					RankingManagerStats.RankingInstanceSoloClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingInstanceSoloClass[number] = rset.getInt("class_id");
					RankingManagerStats.RankingInstanceSoloOn[number] = rset.getInt("online");
					RankingManagerStats.RankingInstanceSolo[number] = rset.getInt("soloinstance");
				}
				else
				{
					RankingManagerStats.RankingInstanceSoloName[number] = null;
					RankingManagerStats.RankingInstanceSoloClan[number] = null;
					RankingManagerStats.RankingInstanceSoloClass[number] = 0;
					RankingManagerStats.RankingInstanceSoloOn[number] = 0;
					RankingManagerStats.RankingInstanceSolo[number] = 0;
				}
				number++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingCIP()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		number = 0;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, class_id, clanid, online, partyinstance FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id) WHERE cs.isBase=1 ORDER BY partyinstance DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while(rset.next())
			{
				if(!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingInstancePartyName[number] = rset.getString("char_name");
					int clan_id = rset.getInt("clanid");
					Clan clan = clan_id == 0 ? null : ClanTable.getInstance().getClan(clan_id);
					RankingManagerStats.RankingInstancePartyClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingInstancePartyClass[number] = rset.getInt("class_id");
					RankingManagerStats.RankingInstancePartyOn[number] = rset.getInt("online");
					RankingManagerStats.RankingInstanceParty[number] = rset.getInt("partyinstance");
				}
				else
				{
					RankingManagerStats.RankingInstancePartyName[number] = null;
					RankingManagerStats.RankingInstancePartyClan[number] = null;
					RankingManagerStats.RankingInstancePartyClass[number] = 0;
					RankingManagerStats.RankingInstancePartyOn[number] = 0;
					RankingManagerStats.RankingInstanceParty[number] = 0;
				}
				number++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	@Override
	public void onShutdown()
	{}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{}
}
