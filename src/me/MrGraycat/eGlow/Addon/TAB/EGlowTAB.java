package me.MrGraycat.eGlow.Addon.TAB;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import me.MrGraycat.eGlow.EGlow;
import me.MrGraycat.eGlow.Config.EGlowMainConfig;
import me.MrGraycat.eGlow.Manager.Interface.IEGlowPlayer;
import me.MrGraycat.eGlow.Util.Text.ChatUtil;
import me.neznamy.tab.api.EnumProperty;
import me.neznamy.tab.api.TABAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.BukkitTABLoadEvent;

public class EGlowTAB implements Listener {
	private boolean TAB_Bukkit = false;
	private boolean nametagPrefixSuffixEnabled = true;
	private boolean isTeamBlockingActive = true;
	private ConcurrentHashMap<Player, String> groups = new ConcurrentHashMap<>();
	private File file = new File("plugins/TAB/config.yml");
	
	/**
	 * Loads in the TAB addon for eGlow 
	 */
	public EGlowTAB() {
		TAB_Bukkit = EGlow.getDebugUtil().pluginCheck("TAB");
		
		if (TAB_Bukkit && !EGlow.getDebugUtil().getPlugin("TAB").getClass().getName().startsWith("me.neznamy.tab"))
			TAB_Bukkit = false;
		
		if (TAB_Bukkit) {
			EGlow.getInstance().getServer().getPluginManager().registerEvents(this, EGlow.getInstance());
			
			if (file.exists()) {
				YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
				nametagPrefixSuffixEnabled = config.getBoolean("change-nametag-prefix-suffix");
				isTeamBlockingActive = (config.contains("anti-override.scoreboard-teams")) ? config.getBoolean("anti-override.scoreboard-teams") : true;
			} else {
				ChatUtil.sendToConsoleWithPrefix("&cTAB is installed but eGlow in unable to access it's config");
			}
			
			if (EGlowMainConfig.OptionAdvancedTABIntegration() && !TABAPI.isUnlimitedNameTagModeEnabled())
				TABAPI.enableUnlimitedNameTagModePermanently();
			
			startUpdateChecker();
		}
		
		if (TAB_Bukkit || EGlow.getDebugUtil().onBungee()) {
			EGlow.getInstance().getServer().getPluginManager().registerEvents(new EGlowTABEvents(), EGlow.getInstance());
		}
	}
	
	@EventHandler
	public void onTABReloadBukkit(BukkitTABLoadEvent e) {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					if (file.exists()) {
						YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
						nametagPrefixSuffixEnabled = config.getBoolean("change-nametag-prefix-suffix");
					} else {
						ChatUtil.sendToConsoleWithPrefix("&cTAB is installed but eGlow in unable to access it's config");
					}

					if (!isUnlimitedNametagModeEnabled()) {
						cancel();
						return;
					}
					
					for (Player p : players) {
						updateTABValues(p, false);
					}
				} catch (Exception e) {
					ChatUtil.reportError(e);
				}
			}
		}.runTaskAsynchronously(EGlow.getInstance());
	}
	
	/**
	 * Start update checker that will check the players group every 2.5s
	 * This to update whenever it changes
	 */
	public void startUpdateChecker() {
		new BukkitRunnable() {
			@Override
			public void run() {
				Collection<? extends Player> players = Bukkit.getOnlinePlayers();
				
				if (!isUnlimitedNametagModeEnabled()) {
					cancel();
					return;
				}
				
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!isUnlimitedNametagModeEnabled()) {
							cancel();
							return;
						}
						
						for (Player p : players) {
							updateTABValues(p, true);
						}
					}
				}.runTaskAsynchronously(EGlow.getInstance());
			}
		}.runTaskTimer(EGlow.getInstance(), 1L, 50L);
	}
	
	/**
	 * Using TAB's API to set values
	 * @param p player to set this for
	 * @param teamCheck true if used in the group update checker, false if not
	 */
	public void updateTABValues(Player p, boolean teamCheck) {
		IEGlowPlayer eglowPlayer = EGlow.getDataManager().getEGlowPlayer(p);
		TabPlayer tabPlayer = (TabPlayer) TABAPI.getPlayer(p.getUniqueId());
		
		if (eglowPlayer == null || tabPlayer == null)
			return;
		
		if (teamCheck) {
			if (groups.containsKey(p) && tabPlayer.getGroup().equals(groups.get(p))) {
				return;
			}
				
			if (!groups.containsKey(p)) {
				groups.put(p, tabPlayer.getGroup());
			} else {
				groups.replace(p, tabPlayer.getGroup());
			}
		}
		
		try {
			String tagPrefix = "";
			String color = (eglowPlayer.getActiveColor().equals(ChatColor.RESET)) ? "" : eglowPlayer.getActiveColor() + "";
			
			try {tagPrefix = tabPlayer.getOriginalValue(EnumProperty.TAGPREFIX);} catch(Exception ex) {tagPrefix = "";}

			if (!EGlowMainConfig.OptionAdvancedTABIntegration()) {
				tabPlayer.setValueTemporarily(EnumProperty.TAGPREFIX, (!tagPrefix.isEmpty()) ? tagPrefix + color : color);
			} else {
				String customTagName = "";
				
				try {customTagName = tabPlayer.getOriginalValue(EnumProperty.CUSTOMTAGNAME);} catch(Exception ex) {customTagName = eglowPlayer.getPlayer().getName();}
				
				tabPlayer.setValueTemporarily(EnumProperty.CUSTOMTAGNAME, tagPrefix.replace("%eglow_glowcolor%", "") + customTagName);
				tabPlayer.setValueTemporarily(EnumProperty.TAGPREFIX, color);
			}
		} catch (IllegalStateException e) {
			//Ignored
		}
	}
	
	/**
	 * Check if TAB is installed on bukkit
	 * @return if TAB is installed on bukkit
	 */
	public boolean installedOnBukkit() {
		return this.TAB_Bukkit;
	}
	
	/**
	 * Check if TAB's unlimited-nametag-prefix-suffix-mode is enabled
	 * @return if TAB's unlimited-nametag-prefix-suffix-mode is enabled
	 */
	public boolean isUnlimitedNametagModeEnabled() {
		return this.nametagPrefixSuffixEnabled;
	}
	
	public boolean isTeamPacketBlockerActive() {
		return this.isTeamBlockingActive;
	}
	
	/**
	 * Remove player from the group update checker
	 * @param p player to remove from group update checker
	 */
	public void removePlayerGroup(Player p) {
		if (this.groups.containsKey(p)) 
			this.groups.remove(p);
	}
}