package dev.riffic33.heroes.skills;


import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;


public class SkillRocketJump extends ActiveSkill {
		
    public SkillRocketJump(Heroes plugin) {
        super(plugin, "Rocketjump");
        setUsage("/skill rocketjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill rocketjump");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("rocket-speed", 2);
        node.set("rocket-boosts", 3);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.COOLDOWN.node(), 10000);
        return node;
    }
    
    @Override
    public String getDescription(Hero hero) {
        int boosts = SkillConfigManager.getUseSetting(hero, this, "rocket-boosts", 1, false);
        String base = String.format("Put on a rocket pack with %s boosts. Safe fall provided.", boosts);
        
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
        float rocketSpeed = (float) SkillConfigManager.getUseSetting(hero, this, "rocket-speed", 1, false);
        int boosts = (int) SkillConfigManager.getUseSetting(hero, this, "rocket-boosts", 3, false);
        long duration = (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
  
        player.setFallDistance(-(rocketSpeed*8F*boosts));
        RocketPack rp = new RocketPack(this, duration, boosts);
        player.sendMessage("Using rocket jump");
        hero.addEffect(rp);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
    
    public class RocketPack extends ExpirableEffect{

    	private int boostsLeft = 1;
    	
		public RocketPack(Skill skill, long duration, int boosts) {
			super(skill, "RocketPack", duration);
			this.boostsLeft = boosts;
			this.types.add(EffectType.PHYSICAL);
			this.types.add(EffectType.BENEFICIAL);
		}
		
		@Override
        public void applyToHero( Hero hero ) {
            super.applyToHero( hero );
        }
		
		@Override
        public void removeFromHero( Hero hero ) {
            super.removeFromHero(hero);
            Messaging.send(hero.getPlayer(), "Rocket Pack ran out of fuel");
        }
		
		public int getBoostsLeft(){
			return boostsLeft;
		}
		//use and return amount left
		public int useBoost(){
			boostsLeft--;
			return boostsLeft;
		}
    	
    }
    
    public class SkillListener implements Listener{
    	private Skill skill;
    	
    	public SkillListener(Skill skill){
    		this.skill = skill;
    	}
		
    	@EventHandler
    	public void onPlayerInteract(PlayerInteractEvent event){
    		Player player = event.getPlayer();
    		Hero hero = plugin.getCharacterManager().getHero( player );
    		if(hero.hasEffect("RocketPack")){
    			float rocketSpeed = (float) SkillConfigManager.getUseSetting(hero, skill, "rocket-speed", 1, false);
    			RocketPack rp = (RocketPack) hero.getEffect("RocketPack");
    			Vector 	newV = player.getLocation().getDirection().clone();
    					newV.normalize().multiply(rocketSpeed);
    			player.setVelocity(newV);
    			Messaging.send(player, "Rocket Pack has $1 boosts left", rp.boostsLeft);
    			if(rp.useBoost() < 1){
    				hero.removeEffect(rp);
    			}
    			
    		}
    		
    	}
    	
    }
    
   
    
    
}