package services;

import l2f.commons.dbutils.DbUtils;
import l2f.gameserver.Config;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.model.Player;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.scripts.ScriptFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Account extends Functions implements ScriptFile
{
	
	public void CharToAcc()
	{
		Player player = (Player) getSelf();
		if (player == null)
			return;
		
		String append = "Transfer characters between accounts.<br>";
		append += "Price: " + Config.ACC_MOVE_PRICE + " " + ItemHolder.getInstance().getTemplate(Config.ACC_MOVE_ITEM).getName() + ".<br>";
		append += "Attention! When you transfer a character to another account, make sure are less than 7 characters on the account, or there might be unforeseen situations for which the Administration is not responsible.<br>";
		append += "Carefully transfer to enter a login, the administration does not return characters.";
		append += "You transfer your character " + player.getName() + ", on which account to transfer it ?";
		append += "<edit var=\"new_acc\" width=150>";
		append += "<button value=\"Move\" action=\"bypass -h scripts_services.Account:NewAccount $new_acc\" width=150 height=15><br>";
		show(append, player, null);
		
	}

	public void NewAccount(String[] name)
	{
		Player player = (Player) getSelf();
		if (player == null)
			return;
		
		if (player.getInventory().getCountOf(Config.ACC_MOVE_ITEM) < Config.ACC_MOVE_PRICE)
		{
			player.sendMessage("You have no " + Config.ACC_MOVE_PRICE + " " + ItemHolder.getInstance().getTemplate(Config.ACC_MOVE_ITEM));
			CharToAcc();
			return;
		}
		String _name = name[0];
		Connection con = null;
        Connection conGS = null;
		PreparedStatement offline = null;
        Statement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("SELECT `login` FROM `accounts` WHERE `login` = ?");
			offline.setString(1, _name);
			rs = offline.executeQuery();
			if (rs.next())
			{
				removeItem(player, Config.ACC_MOVE_ITEM, Config.ACC_MOVE_PRICE, "Account Move");
                conGS = DatabaseFactory.getInstance().getConnection();
			    statement = conGS.createStatement();
				statement.executeUpdate("UPDATE `characters` SET `account_name` = '" + _name + "' WHERE `char_name` = '" + player.getName() + "'");
				player.sendMessage("Character successfully transferred.");
				player.logout();
			}
			else
			{
				player.sendMessage("Introduced account not found.");
				CharToAcc();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		finally
		{
			DbUtils.closeQuietly(con, offline, rs);
            DbUtils.closeQuietly(conGS, statement);
		}
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}