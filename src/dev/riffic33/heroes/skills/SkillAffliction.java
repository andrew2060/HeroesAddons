package dev.riffic33.heroes.skills;

import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillAffliction extends TargettedSkill {
	
    public SkillAffliction(Heroes plugin) {
        super(plugin, "Affliction");
        setUsage("/skill affliction");
        setArgumentRange(0, 0);
        setIdentifiers("skill affliction");
        setTypes(SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.HARMFUL);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.PERIOD.node(), 4000);
        node.set("MaxJumps", 3);
        node.set("MaxJumpDistance", 5);
        
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long period 	= (long) SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 4000, false);
    	long duration 	= (long) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 12000, false);
    	int jumps 		= (int) SkillConfigManager.getUseSetting(hero, this, "MaxJumps", 3, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	String dJump = jumps > 0 ? " Jumps " +jumps+ " times":"";
    	
    	
    	String base = String.format("Put a damage over time effect on the target dealing %s damage every %s seconds over %s seconds.", tickDmg, period/1000L, duration/1000L);
    	StringBuilder description = new StringBuilder( base + dJump );
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
        System.out.println( "M: " + manaUse );
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
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 4000, false);
    	int maxJumps 	= (int) SkillConfigManager.getUseSetting(hero, this, "MaxJumps", 3, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	AfflictionEffect ae = new AfflictionEffect(this, period, duration, tickDmg-1, player, maxJumps);	//-1 QUICKFIX FOR HEROES BUG
    	if (target instanceof Player) {
    		plugin.getCharacterManager().getHero((Player) target).addEffect(ae);
    		
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
        	Monster mstr = plugin.getCharacterManager().getMonster( target );
        			mstr.addEffect( ae );
        	
            return SkillResult.NORMAL;
        } else 
        	Heroes.log(Level.WARNING, target+"");
            return SkillResult.INVALID_TARGET;
    }
    
    public class AfflictionEffect extends PeriodicDamageEffect{
    	//Future ?
    	//private HashSet<LivingEntity> tracked = new HashSet<LivingEntity>(30);
    
    	private String applyText = "Affiction cast on $1";
    	private String expireText = "Affiction removed from $1";
    	private int maxJumps;
    	private Skill skill;
    	
	    public AfflictionEffect(Skill skill, long period, long duration, double tickDmg, Player applier, int maxJumps){
				super(skill, "Affliction", period, duration, tickDmg, applier);
				this.types.add(EffectType.DISPELLABLE);
				this.types.add(EffectType.DARK);
				this.types.add(EffectType.HARMFUL);
				this.skill = skill;
				this.maxJumps = maxJumps;
		}  
	    
	    @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());    
        }
        
	    @Override
        public void applyToMonster(Monster entity) {
            super.applyToMonster(entity);
            broadcast( entity.getEntity().getLocation(), applyText, entity.getEntity().getClass().getSimpleName().substring(5) );    
        }
       
        public void removeFromHero(Hero hero) {
        	Player player = hero.getPlayer();
        	broadcast(player.getLocation(), expireText, player.getDisplayName()); 
        	if(maxJumps-1 <= 0){
        		super.removeFromHero(hero);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, player, ae);
	            super.removeFromHero(hero); 
        	}
        }
        
        @Override
        public void removeFromMonster(Monster entity) {
        	broadcast( entity.getEntity().getLocation(), expireText, entity.getEntity().getClass().getSimpleName().substring(5) ); 
        	if(maxJumps-1 <= 0){
        		super.removeFromMonster(entity);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, entity, ae);
	            super.removeFromMonster(entity);  
        	}
        }
        	
        private void passEffect(Hero hero, Player entity, AfflictionEffect eff){
        	int radius = (int) SkillConfigManager.getUseSetting(hero, this.getSkill(), "MaxJumpDistance", 5, false);
        	for(Entity newTarget : ((Entity) entity).getNearbyEntities(radius, radius, radius)){
        		if(!(newTarget instanceof LivingEntity) || newTarget == eff.getApplier()){
        			continue;
        		}
            	if (newTarget instanceof Player) {
            		plugin.getCharacterManager().getHero((Player) newTarget).addEffect(eff);
                    break;
                }
        	}
        }
        
        private void passEffect(Hero hero, Monster entity, AfflictionEffect eff){
        	int radius = (int) SkillConfigManager.getUseSetting(hero, this.getSkill(), "MaxJumpDistance", 5, false);
        	for(Entity newTarget : entity.getEntity().getNearbyEntities(radius, radius, radius)){
        		if( !(newTarget instanceof LivingEntity) || newTarget.equals( eff.getApplier() ) ){
        			continue;
        		}
            	if (newTarget instanceof LivingEntity) {
                	Monster creature = plugin.getCharacterManager().getMonster( (LivingEntity) newTarget );
                			creature.addEffect(eff);
                    break;
                }
        	}
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
        }
        
        @Override
        public void tickMonster(Monster entity) {
            super.tickMonster(entity);
        }
	    
    }

    

}
