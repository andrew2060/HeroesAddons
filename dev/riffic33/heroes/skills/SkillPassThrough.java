package dev.riffic33.heroes.skills;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillPassThrough extends ActiveSkill {
	
    public SkillPassThrough(Heroes plugin) {
        super(plugin, "Passthrough");
        setUsage("/skill passthrough");
        setArgumentRange(0, 0);
        setIdentifiers("skill passthrough");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
        String base = String.format("Move through a wall of blocks.");
        
        StringBuilder description = new StringBuilder( base );
    	
    	//Additional descriptive-ness of skill settings
    	int initCD = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false);
    	int redCD = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        int CD = (initCD - redCD) / 1000;
        if (CD > 0) {
        	description.append( " CD:"+ CD + "s" );
        }
        
        int initM = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 0, false);
        int redM = SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false)* hero.getSkillLevel(this);
        int manaUse = initM - redM;
        if (manaUse > 0) {
        	description.append(" M:"+manaUse);
        }
        
        int initHP = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false);
        int redHP = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, 0, true) * hero.getSkillLevel(this);
        int HPCost = initHP - redHP;
        if (HPCost > 0) {
        	description.append(" HP:"+HPCost);
        }
        
        int initF = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false);
        int redF = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        int foodCost = initF - redF;
        if (foodCost > 0) {
        	description.append(" FP:"+foodCost);
        }
        
        int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
        	description.append(" W:"+delay);
        }
        
        int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
        if (exp > 0) {
        	description.append(" XP:"+exp);
        }
        
        return description.toString();
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	int distance = 3;
    	
        boolean passBlock = false;
        boolean tpPlayer = false;
        Integer blockCount = 0;
        Block b = null;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        } catch (IllegalStateException e) {
            Messaging.send(player, "There was an error getting your pass through location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        while (iter.hasNext()) {
            b = iter.next();
            if ((!Util.transparentBlocks.contains(b.getType()) || !Util.transparentBlocks.contains(b.getType())) && blockCount == 1) {
                passBlock = true; 
            } else {
            	if(blockCount == 2 && Util.transparentBlocks.contains(b.getType()) && Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())){
            		tpPlayer = true;
            		break;
            	}
            }
            blockCount++;
        }
        if (passBlock) {
        	if(tpPlayer){
	            broadcastExecuteText(hero);
	           
	            Location tpLoc = b.getLocation().clone();
	            tpLoc.setPitch(player.getLocation().getPitch());
	            tpLoc.setYaw(player.getLocation().getYaw());
	            player.teleport(tpLoc);
	            return SkillResult.NORMAL;
        	}else{
        		Messaging.send(player, "This is a thicker wall than you can pass through");
        		return SkillResult.FAIL;
        	}
        } else {
        	Messaging.send(player, "Invalid selection to pass through");
            return SkillResult.FAIL;
        }
    }
    
    
    
}