package l2f.gameserver.model.entity.events.fightclubmanager;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.threading.RunnableImpl;
import l2f.commons.util.Rnd;
import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.data.xml.holder.EventHolder;
import l2f.gameserver.listener.actor.player.OnAnswerListener;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.ClassId;
import l2f.gameserver.model.entity.events.EventType;
import l2f.gameserver.model.entity.events.GlobalEvent;
import l2f.gameserver.model.entity.events.impl.AbstractFightClub;
import l2f.gameserver.model.entity.events.impl.DuelEvent;
import l2f.gameserver.model.entity.olympiad.Olympiad;
import l2f.gameserver.model.instances.SchemeBufferInstance;
import l2f.gameserver.network.serverpackets.*;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.utils.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FightClubEventManager
{

	public enum CLASSES
	{
		FIGHTERS(13113, ClassId.duelist, ClassId.dreadnought, ClassId.titan, ClassId.grandKhauatari, ClassId.maestro, ClassId.doombringer, ClassId.maleSoulhound, ClassId.femaleSoulhound),
		TANKS(13112, ClassId.phoenixKnight, ClassId.hellKnight, ClassId.evaTemplar, ClassId.shillienTemplar, ClassId.trickster),
		ARCHERS(13114, ClassId.sagittarius, ClassId.moonlightSentinel, ClassId.ghostSentinel, ClassId.fortuneSeeker),
		DAGGERS(13114, ClassId.adventurer, ClassId.windRider, ClassId.ghostHunter),
		MAGES(13116, ClassId.archmage, ClassId.soultaker, ClassId.mysticMuse, ClassId.stormScreamer),
		SUMMONERS(13118, ClassId.arcanaLord, ClassId.elementalMaster, ClassId.spectralMaster),
		HEALERS(13115, ClassId.cardinal, ClassId.evaSaint, ClassId.shillienSaint, ClassId.dominator),
		SUPPORTS(13117, ClassId.hierophant, ClassId.swordMuse, ClassId.spectralDancer, ClassId.doomcryer, ClassId.judicator);

		private int _transformId;
		private ClassId[] _classes;

		private CLASSES(int transformId, ClassId... ids)
		{
			_transformId = transformId;
			_classes = ids;
		}

		public ClassId[] getClasses()
		{
			return _classes;
		}

		public int getTransformId()
		{
			return _transformId;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(FightClubEventManager.class);
	private static FightClubEventManager _instance;

	public static final Location RETURN_LOC = new Location(83208, 147672, -3494, 0);
	public static final int FIGHT_CLUB_BADGE_ID = 6673;
	public static final String BYPASS = "_fightclub";

	private final Map<Integer, AbstractFightClub> _activeEvents = new ConcurrentHashMap<>();
	private final List<FightClubGameRoom> _rooms = new CopyOnWriteArrayList<>();
	private final boolean _shutDown = false;
	private AbstractFightClub _nextEvent = null;
	private AbstractFightClub _currentEvent = null;

	public FightClubEventManager()
	{
		if(Config.ALLOW_FIGHT_CLUB)
		{
			if(Config.EVENT_RANDOM_TASK)
				ThreadPoolManager.getInstance().schedule(new RandomEventRunThread(null), 60000L); //run thread after 60 sec
			else
				startAutoEventsTasks();
		}
	}

	public boolean serverShuttingDown()
	{
		return _shutDown;
	}

	/*
	 * Player
	 */

	/**
	 * Looking for room, adding player and sending message Event MUST exist in one of the Rooms already!
	 * @param player player joining event
	 * @param event event to join
	 */
	public void signForEvent(Player player, AbstractFightClub event)
	{
		FightClubGameRoom roomFound = null;

		for(FightClubGameRoom room : getEventRooms(event))
			if(room.getSlotsLeft() > 0)
			{
				roomFound = room;
				break;
			}

		if(roomFound == null)
		{
			AbstractFightClub duplicatedEvent = prepareNewEvent(event);
			roomFound = createRoom(duplicatedEvent);
		}

		roomFound.addAlonePlayer(player);

		player.sendMessage("Вы успешно зарегистрировались в ивенте: " + event.getName() + " !");
	}

	/**
	 * Checking if player can participate(all conditions) Checking if registration is open and if player isn't participated yet Signing for event
	 * @param player player to participate
	 * @param event event to participate
	 */
	public void trySignForEvent(Player player, AbstractFightClub event, boolean checkConditions)
	{
		if(checkConditions && !canPlayerParticipate(player, true, false)){ return; }

		if(!isRegistrationOpened(event))
		{
			player.sendMessage("You cannot participate in " + event.getName() + " right now!");
		}
		else if(isPlayerRegistered(player))
		{
			player.sendMessage("You are already registered in event!");
		}
		else
		{
			signForEvent(player, event);
		}
	}

	/**
	 * Removing player from every room, that he is participated in. Sending Message
	 * @param player to participate
	 */
	public void unsignFromEvent(Player player)
	{
		for(FightClubGameRoom room : _rooms)
			if(room.containsPlayer(player))
			{
				room.leaveRoom(player);
			}

		player.sendMessage("You were unregistered from Event!");
	}

	/**
	 * Is it still possible to Register Players for the Event?
	 * @param event event to rehister
	 * @return registration opened
	 */
	public boolean isRegistrationOpened(AbstractFightClub event)
	{
		for(FightClubGameRoom room : _rooms)
			if(room.getGame() != null && room.getGame().getEventId() == event.getEventId())
				return true;
		return false;
	}

	/**
	 * Is player registered to any event at the moment? Checking by Player and HWID
	 * @param player to check
	 * @return is registered
	 */
	public boolean isPlayerRegistered(Player player)
	{
		if(player.isInFightClub())
			return true;

		for(FightClubGameRoom room : _rooms)
			for(Player iPlayer : room.getAllPlayers())
				if(iPlayer.equals(player) || Config.FIGHT_CLUB_HWID_CHECK && iPlayer.getHWID().equals(player.getHWID()))
					return true;
		return false;
	}

	/*
	 * Rooms
	 */

	public void startEventCountdown(AbstractFightClub event)
	{
		if(!Config.ALLOW_FIGHT_CLUB)
			return;

		FightClubLastStatsManager.getInstance().clearStats();
		_nextEvent = event;

		final AbstractFightClub duplicatedEvent = prepareNewEvent(event);
		createRoom(duplicatedEvent);

		sendToAllMsg(duplicatedEvent, "Регистрация на ивент " + duplicatedEvent.getName() + " началась!");
		sendEventInvitations(event);

		ThreadPoolManager.getInstance().schedule(new Runnable(){
			@Override
			public void run()
			{
				// After 2 minutes
				sendToAllMsg(duplicatedEvent, duplicatedEvent.getName() + " начнется через 3 минуты!");
				try
				{
					Thread.sleep(3 * 60000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				// After 3 minutes
				sendToAllMsg(duplicatedEvent, duplicatedEvent.getName() + " начнется через 1 минуту!");
				notifyConditions(duplicatedEvent);
				try
				{
					Thread.sleep(45000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				// After 45 seconds
				sendToAllMsg(duplicatedEvent, duplicatedEvent.getName() + " начнется через 15 секунд!");
				notifyConditions(duplicatedEvent);
				try
				{
					Thread.sleep(15000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				// After 15 seconds
				sendToAllMsg(duplicatedEvent, duplicatedEvent.getName() + " Начался!");

				if(!Config.ALLOW_FIGHT_CLUB || Config.FIGHT_CLUB_DISALLOW_EVENT == duplicatedEvent.getEventId())// Protection for my mistakes
					return;
				startEvent(duplicatedEvent);
			}
		}, 2 * 60000L);
	}

	/**
	 * Checking {@link #canPlayerParticipate(l2f.gameserver.model.Player, boolean, boolean)} for every player participated in event. Sending Message, checking all condtions
	 * @param event to check
	 */
	private void notifyConditions(AbstractFightClub event)
	{
		for(FightClubGameRoom room : getEventRooms(event))
		{
			for(Player player : room.getAllPlayers())
			{
				canPlayerParticipate(player, true, false);
			}
		}
	}

	/**
	 * Creating event from every Room
	 * @param event
	 */
	private void startEvent(AbstractFightClub event)
	{
		List<FightClubGameRoom> eventRooms = getEventRooms(event);

		if(Config.FIGHT_CLUB_EQUALIZE_ROOMS)
			equalizeRooms(eventRooms);

		for(FightClubGameRoom room : eventRooms)
		{
			_rooms.remove(room);
			if(room.getPlayersCount() < 2)
				continue;
			room.getGame().prepareEvent(room);
		}
	}

	/**
	 * Equilizing players count in All rooms in List
	 * @param eventRooms rooms to equalize
	 */
	private void equalizeRooms(Collection<FightClubGameRoom> eventRooms)
	{
		// getting all players count
		double players = 0.0;
		for(FightClubGameRoom room : eventRooms)
			players += room.getPlayersCount();

		// Getting average
		double average = players / eventRooms.size();
		// Getting players to change room and removing
		List<Player> playersToChange = new ArrayList<>();

		for(FightClubGameRoom room : eventRooms)
		{
			int before = room.getPlayersCount();
			int toRemove = room.getPlayersCount() - (int) Math.ceil(average);
			for(int i = 0; i < toRemove; i++)
			{
				Player player = room.getAllPlayers().get(0);
				room.leaveRoom(player);
				playersToChange.add(player);
			}
			LOG.info("Equalizing FC Room, before:" + before + " toRemove:" + toRemove + " after:" + room.getPlayersCount() + " to Change:" + playersToChange.size());
		}

		// Adding to other room
		for(FightClubGameRoom room : eventRooms)
		{
			int toAdd = (int) Math.floor(average) - room.getPlayersCount();

			for(int i = 0; i < toAdd; i++)
			{
				Player player = playersToChange.remove(0);
				room.addAlonePlayer(player);
			}
			LOG.info("Equalizing FC Room, Final:" + room.getPlayersCount());
		}
	}

	/**
	 * Getting All rooms where Game is same type as event
	 * @param event type of the event
	 * @return all rooms with type of the event
	 */
	private List<FightClubGameRoom> getEventRooms(AbstractFightClub event)
	{
		List<FightClubGameRoom> eventRooms = new ArrayList<>();

		for(FightClubGameRoom room : _rooms)
			if(room.getGame() != null && room.getGame().getEventId() == event.getEventId())
				eventRooms.add(room);

		return eventRooms;
	}

	/**
	 * Sending "Would you like to join XXX event?" Yes/No invitation, to event player in game that meets criteria
	 * @param event event player have to join
	 */
	private void sendEventInvitations(AbstractFightClub event)
	{
		for(Player player : GameObjectsStorage.getAllPlayersForIterate())
			if(canPlayerParticipate(player, false, true) && (player.getEvent(AbstractFightClub.class) == null))
			{
				player.ask(new ConfirmDlg(SystemMsg.S1, 60000).addString("Желаете принять участие в ивенте " + event.getName() + " ?"), new AnswerEventInvitation(player, event));
			}
	}

	private class AnswerEventInvitation implements OnAnswerListener
	{
		private final Player _player;
		private final AbstractFightClub _event;

		private AnswerEventInvitation(Player player, AbstractFightClub event)
		{
			_player = player;
			_event = event;
		}

		@Override
		public void sayYes()
		{
			trySignForEvent(_player, _event, false);
		}

		@Override
		public void sayNo()
		{}
	}

	public FightClubGameRoom createRoom(AbstractFightClub event)
	{
		FightClubGameRoom newRoom = new FightClubGameRoom(event);
		_rooms.add(newRoom);
		return newRoom;
	}

	public AbstractFightClub getNextEvent()
	{
		return _nextEvent;
	}

	public AbstractFightClub getCurrentEvent()
	{
		return _currentEvent;
	}

	public void setCurrentEvent(AbstractFightClub event)
	{
		_currentEvent = event;
	}

	/*
	 * Other
	 */
	private void sendErrorMessageToPlayer(Player player, String msg)
	{
		player.sendPacket(new Say2(player.getObjectId(), ChatType.COMMANDCHANNEL_ALL, "Упс", msg));
		player.sendMessage(msg);
	}

	public void sendToAllMsg(AbstractFightClub event, String msg)
	{
		Say2 packet = new Say2(0, ChatType.CRITICAL_ANNOUNCE, event.getName(), msg);
		for(Player player : GameObjectsStorage.getAllPlayersForIterate())
			player.sendPacket(packet);
	}

	private AbstractFightClub prepareNewEvent(AbstractFightClub event)
	{
		MultiValueSet<String> set = event.getSet();
		AbstractFightClub duplicatedEvent = null;
		try
		{
			@SuppressWarnings("unchecked")
			Class<GlobalEvent> eventClass = (Class<GlobalEvent>) Class.forName(set.getString("eventClass"));
			Constructor<GlobalEvent> constructor = eventClass.getConstructor(MultiValueSet.class);
			duplicatedEvent = (AbstractFightClub) constructor.newInstance(set);

			duplicatedEvent.clearSet();
			// duplicatedEvent.setObjects(event.getObjects());
			_activeEvents.put(duplicatedEvent.getObjectId(), duplicatedEvent);

		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			e.printStackTrace();
		}

		return duplicatedEvent;
	}

	private void startAutoEventsTasks()
	{
		AbstractFightClub closestEvent = null;
		long closestEventTime = Long.MAX_VALUE;

		for(int i = 1; i <= EventHolder.FIGHT_CLUB_EVENTS; i++)
		{
			AbstractFightClub event = EventHolder.getInstance().getEvent(EventType.FIGHT_CLUB_EVENT, i);

			if(event.isAutoTimed())
			{
				Calendar nextEventDate = getClosestEventDate(event.getAutoStartTimes());

				ThreadPoolManager.getInstance().schedule(new EventRunThread(event), nextEventDate.getTimeInMillis() - System.currentTimeMillis());

				// Closest Event
				if(closestEventTime > nextEventDate.getTimeInMillis())
				{
					closestEvent = event;
					closestEventTime = nextEventDate.getTimeInMillis();
				}
			}
		}

		_nextEvent = closestEvent;
	}

	private final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private class RandomEventRunThread extends RunnableImpl
	{
		private AbstractFightClub _event;

		private RandomEventRunThread(AbstractFightClub event)
		{
			_event = event;
		}

		@Override
		public void runImpl() throws Exception
		{
			if(!Config.ALLOW_FIGHT_CLUB)
			{
				_log.info("Event Engine: Event is stoped now.");
				return;
			}

			if(_event != null)
			{
				startEventCountdown(_event);
				_currentEvent = _event;
				_log.info("Event Engine: Event launched -> " + _event.getName() + ".");
			}

			int next = getRandom();
			AbstractFightClub event = EventHolder.getInstance().getEvent(EventType.FIGHT_CLUB_EVENT, next);
			_event = event;

			long time = Config.EVENT_RANDOM_TIME * 60 * 1000;
			String date = DATE.format(new Date(System.currentTimeMillis() + time));
			_log.info("Event Engine: Next event -> " + event.getName() + ". Started at " + date);

			Thread.sleep(60000L);

			ThreadPoolManager.getInstance().schedule(new RandomEventRunThread(_event), time);
			_nextEvent = _event;
		}

		private int getRandom()
		{
			boolean is = false;
			int next = 1;
			while(!is)
			{
				next = Rnd.get(1, EventHolder.FIGHT_CLUB_EVENTS);
				if(next != 3)
					is = true;
			}
			return next;
		}
	}

	/**
	 * Choosing closest Hour and Minute from Array and converting it to Calendar
	 * @param dates {{hour, minute}, {hour, minute}} - of event start
	 * @return Calendar of closest date
	 */
	private Calendar getClosestEventDate(int[][] dates)
	{
		Calendar tempCalendar = Calendar.getInstance();
		tempCalendar.set(Calendar.SECOND, 0);

		Calendar eventCalendar = Calendar.getInstance();

		boolean found = false;
		long smallest = Long.MAX_VALUE;// In case, we need to make it in next day

		for(int[] hourMin : dates)
		{
			tempCalendar.set(Calendar.HOUR_OF_DAY, hourMin[0]);
			tempCalendar.set(Calendar.MINUTE, hourMin[1]);
			long timeInMillis = tempCalendar.getTimeInMillis();

			// If time is smaller than current
			if(timeInMillis < System.currentTimeMillis())
			{
				if(timeInMillis < smallest)
					smallest = timeInMillis;
				continue;
			}

			// If event time wasnt choosen yet or its smaller than current Event Time
			if(!found || timeInMillis < eventCalendar.getTimeInMillis())
			{
				found = true;
				eventCalendar.setTimeInMillis(timeInMillis);
			}
		}

		if(!found)
			eventCalendar.setTimeInMillis(smallest + 86400000);// Smallest time + One Day

		return eventCalendar;
	}

	private class EventRunThread extends RunnableImpl
	{
		private final AbstractFightClub _event;

		private EventRunThread(AbstractFightClub event)
		{
			_event = event;
		}

		@Override
		public void runImpl() throws Exception
		{
			startEventCountdown(_event);

			if(!_event.isAutoTimed())
				return;

			Thread.sleep(60000L);

			Calendar nextEventDate = getClosestEventDate(_event.getAutoStartTimes());

			ThreadPoolManager.getInstance().schedule(new EventRunThread(_event), nextEventDate.getTimeInMillis() - System.currentTimeMillis());
		}
	}

	public boolean canPlayerParticipate(Player player, boolean sendMessage, boolean justMostImportant)
	{
		if(player == null)
			return false;

		if(player.getClassId().level() != 3)
		{
			sendErrorMessageToPlayer(player, "Получите 3 профессию и сможете учавствовать в ивенте!");
			return false;
		}

		if(player.isBlocked())
		{
			sendErrorMessageToPlayer(player, "Вы заблокированы. Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.getCursedWeaponEquippedId() > 0)
		{
			if(sendMessage)
				sendErrorMessageToPlayer(player, "Вы держите проклятое оружие, Регистрация в ивенте не возможна!");
			return false;
		}

		if(Olympiad.isRegistered(player))
		{
			if(sendMessage)
				sendErrorMessageToPlayer(player, "Вы зарегистрированы на олимпиаду, Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.isInOlympiadMode() || player.getOlympiadGame() != null)
		{
			if(sendMessage)
				sendErrorMessageToPlayer(player, "Вы сражаетесь на олимпиаде, Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.isInObserverMode())
		{
			if(sendMessage)
				sendErrorMessageToPlayer(player, "Вы в режиме наблюдателя, Регистрация в ивенте не возможна!");
			return false;
		}

		if(!justMostImportant)
		{
			if(player.isDead() || player.isAlikeDead())
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы мертвы, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isBlocked())
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы заблокированы, Регистрация в ивенте не возможна!");
				return false;
			}

			if(!player.isInPeaceZone() && player.getPvpFlag() > 0)
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы находитесь в PvP, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInCombat())
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы в бою, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.getEvent(DuelEvent.class) != null)
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы в дуэли, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.getKarma() > 0)
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы пк, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInOfflineMode())
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы сидите на оффтрейде, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInStoreMode())
			{
				if(sendMessage)
					sendErrorMessageToPlayer(player, "Вы сидите и торгуете, Регистрация в ивенте не возможна!");
				return false;
			}
		}

		return true;
	}

	public void requestEventPlayerMenuBypass(Player player, String bypass)
	{
		player.sendPacket(TutorialCloseHtml.STATIC);

		// Getting event
		AbstractFightClub event = player.getFightClubEvent();
		if(event == null)
			return;

		// Getting fPlayer
		FightClubPlayer fPlayer = event.getFightClubPlayer(player);
		if(fPlayer == null)
			return;

		// Player isnt viewing main event page now
		fPlayer.setShowTutorial(false);

		// Checking if its the right bypass
		if(!bypass.startsWith(BYPASS))
			return;

		// Getting action
		StringTokenizer st = new StringTokenizer(bypass, " ");
		st.nextToken();// _fightclub

		String action = st.nextToken();

		switch(action)
		{
			case "leave":
				askQuestion(player, "Are you sure You want to leave the event?");
				break;
			case "buffer":
				SchemeBufferInstance.showWindow(player);
				break;
		}
	}

	public void sendEventPlayerMenu(Player player)
	{
		AbstractFightClub event = player.getFightClubEvent();
		if(event == null || event.getFightClubPlayer(player) == null){ return; }

		FightClubPlayer fPlayer = event.getFightClubPlayer(player);

		// Player is viewing main event page now
		fPlayer.setShowTutorial(true);

		StringBuilder builder = new StringBuilder();
		builder.append("<html><head><title>").append(event.getName()).append("</title></head>");
		builder.append("<body>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<table height=20 fixwidth=\"290\" bgcolor=29241d>");
		builder.append("	<tr>");
		builder.append("		<td height=20 width=290>");
		builder.append("			<center><font name=\"hs12\" color=913d3d>").append(event.getName()).append("</font></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<br>");
		builder.append("<table fixwidth=290 bgcolor=29241d>");
		builder.append("	<tr>");
		builder.append("		<td valign=top width=280>");
		builder.append("			<font color=388344>").append(event.getDescription()).append("<br></font>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<br>");

		builder.append("<table width=270>");
		builder.append("	<tr>");
		builder.append("		<td>");
		builder.append("			<center><button value = \"Buffer\" action=\"bypass -h ").append(BYPASS).append(" buffer\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("	<tr>");
		builder.append("		<td>");
		builder.append("			<center><button value = \"Leave Event\" action=\"bypass -h ").append(BYPASS).append(" leave\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("	<tr>");
		builder.append("		<td>");
		builder.append("			<center><button value = \"Close\" action=\"bypass -h ").append(BYPASS).append(" close\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Info_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Info\"></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");

		builder.append("</body></html>");

		player.sendPacket(new TutorialShowHtml(builder.toString()));
		player.sendPacket(new TutorialShowQuestionMark(100));
	}

	private void leaveEvent(Player player)
	{
		AbstractFightClub event = player.getFightClubEvent();
		if(event == null)
			return;

		if(event.leaveEvent(player, true))
			player.sendMessage("You have left the event!");
	}

	private void askQuestion(Player player, String question)
	{
		ConfirmDlg packet = new ConfirmDlg(SystemMsg.S1, 60000).addString(question);
		player.ask(packet, new AskQuestionAnswerListener(player));
	}

	private class AskQuestionAnswerListener implements OnAnswerListener
	{
		private final Player _player;

		private AskQuestionAnswerListener(Player player)
		{
			_player = player;
		}

		@Override
		public void sayYes()
		{
			leaveEvent(_player);
		}

		@Override
		public void sayNo()
		{}

	}

	public AbstractFightClub getEventByObjId(int objId)
	{
		return _activeEvents.get(objId);
	}

	public static FightClubEventManager getInstance()
	{
		if(_instance == null)
			_instance = new FightClubEventManager();
		return _instance;
	}
}
