/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.services.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Future;
import l2f.commons.dbutils.DbUtils;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.scripts.ScriptFile;
import org.slf4j.LoggerFactory;


/**
 *
 * @author saxmog
 */
public class Reward
{
        private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Reward .class);
        static HashMap<Integer, Integer> _rewards = new HashMap();
        private static final String SELECT_REWARD_PLAYERS = "SELECT owner_id, item_id, payment_id, description, count FROM items_delayed WHERE flags=0";
        private static final String UPDATE_REWARD = "UPDATE items_delayed SET flags=1 WHERE payment_id=?";
	protected Future<?> _notifyThread = null;
        private static final Reward _instance = new Reward();
	public static final Reward getInstance()
	{
		return _instance;
	}

        private Reward() 
        {
            ThreadPoolManager.getInstance().scheduleAtFixedRate(new GiveReward(), 30000, 30000);
            LOG.info("Reward Service: loaded sucesfully");
        }

        private class GiveReward implements Runnable
	{
            
		@Override
		public void run()
		{
                    giveReward();
		}
	}
        public void giveReward()
        { 
            try
            {
                Connection con = null;
		PreparedStatement selectObjectStatement = null, selectL2topStatement = null, insertStatement = null;
		ResultSet rsetObject = null, rsetL2top = null;
		try
		{
                       
			con = DatabaseFactory.getInstance().getConnection();
			selectObjectStatement = con.prepareStatement(SELECT_REWARD_PLAYERS);
			rsetObject = selectObjectStatement.executeQuery();
                        int item_id,count,owner_id, payment_id;
                        String description;
			while(rsetObject.next())
                        {
                                owner_id = rsetObject.getInt("owner_id");
				item_id = rsetObject.getInt("item_id");
                                count = rsetObject.getInt("count");
                                payment_id = rsetObject.getInt("payment_id");
                                description = rsetObject.getString("description");
                                for (Player player : GameObjectsStorage.getAllPlayersForIterate())
                                {
                                   if(player.getObjectId() == owner_id)
                                   {
                                       player.getInventory().addItem(item_id, count, "DONATE");
                                       player.sendMessage(description);
                                       _rewards.put(payment_id, payment_id);
                                   }
                                }
                        }
                }
                catch (SQLException e)
		{
			LOG.error("Error while SELECT Rewards on Site", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, selectObjectStatement, rsetObject);
			DbUtils.closeQuietly(con, selectL2topStatement, rsetL2top);
			DbUtils.closeQuietly(con, insertStatement);
		}
                
                if(!_rewards.isEmpty())
                {
                    for (int rewards : _rewards.keySet())
                    {
                        Connection con1 = null;
                        PreparedStatement statement1 = null;
                        ResultSet rset1 = null;
                        try
                        {
                            con1 = DatabaseFactory.getInstance().getConnection();
                            statement1 = con1.prepareStatement(UPDATE_REWARD);
                            statement1.setInt(1, rewards);
                            statement1.execute();
                            statement1.close();
                        }
                        catch (SQLException e)
                        {
                            LOG.error("Error while uddate reward", e);
                        }
                        finally
                        {
                            DbUtils.closeQuietly(con1, statement1, rset1);
                        }
                    }
                }
            }
            catch(Exception ee)
            {
                LOG.info(String.valueOf(ee));
                String go="ERROR on givereward";
                for(StackTraceElement el : ee.getStackTrace())
                {
                    go+="\n";
                    go+=el.toString();
                }
                 LOG.info(go);
            }
        }
}
