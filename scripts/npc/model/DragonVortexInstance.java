package npc.model;

import l2f.commons.util.Rnd;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.templates.npc.NpcTemplate;
import l2f.gameserver.utils.ItemFunctions;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.NpcUtils;

import java.util.ArrayList;
import java.util.List;


public final class DragonVortexInstance extends NpcInstance
{
	private static final long serialVersionUID = -5085759479441962057L;
	private final int[] bosses = { 25718, 25719, 25720, 25721, 25722, 25723, 25724 };
	private NpcInstance boss;
	
	private List<NpcInstance> bosses_list = new ArrayList<NpcInstance>();
	private List<NpcInstance> temp_list = new ArrayList<NpcInstance>();
	
	public DragonVortexInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!canBypassCheck(player, this))
			return;

		if (command.startsWith("request_boss"))
		{

			if (ItemFunctions.getItemCount(player, 17248) > 0)
			{
				if (validateBosses())
				{
					ItemFunctions.removeItem(player, 17248, 1, true, "DragonVortex");
					boss = NpcUtils.spawnSingle(bosses[Rnd.get(bosses.length)], Location.coordsRandomize(getLoc(), 300, 600), getReflection());
					bosses_list.add(boss);
					showChatWindow(player, "default/32871-1.htm");
				}
				else
					showChatWindow(player, "default/32871-3.htm");
				
			}
			else
				showChatWindow(player, "default/32871-2.htm");
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private boolean validateBosses()
	{
		if (bosses_list == null || bosses_list.isEmpty())
			return true;
			
		temp_list.addAll(bosses_list);	
		
		for (NpcInstance npc : temp_list)
		{
			if (npc == null || npc.isDead())
				bosses_list.remove(npc);
		}
		
		temp_list.clear();
		
		if (bosses_list.size() >= 200)
			return false;
			
		return true;	
	}
}