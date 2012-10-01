package dev.riffic33.heroes.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;


public class SkillFear extends TargettedSkill {
	
    public SkillFear(Heroes plugin) {
        super(plugin, "Fear");
        setUsage("/skill fear");
        setArgumentRange(0, 0);
        setIdentifiers("skill fear");
        setTypes(SkillType.SILENCABLE, SkillType.DARK, SkillType.DEBUFF);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);	
        node.set("FearStrength", 0.5);
        node.set("FearStrengthPerLevel", 0.1);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	float fearStrength 	= (float) SkillConfigManager.getUseSetting(hero, this, "FearStrength", 0.5, false);
    	float strengthMulti = (float) SkillConfigManager.getUseSetting(hero, this, "FearStrengthPerLevel", 0.1, false);
    	float newStrength 	= (float) (strengthMulti <= 0L ? fearStrength : fearStrength + strengthMulti*hero.getLevel());
    	String base = String.format("Causes your target to flee for %s seconds at a strength of %s", duration/1000, newStrength);
    	
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
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            Messaging.send(player, "Can't fear the target");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	float fearStrength 	= (float) SkillConfigManager.getUseSetting(hero, this, "FearStrength", 0.5, false);
    	float strengthMulti = (float) SkillConfigManager.getUseSetting(hero, this, "FearStrengthPerLevel", 0.1, false);
    	float newStrength 	= (float) (strengthMulti <= 0L ? fearStrength : fearStrength + strengthMulti*hero.getLevel());
    	
    	FearEffect fe = new FearEffect(this,  duration, player, newStrength);
    	if (target instanceof Player) {
    		plugin.getCharacterManager().getHero( (Player) target ).addEffect(fe);
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
        	plugin.getCharacterManager().getMonster( target ).addEffect( fe );
            return SkillResult.NORMAL;
        } else 
            return SkillResult.INVALID_TARGET;
    }
    
    
    
    public class FearEffect extends PeriodicExpirableEffect{
    
    	private final String applyText = "$1 has been feared";
    	private final String expireText = "Fear Removed from $1";
    	private final Vector mover;
    	private final float yaw;
    	private int swtch;
    	
	    public FearEffect(Skill skill, long duration, Player applier, float strength){
				super(skill, "FearEffect", 100, duration);
				this.types.add(EffectType.DISABLE);
				this.types.add(EffectType.DARK);
				this.types.add(EffectType.DISPELLABLE);
				Vector tempVec = applier.getLocation().getDirection();
				Vector tempVecX = new Vector(tempVec.getX(),0, 0);
				Vector tempVecZ = new Vector(0, 0, tempVec.getZ());
				this.mover = tempVecX.add(tempVecZ).normalize().multiply(strength);
				this.yaw = applier.getLocation().getYaw();
				this.swtch = 1;	
		}  

        @Override
        public void applyToHero( Hero hero ) {
            super.applyToHero( hero );
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }
        
        @Override
        public void applyToMonster( Monster e ) {
            super.applyToMonster( e );
            broadcast( e.getEntity().getLocation(), applyText, e.getEntity().getClass().getSimpleName().substring(5) );
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
        @Override
        public void removeFromMonster( Monster e ) {
            super.removeFromMonster(e);
            broadcast(e.getEntity().getLocation(), expireText, e.getEntity().getClass().getSimpleName().substring(5));
        }
        
        @Override
        public void tickHero(Hero hero) {
            Player p = hero.getPlayer(); 
			if(swtch > 0){
            	p.setVelocity(mover);
			}else{
            	Location lkDir = p.getLocation();
            			 lkDir.setYaw(yaw);
            			 p.teleport(lkDir);
            }
            swtch = -swtch;
        }
        
        @Override
        public void tickMonster(Monster e) {
        	LivingEntity le = e.getEntity();
            if(swtch > 0){
            	le.setVelocity(mover);
			}else{
            	Location lkDir = le.getLocation();
            			 lkDir.setYaw(yaw);
            			 le.teleport(lkDir);
            }
            swtch = -swtch;
        }

    }

   

}
