package l2f.rGuard.hwidmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import  l2f.commons.dbutils.DbUtils;
import  l2f.gameserver.database.DatabaseFactory;
import  l2f.gameserver.network.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HWIDManager
{
	private static final Logger _log = LoggerFactory.getLogger(HWIDManager.class.getName());

	private static HWIDManager _instance;
	static Map<Integer, HWIDInfoList> _listHWID;

	public static HWIDManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new HWIDManager();
		}
		return _instance;
	}

	public HWIDManager()
	{
		_listHWID = new HashMap<Integer, HWIDInfoList>();
		load();
		_log.info("HWIDManager: Loaded " + _listHWID.size() + " HWIDs");
	}

	public static void reload()
	{
		_instance = new HWIDManager();
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM hwid_info");
			rset = statement.executeQuery();
			int counterHWIDInfo = 0;
			while (rset.next())
			{
				final HWIDInfoList hInfo = new HWIDInfoList(counterHWIDInfo);
				hInfo.setHwids(rset.getString("HWID"));
				hInfo.setLogin(rset.getString("Account"));
				_listHWID.put(counterHWIDInfo, hInfo);
				counterHWIDInfo++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static void updateHWIDInfo(GameClient client)
	{
		int counterHWIDInfo = _listHWID.size();
		boolean isFound = false;
		for (int i = 0; i < _listHWID.size(); i++)
		{
			if (_listHWID.get(i).getHWID().equals(client.getHWID()))
			{
				isFound = true;
				counterHWIDInfo = i;
				break;
			}
		}
		Connection con = null;
		PreparedStatement statement = null;
		final HWIDInfoList hInfo = new HWIDInfoList(counterHWIDInfo);
		hInfo.setHwids(client.getHWID());
		hInfo.setLogin(client.getLogin());
		_listHWID.put(counterHWIDInfo, hInfo);
		if (isFound)
		{
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE hwid_info SET Account=? WHERE HWID=?");
				statement.setString(1, client.getLogin());
				statement.setString(2, client.getHWID());
				statement.execute();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
		else
		{
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO hwid_info (HWID, Account) values (?,?)");
				statement.setString(1, client.getHWID());
				statement.setString(1, client.getLogin());
				statement.execute();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
}