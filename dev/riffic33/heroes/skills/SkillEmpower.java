package dev.riffic33.heroes.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillEmpower extends ActiveSkill {
	
    public SkillEmpower(Heroes plugin) {
        super(plugin, "Empower");
        setUsage("/skill empower");
        setArgumentRange(0, 0);
        setIdentifiers("skill empower");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);	
        node.set("BaseDamage", 2);
        node.set("LevelMultiplier", 0.5);
        node.set("Percentage", 10);
        node.set("LevelPercentageIncrease", 0.1);
        
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int newDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	int bPercent 		= (int) SkillConfigManager.getUseSetting(hero, this, "Percentage", 10, false);
    	float bPerMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelPercentageIncrease", 2, false);
    	float newPercent 	= (int) (bPerMulti <= 0L ? bPercent : bPercent + bPerMulti*hero.getLevel());
    	
    	String base = String.format("Your next physical attack deals %s damage and takes %s of the target's health. Buff lasts for %s seconds", newDmg, newPercent+"%", duration/1000);
    	
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
	public SkillResult use(Hero hero, String[] arg1) {
    	
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	hero.addEffect(new EmpowerBuff(this, duration));
    	
		return SkillResult.NORMAL;
	}

    public class EmpowerBuff extends ExpirableEffect{
    
    	private final String applyText = "$1 has been Empowered";
    	private final String expireText = "$1 lost Empowerment";
    	
	    public EmpowerBuff(Skill skill, long duration){
				super(skill, "EmpowerBuff", duration);
				this.types.add(EffectType.BENEFICIAL);
				this.types.add(EffectType.PHYSICAL);
		}  

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
    }

    public class SkillListener implements Listener{
		
    	private final Skill skill;
        
        public SkillListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
        	if (!(event instanceof EntityDamageByEntityEvent)) {
                return;
            }
            
        	Entity initTarg = event.getEntity();
            if (!(initTarg instanceof LivingEntity) && !(initTarg instanceof Player)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                return;
            }
            
            Player player = (Player) subEvent.getDamager();
            Hero hero = plugin.getCharacterManager().getHero( player );
            if (hero.hasEffect("EmpowerBuff")) {
            	
                ItemStack item = player.getItemInHand();
                if (!Util.swords.contains(item.getType().name()) && !Util.axes.contains(item.getType().name())) {
                    return;
                }   
                EmpowerBuff eb = (EmpowerBuff) hero.getEffect("EmpowerBuff");
                
                hero.removeEffect(eb);
                
                int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, skill, "BaseDamage", 3, false);
            	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, skill, "LevelMultiplier", 0.5, false);
            	int newDmg 			= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
            	
            	int bPercent 		= (int) SkillConfigManager.getUseSetting(hero, skill, "Percentage", 10, false);
            	float bPerMulti 	= (float) SkillConfigManager.getUseSetting(hero, skill, "LevelPercentageIncrease", 2, false);
            	float newPercent 		= (int) (bPerMulti <= 0L ? bPercent : bPercent + bPerMulti*hero.getLevel());
            	
            	int addPercentDamage = 0;
            	if(initTarg instanceof Player){
            		Hero targHero = plugin.getCharacterManager().getHero( (Player) initTarg );
            		addPercentDamage = (int) ((int) targHero.getMaxHealth()*(newPercent/100f));
            	}else{
            		addPercentDamage = (int) (((LivingEntity) initTarg).getMaxHealth()*(newPercent/100f));
            	}

                event.setDamage( newDmg+addPercentDamage);
               
                broadcast(player.getLocation(),"$1 used an Empowered attack", player.getDisplayName());
            }
        }	
    }

}
