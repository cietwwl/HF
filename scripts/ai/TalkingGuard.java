package ai;

import l2f.commons.util.Rnd;
import l2f.gameserver.Config;
import l2f.gameserver.ai.CtrlIntention;
import l2f.gameserver.ai.Guard;
import l2f.gameserver.geodata.GeoEngine;
import l2f.gameserver.model.AggroList;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Playable;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.scripts.Functions;

public class TalkingGuard extends Guard
{
	private boolean _crazyState;
	private long _lastAggroSay;
	private long _lastNormalSay;
	private static final int _crazyChance = Config.TalkGuardChance;
	private static final int _sayNormalChance = Config.TalkNormalChance;
	private static final long _sayNormalPeriod = Config.TalkNormalPeriod * 6000;
	private static final long _sayAggroPeriod = Config.TalkAggroPeriod * 6000;

	// Фразы, которые может произнести гвард, когда начинает атаковать пк
	private static final String[] _sayAggroText =
	{
		"{name}, даже не смей уходить гаденыш, я убью тебя!",
		"{name}, Я готов убивать, motherfucker!",
		"Ла ла ла, я сошел с ума, я тот, кто тебя прикончит!",
		"Я убил стольких, что тебя убить будет проще простого! Ты следующий, {name}, тебе конец!",
		"I'm terrified of flying on the wings of the night! I'm chewing gum sticking to your base! I. .. In short, {name}, Now I'm going to kill you!",
		"I fear, trembling in the night! I dodgy lock the basement of justice! I have a favorite good luck! I am a Black Guard!",
		"Wow, my future victim. This is me talking to you, {name}! Do not pretend that you're not in the business!",
		"Hooray! For their country, for all my brethren! Prepare to die, {name}!",
		"{name}, stand and deliver?",
		"{name}, just die, not to complicate my life!",
		"{name}, how do you like to die? Quickly and easily, or slowly and painfully?",
		"{name}, pvp or pissed?",
		"{name}, I'll kill you softly.",
		"{name}, I'll tear as Tuzik warmer!",
		"Prepare to die, {name}!",
		"{name}, You fight like a girl!",
		"{name}, pray before death! Although ... do not have time!"
	};
	// Фразы, которые может произнести гвард, адресуя их проходящим мимо игрокам мужского пола
	private static final String[] _sayNormalTextM =
	{
		"{name}, cho is?",
		"{name}, hello!",
		"{name}, hi!",
		"{name}, hello nasty.",
		"{name}, Give weapon for a moment, I want to make a screen.",
		"{name}, successful hunt.",
		"{name}, what force, brother?",
		"{name}, more kills you.",
		"{name}, you give me nightmares dreamed.",
		"{name}, I know you - you have long wanted for the murder of innocent monsters.",
		"{name}, pvp or pissed?",
		"{name}, you have a purse dropped.",
		"{name}, I will not go with you on a date, do not even ask.",
		"Smack all in this chat."
	};
	// Фразы, которые может произнести гвард, адресуя их проходящим мимо игрокам женского пола
	private static final String[] _sayNormalTextF =
	{
		"{name}, hello beautiful.",
		"{name}, Wow, what are you ... e ... eyes.",
		"{name}, do not want to hang out with a real macho?",
		"{name}, hi!",
		"{name}, let me touch ... e ... Well in general forbid certain things touch.",
		"{name}, No woman is a thing - to kill enemies.",
		"{name}, you have the upper hand broke, do not shine ... eyes.",
		"{name}, oh what buns...",
		"{name}, oh what legs...",
		"{name}, yes but you babe.",
		"{name}, wah, what a woman, I would have a.",
		"{name}, and what are you doing tonight?",
		"{name}, You agree that from the point of banal erudition, not every individual is able to locally-selected tendency to ignore the potential of emotions and parity allotsirovat ambivalent quanta logistics extractable given anthropomorphic heuristic genesis?",
		"{name}, offer his hand and heart. And after the wedding purse."
	};

	public TalkingGuard(NpcInstance actor)
	{
		super(actor);
		MAX_PURSUE_RANGE = 600;
		_crazyState = false;
		_lastAggroSay = 0;
		_lastNormalSay = 0;
	}

	@Override
	protected void onEvtSpawn()
	{
		_lastAggroSay = 0;
		_lastNormalSay = 0;
		_crazyState = Rnd.chance(_crazyChance) ? true : false;
		super.onEvtSpawn();
	}

	@Override
	protected boolean checkAggression(Creature target, boolean avoidAttack)
	{
		if (_crazyState)
		{
			NpcInstance actor = getActor();
			Player player = target.getPlayer();
			if (actor == null || actor.isDead() || player == null)
				return false;
			if (player.isGM())
				return false;
			if (Rnd.chance(_sayNormalChance))
			{
				if (target.isPlayer() && target.getKarma() <= 0 && (_lastNormalSay + _sayNormalPeriod < System.currentTimeMillis()) && actor.isInRange(target, 250L))
				{
					Functions.npcSay(actor, target.getPlayer().getSex() == 0 ? _sayNormalTextM[Rnd.get(_sayNormalTextM.length)].replace("{name}", target.getName()) : _sayNormalTextF[Rnd.get(_sayNormalTextF.length)].replace("{name}", target.getName()));
					_lastNormalSay = System.currentTimeMillis();
				}
			}
			if (target.getKarma() <= 0)
				return false;
			if (getIntention() != CtrlIntention.AI_INTENTION_ACTIVE)
				return false;
			if (_globalAggro < 0L)
				return false;
			AggroList.AggroInfo ai = actor.getAggroList().get(target);
			if (ai != null && ai.hate > 0)
			{
				if (!target.isInRangeZ(actor.getSpawnedLoc(), MAX_PURSUE_RANGE))
					return false;
			}
			else if (!target.isInRangeZ(actor.getSpawnedLoc(), 600))
				return false;
			if (target.isPlayable() && !canSeeInSilentMove((Playable) target))
				return false;
			if (!GeoEngine.canSeeTarget(actor, target, false))
				return false;
			if (target.isPlayer() && ((Player) target).isInvisible())
				return false;

			if (!avoidAttack)
			{
				if ((target.isSummon() || target.isPet()) && target.getPlayer() != null)
					actor.getAggroList().addDamageHate(target.getPlayer(), 0, 1);
				actor.getAggroList().addDamageHate(target, 0, 2);
				startRunningTask(2000);
				if (_lastAggroSay + _sayAggroPeriod < System.currentTimeMillis())
				{
					Functions.npcSay(actor, _sayAggroText[Rnd.get(_sayAggroText.length)].replace("{name}", target.getPlayer().getName()));
					_lastAggroSay = System.currentTimeMillis();
				}

				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
			return true;
		}
		else
		{
			super.checkAggression(target, avoidAttack);
		}
		return false;
	}
}