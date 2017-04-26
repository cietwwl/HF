package ai.isle_of_prayer;

import instances.CrystalCaverns;
import l2f.gameserver.ai.DefaultAI;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.instances.NpcInstance;

public class EvasProtector extends DefaultAI
{
	public EvasProtector(NpcInstance actor)
	{
		super(actor);
		actor.setHasChatWindow(false);
	}

	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
		NpcInstance actor = getActor();
		if (skill.getSkillType() == Skill.SkillType.HEAL && actor.getReflection().getInstancedZoneId() == 10)
			((CrystalCaverns) actor.getReflection()).notifyProtectorHealed(actor);
		super.onEvtSeeSpell(skill, caster);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}