package l2f.rGuard;

import java.io.IOException;

import  l2f.gameserver.handler.admincommands.AdminCommandHandler;
import  l2f.rGuard.crypt.BlowfishEngine;
import  l2f.rGuard.hwidmanager.HWIDAdminBan;
import  l2f.rGuard.hwidmanager.HWIDBan;
import  l2f.rGuard.hwidmanager.HWIDManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Protection
{
	private static final Logger _log = LoggerFactory.getLogger(Protection.class);
	private static byte[] _key = new byte[16];

	public static void Init()
	{
		ConfigProtection.load();
		if(isProtectionOn())
		{_log.info("************[ Castiel Defense System: Loading... ]*************");
			HWIDBan.getInstance();
			HWIDManager.getInstance();
		//	ProtectManager.getInstance();
			AdminCommandHandler.getInstance().registerAdminCommandHandler(new HWIDAdminBan());
		_log.info("************[ Castiel Defense System: ON ]*************");}
	}

	public static boolean isProtectionOn()
	{
		if(ConfigProtection.ALLOW_GUARD_SYSTEM)
			return true;
		return false;
	}

	public static byte[] getKey(byte[] key)
	{
		byte[] bfkey = {-78,
		12,
		74,
		17,
		-22,
		47,
		73,
		11,
		97,
		-71,
		12,
		86,
		-34,
		39,
		75,
		14};
		try
		{
			BlowfishEngine bf = new BlowfishEngine();
			bf.init(true, bfkey);
			bf.processBlock(key, 0, _key, 0);
			bf.processBlock(key, 8, _key, 8);
		}
		catch(IOException e)
		{
			_log.info("Bad key!!!");
		}
		return _key;
	}
}