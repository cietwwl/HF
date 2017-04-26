package services.totalonline;

import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.tables.FakePlayersTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Online -> real + fake
 */
public class totalonline extends Functions implements ScriptFile
{
	private static final Logger LOG = LoggerFactory.getLogger(totalonline.class);

	public void onLoad()
	{
		//if(Config.ALLOW_ONLINE_PARSE)
		//{
		//ThreadPoolManager.getInstance().scheduleAtFixedRate(new updateOnline(), Config.FIRST_UPDATE*60000, Config.DELAY_UPDATE*60000);

		//}
	}

	private class updateOnline implements Runnable
	{
		public void run()
		{
			int members = getOnlineMembers();
			int offMembers = getOfflineMembers();
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("update online set online =?, offline = ? where 'index' =0");
				statement.setInt(1, members);
				statement.setInt(2, offMembers);
				statement.execute();
			}
			catch(SQLException e)
			{
				LOG.error("updateOnline: ", e);
			}
		}
	}

	//for future possibility of parsing names of players method is taking also name to array for init
	private int getOnlineMembers()
	{
		return GameObjectsStorage.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount();
	}

	private int getOfflineMembers()
	{
		int i = 0;
		for(Player player : GameObjectsStorage.getAllPlayersForIterate())
		{
			if(player.isInOfflineMode())
				i++;
		}

		return i;
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}