package dev.riffic33.heroes.skills;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;



public class SkillGroundPound extends ActiveSkill {
	
    public SkillGroundPound(Heroes plugin) {
        super(plugin, "Groundpound");
        setUsage("/skill groundpound");
        setArgumentRange(0, 0);
        setIdentifiers("skill groundpound");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FORCE);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set("Targets", 4);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("JumpMultiplier", 1.2);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	double bDmg 			= SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int targets 		= (int) SkillConfigManager.getUseSetting(hero, this, "Targets", 10, false);
    	double newDmg 		= (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	String base = String.format("Hit the ground dealing %s damage and sending %s nearby enemies into the air", newDmg, targets);
    	
    	StringBuilder description = new StringBuilder( base );
    	
    	//Additional descriptive-ness of skill settings
    	int initCD = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false);
    	int redCD = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        int CD = (initCD - redCD) / 1000;
        if (CD > 0) {
        	description.append( " CD:"+ CD + "s" );
        }
        
        int initM = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 0, false);
        int redM = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false)* hero.getSkillLevel(this);
        int manaUse = initM - redM;
        if (manaUse > 0) {
        	description.append(" M:"+manaUse);
        }
        
        int initHP = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false);
        int redHP = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, 0, true) * hero.getSkillLevel(this);
        int HPCost = initHP - redHP;
        if (HPCost > 0) {
        	description.append(" HP:"+HPCost);
        }
        
        int initF = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false);
        int redF = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        int foodCost = initF - redF;
        if (foodCost > 0) {
        	description.append(" FP:"+foodCost);
        }
        
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
        	description.append(" W:"+delay);
        }
        
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
        	description.append(" XP:"+exp);
        }
        
        return description.toString();
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	double bDmg 			= SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int targets 		= (int) SkillConfigManager.getUseSetting(hero, this, "Targets", 10, false);
    	int radius 			= (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
    	float jMod 			= (float) SkillConfigManager.getUseSetting(hero, this, "JumpMultiplier", 0.6, false);
    	double newDmg 		= (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
    	HeroParty hParty = hero.getParty();
    	int hitsLeft = targets;
    	Vector flyer = new Vector(0, jMod, 0);
    	if(hParty != null){
	    	for(Entity entity : nearby){
	    		if(hitsLeft <= 0) break;
	    		if((entity instanceof Player && hParty.isPartyMember((Player) entity))){
	    			continue;
	    		}
	    		if( entity instanceof Monster){
	                addSpellTarget(entity, hero);
	                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	                entity.setVelocity(flyer);
	                hitsLeft -= 1;
	    		}
	    		if( entity instanceof Player){
	    			if (damageCheck(player, (LivingEntity) entity)){
	    				addSpellTarget(entity, hero);
		                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	    			}
	    			entity.setVelocity(flyer);
	    			hitsLeft -= 1;
	    		} 
	    	}
    	}else{
    		for(Entity entity : nearby){
    			if(hitsLeft <= 0) break;
	    		if( entity instanceof Monster){
	                addSpellTarget(entity, hero);
	                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	                entity.setVelocity(flyer);
	                hitsLeft -= 1;
	    		}
	    		if( entity instanceof Player){
	    			if (damageCheck(player, (LivingEntity) entity)){
	    				addSpellTarget(entity, hero);
		                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	    			}
	    			entity.setVelocity(flyer);
	    			hitsLeft -= 1;
	    		} 
	    	}
    	}
    	broadcast(player.getLocation(), "$1 used ground pound", player.getDisplayName());
    	return SkillResult.NORMAL;
    }
    
   
    
}