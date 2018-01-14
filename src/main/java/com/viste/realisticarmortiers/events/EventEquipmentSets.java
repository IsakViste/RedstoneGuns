package com.viste.realisticarmortiers.events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viste.realisticarmortiers.RealisticArmorTiers;
import com.viste.realisticarmortiers.Reference;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class EventEquipmentSets {	
	
	public EventEquipmentGlobalVar global;
	public List<Armors> armors = new ArrayList<Armors>();
	
	private static final Logger log = LogManager.getLogger(Reference.MODID);
	
	public EventEquipmentSets() {
		
		this.global = new EventEquipmentGlobalVar();
		
		// Check / Copy the config files into the config folder
		File configDir = new File(new String(RealisticArmorTiers.instance.configFile.getPath() + Reference.CONFIG_PATH));
		File jsonConfigTiers = new File(new String(RealisticArmorTiers.instance.configFile.getPath() + Reference.JSON_CONFIG_TIERS_PATH));
		File jsonConfigSets = new File(new String(RealisticArmorTiers.instance.configFile.getPath() + Reference.JSON_CONFIG_SETS_PATH));
		
		// Create config folder
		if(!configDir.exists()) {
			log.info("(Config Folder) Creating " + Reference.CONFIG_PATH);
			try {
				configDir.mkdirs();
			} catch (SecurityException se) {
				log.fatal("(Config Folder) Creation Failed");
				log.fatal(se);
				return;
			}
			log.info("(Config Folder) Creation Success");
		} else {
			log.info("(Config Folder) Found " + Reference.CONFIG_PATH);
		}
		
		// Create TIERS & SETS config
		copyFile(jsonConfigTiers, Reference.ASSET_TIERS_PATH);
		copyFile(jsonConfigSets, Reference.ASSET_SETS_PATH);
		
		// JSON File loading
		try {
			log.info("(JSON File) Loading");
			
			List<Tiers> tiers;
			List<Sets> sets;
			
			log.info("-> (File Read) Reading JSON files");
			try {
				Gson gson = new Gson();
				
				// Tiers
				BufferedReader brTiers = new BufferedReader(new FileReader(new String(RealisticArmorTiers.instance.configFile.getPath() + Reference.JSON_CONFIG_TIERS_PATH)));
				Type typeTiers = new TypeToken<List<Tiers>>(){}.getType();
				tiers = gson.fromJson(brTiers, typeTiers);
				
				// Sets
				BufferedReader brSets = new BufferedReader(new FileReader(new String(RealisticArmorTiers.instance.configFile.getPath() + Reference.JSON_CONFIG_SETS_PATH)));
				Type typeSets = new TypeToken<List<Sets>>(){}.getType();
				sets = gson.fromJson(brSets, typeSets);
			} catch (IOException ioe) {
				log.fatal("-> (File Read) Reading Failure");
				log.fatal(ioe);
				return;
			}
			
			log.info("-> (File Read) Reading Success");
			
			try {
				log.info("-> (Armors) Loading All");
				for(int i = 0; i < sets.size(); i++) {
					float speed = 0;
					boolean found = false;
					for(int j = 0; j < tiers.size(); j++) {
						if(sets.get(i).set.contentEquals(tiers.get(j).set)) {
							speed = tiers.get(j).speed;
							found = true;
							break;
						}
					}
					
					if(found == false) {
						log.warn("-> -> (Armor Set) " + sets.get(i).set + " was not found in " + Reference.JSON_TIERS_FILE);
					}
					
					List<Effects> effects = sets.get(i).effects;
					List<ItemArmor> piecesArmors = new ArrayList<ItemArmor>();
					for(int j = 0; j < sets.get(i).pieces.size(); j++) {
						String path = sets.get(i).modId + ":" + sets.get(i).pieces.get(j);
						ItemArmor armor = (ItemArmor)ItemArmor.getByNameOrId(path);
						if(armor == null) {
							log.warn("-> -> (Armor Piece) " + sets.get(i).pieces.get(j) + " was not found!");
						} else {
							piecesArmors.add(armor);
						}
					}
					armors.add(new Armors(global, piecesArmors, effects, speed));
				}
				log.info("-> (Armors) Loading Success");
			} catch (Exception e) {
				log.fatal("-> (Armors) Loading Failure");
				log.fatal(e);
			}
		} catch (Exception e) {
			log.fatal("(JSON File) Loading Failure");
			log.fatal(e);
			return;
		}
		log.info("(JSON File) Loading Success");
	}

	@SubscribeEvent
	public void onArmorUpdate(PlayerTickEvent evt) {
		
		Item armor = null;
		ItemStack stacks = null;
		
		int j=0;
		
		global.setSpeed(0.1f);
		
		while (j < armors.size()){
			armors.get(j).resetPieces();
			j++;
		}
		
		for(int i = 0; i < 4; i++) {
			stacks = evt.player.inventory.armorItemInSlot(i);
			
			if(stacks != null) {
				armor = stacks.getItem();
				
				j=0;
				while (j < armors.size()){
					if(armors.get(j).checkArmor(armor,evt)){
						break;
					}
					
					j++;
				}
			}
		}
		// Movement Speed
		evt.player.capabilities.setPlayerWalkSpeed(global.getSpeed());
	}
	
	// Copy from 'asset' into 'file'
	void copyFile(File file, String asset) {
		if(!file.exists()) {
			log.info("(JSON File) Copying " + asset);
			try {
				file.createNewFile();
				
				InputStream instream = this.getClass().getResourceAsStream(asset);
				FileOutputStream outstream = new FileOutputStream(file);
				
				byte[] buffer = new byte[1024];
				int length;
				
				while ((length = instream.read(buffer)) > 0) {
					outstream.write(buffer, 0, length);
				}
				
				instream.close();
				outstream.close();
			} catch (IOException ioe) {
				log.fatal("(JSON File) Copy Failed");
				log.fatal(ioe);
				return;
			}
			log.info("(JSON File) Copy Success");
		} else {
			log.info("(JSON File) Found " + asset); 
		}
	}
}

class Armors {
	private List<ItemArmor> armors = null;
	private int armorPieces = 0;
	private int armorPiecesLength = 0;
	private List<Effects> effects;
	private EventEquipmentGlobalVar global;
	private float speed = 0;
	
	public Armors(EventEquipmentGlobalVar global, List<ItemArmor> items, List<Effects> effects, float speed) {
		this.global = global;
		this.speed = speed;
		this.armors = items;
		this.armorPiecesLength = items.size();
		this.effects = effects;
	}
	
	public boolean checkArmor(Item item, PlayerTickEvent evt){
		if(armors.contains(item)){
			this.armorPieces++;
			global.setSpeed(global.getSpeed() + speed);
			this.addSetEffectsArmor(evt);
			return true;
		}
		return false;
	}

	public boolean isFullSet(){
		if(this.armorPieces >= this.armorPiecesLength){
			return true;
		}
		return false;
	}

	public void resetPieces(){
		this.armorPieces = 0;
	}

	public void addSetEffectsArmor(PlayerTickEvent evt){
		if(this.isFullSet()){
			int i = 0;
			while (i < effects.size()){
				evt.player.addPotionEffect(new PotionEffect(Potion.getPotionById(effects.get(i).potion_effect), global.getPotionDur(), effects.get(i).efficiency - 1));
				i++;
			}
		}
	}
}

class Sets {
	public String set;
	public String modId;
	public List<String> pieces;
	public List<Effects> effects;
	
	public Sets(String set, String modId, List<String> pieces, List<Effects> effects) {
		this.set = set;
		this.modId = modId;
		this.pieces = pieces;
		this.effects = effects;
	}
}

class Effects {
	public int potion_effect;
	public int efficiency;
	
	public Effects(int potion_effect, int efficiency){
		this.potion_effect = potion_effect;
		this.efficiency = efficiency;
	}
}

class Tiers {
	public String set;
	public int weight;
	public float speed;
	
	public Tiers(String set, int weight, float speed) {
		this.set = set;
		this.weight = weight;
		this.speed = speed;
	}
}

class EventEquipmentGlobalVar {
	private int maxPotions = 28; // Amount of potions + 1
	private int potionDur = 20; // 20 ticks ~= 1 second
	private float newPlayerSpeed = 0.1f;

	public float getSpeed(){
		return this.newPlayerSpeed;
	}
	
	public void setSpeed(float speed){
		this.newPlayerSpeed = speed;
	}

	public int getPotionDur(){
		return this.potionDur;
	}
}
