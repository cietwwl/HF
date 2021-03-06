package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.ExBR_ProductList;
import l2f.gameserver.scripts.Functions;


public class Donate extends Functions implements IVoicedCommandHandler
{
  private static final String[] COMMANDS = { "donate" };
  
    public Donate() {}
    @Override
    public boolean useVoicedCommand(String command, Player activeChar, String args)
    {
        String html = "donate/60000.htm";
    
        String dialog = HtmCache.getInstance().getNotNull(html, activeChar);
    
        show(dialog, activeChar);
        return true;
    }
  
    @Override
    public String[] getVoicedCommandList()
    {
        return COMMANDS;
    }
}