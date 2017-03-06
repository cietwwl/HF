package l2f.rGuard.network;

import  l2f.commons.threading.RunnableImpl;
import  l2f.gameserver.GameTimeController;
import  l2f.gameserver.ThreadPoolManager;
import  l2f.gameserver.model.GameObjectsStorage;
import  l2f.gameserver.model.Player;
import  l2f.gameserver.network.GameClient;
import  l2f.gameserver.network.serverpackets.ServerClose;
import  l2f.rGuard.ConfigProtection;
import  l2f.rGuard.Protection;
import  l2f.rGuard.hwidmanager.HWIDBan;
import  l2f.rGuard.network.serverpackets.GameGuardQuery;
import  l2f.rGuard.network.serverpackets.SpecialString;

public final class ProtectionManager
{
	
	public static void SendSpecialSting(GameClient client)
	{
		if(Protection.isProtectionOn()){
			if(ConfigProtection.SHOW_PROTECTION_INFO_IN_CLIENT)
				client.sendPacket(new SpecialString(0, true, -1, ConfigProtection.PositionXProtectionInfoInClient, ConfigProtection.PositionYProtectionInfoInClient, ConfigProtection.ColorProtectionInfoInClient, "PROTECTION ON"));
			if(ConfigProtection.SHOW_NAME_SERVER_IN_CLIENT)
				client.sendPacket(new SpecialString(1, true, -1, ConfigProtection.PositionXNameServerInfoInClient, ConfigProtection.PositionYNameServerInfoInClient, ConfigProtection.ColorNameServerInfoInClient, (client.getActiveChar().isLangRus() ? "Сервер: " : "Server: ") + ConfigProtection.NameServerInfoInClient));
			if(ConfigProtection.SHOW_REAL_TIME_IN_CLIENT)
				client.sendPacket(new SpecialString(15, true, -1, ConfigProtection.PositionXRealTimeInClient, ConfigProtection.PositionYRealTimeInClient, ConfigProtection.ColorRealTimeInClient, client.getActiveChar().isLangRus() ? "Реальное время: " : "Real time: "));
			sendToClient(client.getActiveChar());
			if(ConfigProtection.ALLOW_SEND_GG_REPLY)
				sendGGReply(client);}}

	public static void sendToClient(Player client)
	{
		if(ConfigProtection.SHOW_ONLINE_IN_CLIENT)
			client.sendPacket(new SpecialString(2, true, -1, ConfigProtection.PositionXOnlineInClient, ConfigProtection.PositionYOnlineInClient, ConfigProtection.ColorOnlineInClient, (client.isLangRus() ? "Онлайн: " : "Online: ") + GameObjectsStorage.getAllPlayersCount()));
		if(ConfigProtection.SHOW_SERVER_TIME_IN_CLIENT)
		{
			String strH, strM;
			int h = GameTimeController.getInstance().getGameHour();
			int m = GameTimeController.getInstance().getGameMin();
			String nd;
			if(GameTimeController.getInstance().isNowNight())
				nd = client.isLangRus() ? "Ночь." : "Night.";
			else
				nd = client.isLangRus() ? "День." : "Day.";
			if(h < 10)
				strH = "0" + h;
			else
				strH = "" + h;
			if(m < 10)
				strM = "0" + m;
			else
				strM = "" + m;
			client.sendPacket(new SpecialString(3, true, -1, ConfigProtection.PositionXServerTimeInClient, ConfigProtection.PositionYServerTimeInClient, ConfigProtection.ColorServerTimeInClient, (client.isLangRus() ? "Игровое время: " : "Game time: ") + strH + ":" + strM + " (" + nd + ")"));
		}
		if(ConfigProtection.SHOW_PING_IN_CLIENT)
			client.sendPacket(new SpecialString(14, true, -1, ConfigProtection.PositionXPingInClient, ConfigProtection.PositionYPingInClient, ConfigProtection.ColorPingInClient, client.isLangRus() ? "Пинг: " : "Ping: "));
		scheduleSendPacketToClient(ConfigProtection.TIME_REFRESH_SPECIAL_STRING, client);
	}

	public static void OffMessage(Player client){
		if(client != null){
			client.sendPacket(new SpecialString(0, false, -1, ConfigProtection.PositionXProtectionInfoInClient, ConfigProtection.PositionYProtectionInfoInClient, 0xFF00FF00, ""));
			client.sendPacket(new SpecialString(1, false, -1, ConfigProtection.PositionXNameServerInfoInClient, ConfigProtection.PositionYNameServerInfoInClient, 0xFF00FF00, ""));
			client.sendPacket(new SpecialString(2, false, -1, ConfigProtection.PositionXOnlineInClient, ConfigProtection.PositionYOnlineInClient, 0xFF00FF00, ""));
			client.sendPacket(new SpecialString(3, false, -1, ConfigProtection.PositionXServerTimeInClient, ConfigProtection.PositionYServerTimeInClient, 0xFF00FF00, ""));
			client.sendPacket(new SpecialString(14, false, -1, ConfigProtection.PositionXPingInClient, ConfigProtection.PositionYPingInClient, 0xFF00FF00, ""));
			client.sendPacket(new SpecialString(15, false, -1, ConfigProtection.PositionXRealTimeInClient, ConfigProtection.PositionYRealTimeInClient, 0xFF00FF00, ""));
		}return;}

	public static void scheduleSendPacketToClient(long time, final Player client){
		if(time <= 0){
			OffMessage(client);
			return;}

		ThreadPoolManager.getInstance().schedule(new RunnableImpl(){
			@Override
			public void runImpl() throws Exception
			{sendToClient(client);}}, time);}

	public static void sendGGReply(GameClient client)
	{
		if(client != null && client.getActiveChar() != null)
		{
			client.sendPacket(new GameGuardQuery());
			if(ConfigProtection.ALLOW_SEND_GG_REPLY)
				scheduleSendGG(ConfigProtection.TIME_SEND_GG_REPLY * 1000, client);
		}
	}

	public static void scheduleSendGG(long time, final GameClient client)
	{
		if(time <= 0)
			return;

		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl() throws Exception
			{
				if (client != null && client.getActiveChar() != null && !client.isGameGuardOk())
				{
					_log.info("Client "+client+" failed to reply GameGuard query and is being kicked!");
					client.closeNow(true);
				}
				if (HWIDBan.getInstance().checkFullHWIDBanned(client))
				{
					_log.info("Client "+client+" is banned. Kicked! |HWID: " + client.getHWID() + " IP: " + client.getIpAddr());
					client.close(ServerClose.STATIC);
				}
				sendGGReply(client);
			}
		}, time);
	}
}