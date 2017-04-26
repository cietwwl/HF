package npc.model.residences.castle;

import java.util.HashSet;
import java.util.Set;

import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Spawner;
import l2f.gameserver.model.instances.residences.SiegeToggleNpcInstance;
import l2f.gameserver.templates.npc.NpcTemplate;

public class CastleControlTowerInstance extends SiegeToggleNpcInstance
{
	private static final long serialVersionUID = -7516474747580672571L;
	private Set<Spawner> _spawnList = new HashSet<Spawner>();

	public CastleControlTowerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onDeathImpl(Creature killer)
	{
		for (Spawner spawn : _spawnList)
			spawn.stopRespawn();
		_spawnList.clear();
	}

	@Override
	public void register(Spawner spawn)
	{
		_spawnList.add(spawn);
	}
}