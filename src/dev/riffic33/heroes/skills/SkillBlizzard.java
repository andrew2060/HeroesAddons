package dev.riffic33.heroes.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;


public class SkillBlizzard extends ActiveSkill{
	
    public SkillBlizzard(Heroes plugin) {
        super(plugin, "Blizzard");
        setDescription("Rapid fire snowballs for $1 seconds");
        setUsage("/skill blizzard");
        setArgumentRange(0, 0);
        setIdentifiers("skill blizzard");
        setTypes(SkillType.ICE, SkillType.HARMFUL, SkillType.SILENCABLE);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("DurationLevelMultiplier", 0.1);
        return  node;
    }
    
    @Override
	public String getDescription(Hero hero) {
		int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
    	int durMulti 		= (int) SkillConfigManager.getUseSetting(hero, this, "DurationLevelMultiplier", 100, false);
    	int newDur 		= (int) (durMulti <= 0L ? duration : duration + durMulti*hero.getLevel());
		
		StringBuilder description = new StringBuilder( String.format("Rapid fire snowballs for %s seconds", newDur/1000) );
    	
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
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
    	int durMulti 		= (int) SkillConfigManager.getUseSetting(hero, this, "DurationLevelMultiplier", 100, false);
    	int newDur 		= (int) (durMulti <= 0L ? duration : duration + durMulti*hero.getLevel());
    	
    	BlizzardEffect be = new BlizzardEffect(this, newDur, player);
    	hero.addEffect(be);
        return SkillResult.NORMAL;
    }
    
    public class BlizzardEffect extends PeriodicExpirableEffect{
    	Player user;
    
    	public BlizzardEffect(Skill skill, long duration, Player user) {
			super(skill, "BlizzardEffect", 200, duration);		
			this.types.add(EffectType.DISPELLABLE);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.ICE);
			this.user = user;
    	}
    	
    	@Override
    	public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            broadcast(user.getLocation(), "$1 gained Blizzard", user.getDisplayName());    
        }
    	
    	 @Override
         public void removeFromHero(Hero hero) {
    		super.removeFromHero(hero);
         	broadcast(user.getLocation(), "$1 lost Blizzard", user.getDisplayName()); 
         }

		@Override
		public void tickHero(Hero hero) {
			user.launchProjectile( Snowball.class );
		}

		@Override
		public void tickMonster(Monster arg0) {
		}
    	
    }

	
	
	
}
