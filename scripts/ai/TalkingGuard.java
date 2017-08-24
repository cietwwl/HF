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
"{name}, Я готов убивать, ублюдок!",
"{name},Ла ла ла, я сошел с ума, я тот, кто тебя прикончит!",
"Я убил стольких, что тебя убить будет проще простого! Ты следующий, {name}, конец тебе!",
"Я ужас, летящий на крыльях ночи! Я жвачка прилипла к вашей базе! И. .. короче, {name}, теперь я собираюсь убить тебя!",
"{name}, Я боюсь, дрожат коленки! Я хитроумный замок от подвала правосудия! У меня есть любимая дача! Я Серый Страж! Смотрел игру престолов?",
 "Ты, моя будущая жертва. Это я говорю тебе, {name}! Не делайте вид, что Ты не в понимаешь!",
"Ура! За свою страну, за всех моих братьев! Готовься к смерти, {name}!",
"{name}, стоишь и ссышь?",
"{name}, просто умери, не усложняй свою жизнь!",
"{name}, как вы хотели бы умереть? Быстро и легко, или медленно и мучительно?",
"{name}, пвп или зассал?",
"{name}, я тебя убью мягко.",
"{name}, я порву как Тузик грелку!",
"Готовься к смерти, {name}!",
"{name}, ты дерешься, как бабка на базаре!",
"{name}, молись перед смертью! Хотя ... не успеешь!"
	};
	// Фразы, которые может произнести гвард, адресуя их проходящим мимо игрокам мужского пола
	private static final String[] _sayNormalTextM =
	{
		"{name}, чо надо?",
"{name}, привет!",
"{name}, привет!",
"{name}, привет противный",
"{name}, дай оружие на сек, я хочу сделать скрин.",
"{name}, удачной охоты.",
"{name}, в чем сила, брат?",
"{name}, больше умираешь ты, чем я.",
"{name}, мне кошмары снились.",
"{name}, я знаю, тебя уже давно разыскивают за убийства невинных монстров.",
"{name}, пвп или зассал?",
"{name}, у вас кошелек упал.",
"{name}, я не пойду с тобой на свидание, даже не проси.",
"Чмоки чпоки всем в этом чате"
	};
	// Фразы, которые может произнести гвард, адресуя их проходящим мимо игрокам женского пола
	private static final String[] _sayNormalTextF =
	{
		"{name}, привет, красавица",
"{name}, ух, какая ты ... э ... глаза",
"{name}, не хочешь потусоваться с настоящим мачо?",
"{name}, привет красавица!",
"{name}, дай мне прикоснуться к ... э ... Ну в общем запрещаю определенные вещи трогать.",
"{name}, не женское это дело - убивать врагов.",
"{name}, ногти сломала, не свети ... в глаза.",
"{name}, ой, какие плюшки...",
"{name}, ой какие ножки...",
"{name}, да, но ты круче, детка.",
"{name}, вах, какая женщина, я бы.",
"{name}, а что ты делаешь сегодня вечером?",
 "{name}, Вы согласитесь, что с точки зрения банальной эрудиции, не каждый индивидум способен игнорировать потенциальные эмоции и четко аллотсировать амбивалентные кванты логистики с учетом экстрагирования антропоморфного эвристического генезиса?",
"{name}, предложить бы руку и сердце. Да вот после свадьбы кошелек жалко"
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