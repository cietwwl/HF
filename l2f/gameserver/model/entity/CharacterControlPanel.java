package l2f.gameserver.model.entity;

import java.util.StringTokenizer;

import l2f.gameserver.Config;
import l2f.gameserver.instancemanager.QuestManager;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.CCPHelpers.CCPCWHPrivilages;
import l2f.gameserver.model.entity.CCPHelpers.CCPOffline;
import l2f.gameserver.model.entity.CCPHelpers.CCPPassword;
import l2f.gameserver.model.entity.CCPHelpers.CCPPasswordRecover;
import l2f.gameserver.model.entity.CCPHelpers.CCPPoll;
import l2f.gameserver.model.entity.CCPHelpers.CCPRepair;
import l2f.gameserver.model.entity.CCPHelpers.CCPSecondaryPassword;
import l2f.gameserver.model.entity.CCPHelpers.CCPSmallCommands;
import l2f.gameserver.model.entity.events.impl.DuelEvent;
import l2f.gameserver.model.entity.olympiad.Olympiad;
import l2f.gameserver.model.quest.Quest;
import l2f.gameserver.model.quest.QuestState;

import org.apache.commons.lang3.StringUtils;

public class CharacterControlPanel
{
	private static CharacterControlPanel _instance;

	public String useCommand(Player activeChar, String text, String bypass)
	{
		// While some1 is currently writing secondary password
		if (activeChar.isBlocked())
		{
			return null;
		}

		String[] param = text.split(" ");
		if (param.length == 0)
			return "char.html";

		// Block unwanted buffs
		else if (param[0].equalsIgnoreCase("grief"))
		{
			CCPSmallCommands.setAntiGrief(activeChar);
		}
		// Block Experience
		else if (param[0].equalsIgnoreCase("noe"))
		{
			if (activeChar.getVar("NoExp") == null)
				activeChar.setVar("NoExp", "1", -1);
			else
				activeChar.unsetVar("NoExp");
		}
		// Show Online Players
		else if (param[0].equalsIgnoreCase("online"))
		{
			activeChar.sendMessage(CCPSmallCommands.showOnlineCount());
		}
		else if (param[0].equalsIgnoreCase("changeLog"))
		{
			Quest q = QuestManager.getQuest(QuestManager.TUTORIAL_QUEST_ID);
			if (q != null)
			{
				QuestState st = activeChar.getQuestState(q.getName());
				if (st != null)
				{
					String change = ChangeLogManager.getInstance().getChangeLog(ChangeLogManager.getInstance().getLatestChangeId());
					st.showTutorialHTML(change);
				}
			}
		}
		// Show private stores
		else if (param[0].equalsIgnoreCase(Player.NO_TRADERS_VAR))
		{
			if (activeChar.getVar(Player.NO_TRADERS_VAR) == null)
			{
				activeChar.setNotShowTraders(true);
				activeChar.setVar(Player.NO_TRADERS_VAR, "1", -1);
			}
			else
			{
				activeChar.setNotShowTraders(false);
				activeChar.unsetVar(Player.NO_TRADERS_VAR);
			}
		}
		// Show skill animations
		else if (param[0].equalsIgnoreCase(Player.NO_ANIMATION_OF_CAST_VAR))
		{
			if (activeChar.getVar(Player.NO_ANIMATION_OF_CAST_VAR) == null)
			{
				activeChar.setNotShowBuffAnim(true);
				activeChar.setVar(Player.NO_ANIMATION_OF_CAST_VAR, "true", -1);
			}
			else
			{
				activeChar.setNotShowBuffAnim(false);
				activeChar.unsetVar(Player.NO_ANIMATION_OF_CAST_VAR);
			}
		}
		// Change auto loot
		else if (param[0].equalsIgnoreCase("autoloot"))
		{
			setAutoLoot(activeChar);
		}
		else if (param[0].equalsIgnoreCase("repairCharacter"))
		{
			if (param.length > 1)
				CCPRepair.repairChar(activeChar, param[1]);
			else
				return null;
		}
		else if (param[0].equalsIgnoreCase("offlineStore"))
		{
			boolean result = CCPOffline.setOfflineStore(activeChar);
			if (result)
				return null;
			else
				return "char.htm";
		}
		else if (param[0].startsWith("poll") || param[0].startsWith("Poll"))
		{
			CCPPoll.bypass(activeChar, param);
			return null;
		}
		else if (param[0].equals("combineTalismans"))
		{
			CCPSmallCommands.combineTalismans(activeChar);
			return null;
		}
		else if (param[0].equals("otoad"))
		{
			CCPSmallCommands.openToad(activeChar, -1);
			return null;
		}
		else if (param[0].equals("hwidPage"))
		{
			// if(Config.ALLOW_SMARTGUARD)
			// {
			if (activeChar.getHwidLock() != null)
				return "cfgUnlockHwid.htm";
			else
				return "cfgLockHwid.htm";
			// }
		}
		else if (param[0].equals("lockHwid"))
		{
			// if(Config.ALLOW_SMARTGUARD)
			// {
			boolean shouldLock = Boolean.parseBoolean(param[1]);
			if (shouldLock)
			{
				activeChar.setHwidLock(activeChar.getHWID());
				activeChar.sendMessage("Character is now Locked!");
			}
			else
			{
				activeChar.setHwidLock(null);
				activeChar.sendMessage("Character is now Unlocked!");
			}
			// }
		}
		else if (param[0].equalsIgnoreCase("setupPRecover"))
		{
			CCPPasswordRecover.startPasswordRecover(activeChar);
			return null;
		}
		else if (param[0].startsWith("setupPRecover"))
		{
			CCPPasswordRecover.setup(activeChar, text);
			return null;
		}
		else if (param[0].startsWith("cfgSPPassword") || param[0].startsWith("cfgSPRecover"))
		{
			CCPPasswordRecover.reset(activeChar, text);
			return null;
		}
		else if (param[0].startsWith("secondaryPass"))
		{
			CCPSecondaryPassword.startSecondaryPasswordSetup(activeChar, text);
			return null;
		}
		else if (param[0].equalsIgnoreCase("showPassword"))
		{
			return "cfgPassword.htm";
		}
		else if (param[0].equals("changePassword"))
		{
			StringTokenizer st = new StringTokenizer(text, " | ");
			String[] passes = new String[st.countTokens() - 1];
			st.nextToken();
			for (int i = 0; i < passes.length; i++)
			{
				passes[i] = st.nextToken();
			}
			boolean newDialog = CCPPassword.setNewPassword(activeChar, passes);
			if (newDialog)
				return null;
			else
				return "cfgPassword.htm";
		}
		else if (param[0].equalsIgnoreCase("showRepair"))
		{
			return "cfgRepair.htm";
		}
		else if (param[0].equalsIgnoreCase("ping"))
		{
			CCPSmallCommands.getPing(activeChar);
			return null;
		}
		else if (param[0].equalsIgnoreCase("cwhPrivs"))
		{
			if (param.length > 1)
			{
				String args = param[1] + (param.length > 2 ? " " + param[2] : "");
				return CCPCWHPrivilages.clanMain(activeChar, args);
			}
			else
			{
				return "cfgClan.htm";
			}
		}
		else if (param[0].equals("delevel"))
		{
			if (param.length > 1 && StringUtils.isNumeric(param[1]))
			{
				boolean success = CCPSmallCommands.decreaseLevel(activeChar, Integer.parseInt(param[1]));
				if (success)
					return null;
			}

			return "cfgDelevel.htm";
		}
                //OBT
                else if (param[0].equals("cbt.level"))
		{
			if(checkCBT(activeChar))
                            CCPSmallCommands.addLevel(activeChar);

			return "default/53000.htm";
		}
                else if (param[0].equals("cbt.adena"))
		{
			
                    if( activeChar.getInventory().getAdena()<2000000000&&checkCBT(activeChar))
                    {
                        activeChar.getInventory().addAdena(1000000000,"OBT");
                    }
                    return "default/53000.htm";
		}
                else if (param[0].equals("cbt.noble"))
		{
                        if(!activeChar.isNoble()&&checkCBT(activeChar))
                        {
                            activeChar.setNoble(true);
                        }
			return "default/53000.htm";
		}
                else if (param[0].equals("cbt.hero"))
		{
                        if(!activeChar.isHero()&&checkCBT(activeChar))
                        {
                            activeChar.setHero(activeChar);
                        }
			return "default/53000.htm";
		}
		return "char.htm";
	}
        private boolean checkCBT(Player player)
        {
                        if(player.isDead() || player.isAlikeDead())
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы мертвы, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isBlocked())
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы заблокированы, Регистрация в ивенте не возможна!");
				return false;
			}

			if(!player.isInPeaceZone() && player.getPvpFlag() > 0)
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы находитесь в PvP, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInCombat())
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы в бою, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.getEvent(DuelEvent.class) != null)
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы в дуэли, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.getKarma() > 0)
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы пк, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInOfflineMode())
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы сидите на оффтрейде, Регистрация в ивенте не возможна!");
				return false;
			}

			if(player.isInStoreMode())
			{
				//if(sendMessage)
				//	sendErrorMessageToPlayer(player, "Вы сидите и торгуете, Регистрация в ивенте не возможна!");
				return false;
			}
                        if(player.isBlocked())
		{
			//sendErrorMessageToPlayer(player, "Вы заблокированы. Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.getCursedWeaponEquippedId() > 0)
		{
			//if(sendMessage)
			//	sendErrorMessageToPlayer(player, "Вы держите проклятое оружие, Регистрация в ивенте не возможна!");
			return false;
		}

		if(Olympiad.isRegistered(player))
		{
			//if(sendMessage)
			//	sendErrorMessageToPlayer(player, "Вы зарегистрированы на олимпиаду, Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.isInOlympiadMode() || player.getOlympiadGame() != null)
		{
			//if(sendMessage)
			//	sendErrorMessageToPlayer(player, "Вы сражаетесь на олимпиаде, Регистрация в ивенте не возможна!");
			return false;
		}

		if(player.isInObserverMode())
		{
			//if(sendMessage)
			//	sendErrorMessageToPlayer(player, "Вы в режиме наблюдателя, Регистрация в ивенте не возможна!");
			return false;
		}
                if(player.isInCombat())
		{
			//if(sendMessage)
			//	sendErrorMessageToPlayer(player, "Вы в режиме наблюдателя, Регистрация в ивенте не возможна!");
			return false;
		}
                        return true;
        }
	public String replacePage(String currentPage, Player activeChar, String additionalText, String bypass)
	{
		currentPage = currentPage.replaceFirst("%online%", CCPSmallCommands.showOnlineCount());
		currentPage = currentPage.replaceFirst("%antigrief%", getEnabledDisabled(activeChar.getVarB("antigrief")));
		currentPage = currentPage.replaceFirst("%noe%", getEnabledDisabled(activeChar.getVarB("NoExp")));
		currentPage = currentPage.replaceFirst("%notraders%", getEnabledDisabled(activeChar.getVarB("notraders")));
		currentPage = currentPage.replaceFirst("%notShowBuffAnim%", getEnabledDisabled(activeChar.getVarB("notShowBuffAnim")));
		currentPage = currentPage.replaceFirst("%autoLoot%", getEnabledDisabled(activeChar.isAutoLootEnabled()));
		if (currentPage.contains("%charsOnAccount%"))
			currentPage = currentPage.replaceFirst("%charsOnAccount%", CCPRepair.getCharsOnAccount(activeChar.getName(), activeChar.getAccountName()));

		return currentPage;
	}

	private String getEnabledDisabled(boolean enabled)
	{
		if (enabled)
			return "Enabled";
		else
			return "Disabled";
	}

	public void setAutoLoot(Player player)
	{
		if (Config.AUTO_LOOT_INDIVIDUAL)
		{
			player.setAutoLoot(!player.isAutoLootEnabled());
		}
	}

	public static CharacterControlPanel getInstance()
	{
		if (_instance == null)
			_instance = new CharacterControlPanel();
		return _instance;
	}
}
