package l2f.gameserver.network.clientpackets;

import l2f.gameserver.Config;
import l2f.rGuard.ConfigProtection;
import l2f.gameserver.network.serverpackets.KeyPacket;
import l2f.gameserver.network.serverpackets.SendStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import l2f.rGuard.Protection;

public class ProtocolVersion extends L2GameClientPacket
{
	private static final Logger _log = LoggerFactory.getLogger(ProtocolVersion.class);

	private int protocol;
	private byte[] hwidData;
        private String _hwidHdd = "", _hwidMac = "", _hwidCPU = "";

        @Override
	protected void readImpl()
	{
		protocol = readD();
		
		if ((_buf.remaining() > 260))
		{
			hwidData = new byte[66];
			try
			{
				readB(hwidData);
                                if(ConfigProtection.ALLOW_GUARD_SYSTEM)
                                {
                                    _hwidHdd = readS();
                                    _hwidMac = readS();
                                    _hwidCPU = readS();
                                }
			}
			catch (RuntimeException e)
			{
				hwidData = null;
                                _log.info("-100");
			}
		}
	}

        @Override
	protected void runImpl()
	{
		if (protocol == -2)
		{
                    _log.info("-1");
			_client.closeNow(false);
			return;
		}
		else if (protocol == -3)
		{
			_log.info("Status request from IP : " + getClient().getIpAddr());
			getClient().close(new SendStatus());
			return;
		}
		else if (protocol < Config.MIN_PROTOCOL_REVISION || protocol > Config.MAX_PROTOCOL_REVISION)
		{
			_log.warn("Unknown protocol revision : " + protocol + ", client : " + _client);
			getClient().close(new KeyPacket(null));
			return;
		}
		
		if (Protection.isProtectionOn())
		{
			boolean isEverythingOk;
			
			try
			{
				isEverythingOk = setHwid();
			}
			catch (RuntimeException e)
			{
				isEverythingOk = false;
			}
			
			if (!isEverythingOk)
			{
                            _log.info("-2");
				_client.setSystemVersion(999);
				getClient().setHWID("TEMP_ERROR");
                                getClient().close(new KeyPacket(null));
			}
		}
		else
		{
                    _log.info("-3");
			_client.setSystemVersion(Config.LATEST_SYSTEM_VER);
			getClient().setHWID("NO-SMART-GUARD-ENABLED");
		}
		 _log.info("-200");
		sendPacket(new KeyPacket(_client.enableCrypt()));
	}
	
	private boolean setHwid()
	{
		if (!(ConfigProtection.ALLOW_GUARD_SYSTEM) && _hwidHdd=="" && _hwidMac=="" &&_hwidCPU =="")
                {
                    _log.info("-4");
                    return false;
                }
		_client.setSystemVersion(Config.LATEST_SYSTEM_VER);
		getClient().setHWID(_hwidCPU);
                _log.info("-5");
		return true;
	}

	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}