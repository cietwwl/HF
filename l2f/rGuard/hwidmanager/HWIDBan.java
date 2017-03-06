package l2f.rGuard.hwidmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import  l2f.commons.dbutils.DbUtils;
import  l2f.gameserver.database.DatabaseFactory;
import  l2f.gameserver.network.GameClient;

public class HWIDBan
{
	private static HWIDBan _instance;

	private static Map<Integer, HWIDBanList> _lists;

	public static HWIDBan getInstance()
	{
		if (_instance == null)
		{
			_instance = new HWIDBan();
		}
		return _instance;
	}

	public static void reload()
	{
		_instance = new HWIDBan();
	}
	
	public HWIDBan()
	{
		_lists = new HashMap<Integer, HWIDBanList>();
		load();
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		String HWID = "";
		int counterHWIDBan = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM hwid_bans");
			rset = statement.executeQuery();
			while (rset.next())
			{

				HWID = rset.getString("HWID");
				HWIDBanList hb = new HWIDBanList(counterHWIDBan);
				hb.setHWIDBan(HWID);
				_lists.put(counterHWIDBan, hb);
				counterHWIDBan++;
			}
		}
		catch (Exception E)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public boolean checkFullHWIDBanned(GameClient client)
	{
		if (_lists.size() == 0)
		{
			return false;
		}
		for (int i = 0; i < _lists.size(); i++)
		{
			if (_lists.get(i).getHWID().equals(client.getHWID()))
			{
				return true;
			}
		}
		return false;
	}

	public static int getCountHWIDBan()
	{
		return _lists.size();
	}

	public static void addHWIDBan(GameClient client)
	{
		String HWID = client.getHWID();
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO hwid_bans SET HWID=?");
			statement.setString(1, HWID);
			statement.execute();
		}
		catch (Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}