package services;
import l2f.gameserver.Config;
import l2f.gameserver.cache.Msg;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.Functions;

public class TitleColor extends Functions
{
	public void list()
	{
		Player player = getSelf();
		if (player == null)
			return;
		if (!Config.SERVICES_CHANGE_Title_COLOR_ENABLED)
		{
				show("Service is disabled.", player);
				return;
		}
		StringBuilder append = new StringBuilder();
		append.append("You can purchase the title for color: ").append(Config.SERVICES_CHANGE_Title_COLOR_PRICE).append(" ").append(ItemHolder.getInstance().getTemplate(Config.SERVICES_CHANGE_Title_COLOR_ITEM).getName()).append(".");
		append.append("<br>Available colors:<br>");
		for (String color : Config.SERVICES_CHANGE_Title_COLOR_LIST)
			append.append("<br><a action=\"bypass -h scripts_services.TitleColor:change ").append(color).append("\"><font color=\"").append(color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).append("\">").append(color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).append("</font></a>");
		append.append("<br><a action=\"bypass -h scripts_services.TitleColor:change FFFFFF\"><font color=\"FFFFFF\">Return standard (Free)</font></a>");
		show(append.toString(), player, null);
	}

	public void change(String[] param)
	{
		Player player = getSelf();
		if (player == null)
			return;
		if (!Config.SERVICES_CHANGE_Title_COLOR_ENABLED)
		{
				show("Service is disabled.", player);
				return;
		}
		if (param[0].equalsIgnoreCase("FFFFFF"))
		{
			player.setTitleColor(Integer.decode("0xFFFFFF"));
			player.broadcastUserInfo(true);
			return;
		}

		if (player.getInventory().destroyItemByItemId(Config.SERVICES_CHANGE_Title_COLOR_ITEM, Config.SERVICES_CHANGE_Title_COLOR_PRICE, "TitleColor"))
		{
			player.setTitleColor(Integer.decode("0x" + param[0]));
			player.broadcastUserInfo(true);
		}
		else if (Config.SERVICES_CHANGE_Title_COLOR_ITEM == 57)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			player.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
	}
}