package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.scripts.Functions;


public class Obt extends Functions implements IVoicedCommandHandler
{
  private static final String[] COMMANDS = { "obt" };
  
    public Obt() {}
    @Override
    public boolean useVoicedCommand(String command, Player activeChar, String args)
    {
        String html = "default/53000.htm";
    
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