package com.nisovin.shopkeepers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.server.v1_4_6.*;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class VolatileCode {

	@SuppressWarnings("unchecked")
	public static boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {

		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer)player).getHandle().world, 0);
			
			Field recipeListField = EntityVillager.class.getDeclaredField(Settings.recipeListVar);
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList)recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (ItemStack[] recipe : shopkeeper.getRecipes()) {
				recipeList.add(createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
			}
			
			villager.a(((CraftPlayer)player).getHandle());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity)entity).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("rawtypes")
	public static void overwriteVillagerAI(LivingEntity villager) {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void setVillagerProfession(Villager villager, int profession) {
		((CraftVillager)villager).getHandle().setProfession(profession);
	}
	
	public static ItemStack loadItemStack(ConfigurationSection config) {
	    ItemStack item = new ItemStack(config.getInt("id"), config.getInt("amt"), (short)config.getInt("data"));
		if (config.contains("itemMeta")) {
		    item.setItemMeta((ItemMeta) config.get("itemMeta"));
		}
		
        // rest of code left for backwards compatibility and for just-in-case
		if (config.contains("nbtdata")) {
			try {
				Object nbtData = config.get("nbtdata");
				ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) nbtData);
				NBTBase tag = NBTBase.b(new DataInputStream(stream));
				if (tag instanceof NBTTagCompound) {
				    net.minecraft.server.v1_4_6.ItemStack tempItem = CraftItemStack.asNMSCopy(item);
				    tempItem.tag = (NBTTagCompound)tag;
				    item = CraftItemStack.asBukkitCopy(tempItem);
				}
			} catch (Exception e) {
				ShopkeepersPlugin.debug("Error loading item NBT data");
			}
		}
		
		if (config.contains("name") || config.contains("lore") || config.contains("color")) {
            net.minecraft.server.v1_4_6.ItemStack tempItem = CraftItemStack.asNMSCopy(item);

			NBTTagCompound tag = tempItem.tag;
			if (tag == null) {
				tag = new NBTTagCompound();
				tempItem.tag = tag;
			}
			NBTTagCompound display = tag.getCompound("display");
			if (display == null) {
				display = new NBTTagCompound();
			}
			if (config.contains("name")) {
				display.setString("Name", config.getString("name"));
			}
			if (config.contains("lore")) {
				List<String> lore = config.getStringList("lore");
				NBTTagList list = new NBTTagList();
				for (String l : lore) {
					list.add(new NBTTagString("", l));
				}
				display.set("Lore", list);
			}
			if (config.contains("color")) {
				display.setInt("color", config.getInt("color"));
			}
			tag.setCompound("display", display);
			
            item = CraftItemStack.asBukkitCopy(tempItem);
		}
		
		if (config.contains("enchants")) {
			List<String> list = config.getStringList("enchants");
			for (String s : list) {
				String[] enchantData = s.split(" ");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(enchantData[0])), Integer.parseInt(enchantData[1]));
			}
		}
		
		if (item.getType() == Material.WRITTEN_BOOK && config.contains("title") && config.contains("author") && config.contains("pages")) {
            net.minecraft.server.v1_4_6.ItemStack tempItem = CraftItemStack.asNMSCopy(item);

            NBTTagCompound tag = tempItem.tag;
            if (tag == null) {
				tag = new NBTTagCompound();
				tempItem.tag = tag;
			}
			tag.setString("title", config.getString("title"));
			tag.setString("author", config.getString("author"));
			List<String> pages = config.getStringList("pages");
			NBTTagList tagPages = new NBTTagList();
			for (String page : pages) {
				NBTTagString tagPage = new NBTTagString(null, page);
				tagPages.add(tagPage);
			}
			tag.set("pages", tagPages);
			
            item = CraftItemStack.asBukkitCopy(tempItem);
		}
		
		if (config.contains("extra")) {
            net.minecraft.server.v1_4_6.ItemStack tempItem = CraftItemStack.asNMSCopy(item);

            NBTTagCompound tag = tempItem.tag;
            if (tag == null) {
				tag = new NBTTagCompound();
				tempItem.tag = tag;
			}
			ConfigurationSection extraDataSection = config.getConfigurationSection("extra");
			for (String key : extraDataSection.getKeys(false)) {
				tag.setString(key, extraDataSection.getString(key));
			}
			
            item = CraftItemStack.asBukkitCopy(tempItem);

		}
		return item;
	}
	
	public static void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("id", item.getTypeId());
		config.set("data", item.getDurability());
		config.set("amt", item.getAmount());
		
		config.set("name", item.getItemMeta().getDisplayName());
		config.set("itemMeta", item.getItemMeta());
	}
	
    public static String getTitleOfBook(ItemStack book) {
        BookMeta meta = (BookMeta) book.getItemMeta();
        return meta.getTitle();
	}
	
	public static boolean isBookAuthoredByShopOwner(ItemStack book, String owner) {
        BookMeta meta = (BookMeta) book.getItemMeta();
        return meta.getAuthor().equalsIgnoreCase(owner);
	}
	
	public static String getNameOfItem(ItemStack item) {
        return item.getItemMeta().getDisplayName();
	}
	
	public static boolean itemNamesEqual(ItemStack item1, ItemStack item2) {
		String name1 = getNameOfItem(item1);
		String name2 = getNameOfItem(item2);
		return (name1.equals(name2));
	}
	
	private static MerchantRecipe createMerchantRecipe(ItemStack item1, ItemStack item2, ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
		try {
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);
		} catch (Exception e) {}
		return recipe;
	}
	
	private static net.minecraft.server.v1_4_6.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asNMSCopy(item);
	}

	
}
