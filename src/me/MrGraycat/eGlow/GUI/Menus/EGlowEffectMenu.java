package me.MrGraycat.eGlow.GUI.Menus;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import me.MrGraycat.eGlow.EGlow;
import me.MrGraycat.eGlow.Config.*;
import me.MrGraycat.eGlow.Config.EGlowCustomEffectsConfig.Effect;
import me.MrGraycat.eGlow.Config.EGlowMessageConfig.Message;
import me.MrGraycat.eGlow.GUI.*;
import me.MrGraycat.eGlow.Manager.Interface.*;
import me.MrGraycat.eGlow.Util.EnumUtil.GlowDisableReason;
import me.MrGraycat.eGlow.Util.Packets.MultiVersion.ProtocolVersion;
import me.MrGraycat.eGlow.Util.Text.ChatUtil;

public class EGlowEffectMenu extends PaginatedMenu {
	private ConcurrentHashMap<Integer, String> effects = new ConcurrentHashMap<>();	
	
	public EGlowEffectMenu(MenuMetadata menuMetadata) {
		super(menuMetadata);
	}

	@Override
	public String getMenuName() {
		return ChatUtil.translateColors(((EGlowMainConfig.OptionDisablePrefixInGUI()) ? Message.GUI_TITLE.get() : Message.PREFIX.get() + Message.GUI_TITLE.get()));
	}

	@Override
	public int getSlots() {
		return 36;
	}

	@Override
	public void handleMenu(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		IEGlowPlayer eGlowPlayer = EGlow.getDataManager().getEGlowPlayer(player);
		ClickType clickType = e.getClick();
		int clickedSlot = e.getSlot();
		
		switch(clickedSlot) {
		case(28):
			if (!eGlowPlayer.getSaveData())
				eGlowPlayer.setSaveData(true);
			
			eGlowPlayer.setGlowOnJoin(!eGlowPlayer.getGlowOnJoin());
		break;
		case(29):
			if (eGlowPlayer.getPlayer().hasPermission("eglow.command.toggle")) {
				if (eGlowPlayer.getFakeGlowStatus() || eGlowPlayer.getGlowStatus()) {
					eGlowPlayer.toggleGlow();
					ChatUtil.sendMsgWithPrefix(player, Message.DISABLE_GLOW.get());
				} else {
					if (eGlowPlayer.getEffect() == null || eGlowPlayer.getEffect().getName().equals("none")) {
						ChatUtil.sendMsgWithPrefix(player, Message.NO_LAST_GLOW.get());
						return;
					} else {
						if (eGlowPlayer.getGlowDisableReason().equals(GlowDisableReason.DISGUISE)) {
							ChatUtil.sendMsgWithPrefix(player, Message.DISGUISE_BLOCKED.get());
							return;
						}
						
						if (eGlowPlayer.getPlayer().hasPermission(eGlowPlayer.getEffect().getPermission())) {
							eGlowPlayer.toggleGlow();
						} else {
							ChatUtil.sendMsgWithPrefix(player, Message.NO_PERMISSION.get());
							return;
						}
						ChatUtil.sendMsgWithPrefix(player, Message.NEW_GLOW.get(eGlowPlayer.getLastGlowName()));
					}
				}
			} else {
				ChatUtil.sendMsgWithPrefix(player, Message.NO_PERMISSION.get());
			}
		break;
		case(33):
			if (page == 1) {
				new EGlowMainMenu(MenuManager.getMenuMetadata(eGlowPlayer.getPlayer())).openInventory();
			} else {
				page = page - 1;
				super.openInventory();
			}
			break;
		case(34):
			if (EGlow.getDataManager().getCustomEffects().size() > (page * getMaxItemsPerPage())) {
				page = page + 1;
				super.openInventory();
			}
			break;
		default:
			if (effects.containsKey(clickedSlot)) {
				String effect = effects.get(clickedSlot);
				enableGlow(eGlowPlayer.getPlayer(), clickType, effect);	
			}
			break;
		}
		
		UpdateMainEffectsNavigationBar(eGlowPlayer);
	}

	@Override
	public void setMenuItems() {
		IEGlowPlayer p = EGlow.getDataManager().getEGlowPlayer(menuMetadata.getOwner());
		effects = new ConcurrentHashMap<>();
		UpdateMainEffectsNavigationBar(p);
		
		for (String effect : Effect.GET_ALL_EFFECTS.get()) {
			IEGlowEffect Eeffect = EGlow.getDataManager().getEGlowEffect(effect.toLowerCase());
			if (Eeffect == null)
				continue;
			
			int pageNumb = getPage(effect);
			
			if (pageNumb != page)
				continue;
			
			int slot = getSlot(effect);
			
			if (slot > 26)
				continue;
			
			Material material = getMaterial(effect);
			String name = getName(effect);
			int meta = getMeta(effect);
			int model = getModelID(effect);
			ArrayList<String> lores = new ArrayList<>();
			
			for (String lore : Effect.GET_LORES.getList(effect)) {
				lore = ChatUtil.translateColors(lore.replace("%effect_name%", Eeffect.getDisplayName()).replace("%effect_has_permission%", hasPermission(p, Eeffect.getPermission())));
				lores.add(lore);
			}
			
			if (model < 0) {
				inventory.setItem(slot, createItem(material, name, meta, lores));
			} else {
				inventory.setItem(slot, createItem(material, name, meta, lores, model));
			}
			
			if (!effects.containsKey(slot))
				effects.put(slot , Eeffect.getName());
		}
	}
	
	private int getPage(String effect) {
		return Effect.GET_PAGE.getInt(effect);
	}
	
	private int getSlot(String effect) {
		int slot = Effect.GET_SLOT.getInt(effect) - 1;
		if (slot == -1 || slot > 26) {
			ChatUtil.sendToConsoleWithPrefix("Slot: " + (slot + 1) + " for effect " + effect + "is not valid.");
			return 100;
		}
		return slot;
	}
	
	private Material getMaterial(String effect) {
		String mat = Effect.GET_MATERIAL.getString(effect).toUpperCase();
		try {
			if (mat.equals("SAPLING") && EGlow.getDebugUtil().getMinorVersion() >= 13) mat = "SPRUCE_SAPLING";
			if (mat.equals("PUMPKIN") && EGlow.getDebugUtil().getMinorVersion() >= 13) mat = "CARVED_PUMPKIN";
			return Material.valueOf(mat);
		} catch (IllegalArgumentException | NullPointerException e) {
			ChatUtil.sendToConsoleWithPrefix("Material: " + mat + " for effect " + effect + "is not valid.");
			return Material.valueOf("DIRT");
		}
	}
	
	private String getName(String effect) {
		return Effect.GET_NAME.getString(effect);
	}
	
	private int getMeta(String effect) {
		return Effect.GET_META.getInt(effect);
	}
	
	private int getModelID(String effect) {
		return (ProtocolVersion.SERVER_VERSION.getMinorVersion() >= 14) ? Effect.GET_MODEL_ID.getInt(effect) : -1;
	}
}
