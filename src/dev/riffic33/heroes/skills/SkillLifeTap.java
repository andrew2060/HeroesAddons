package dev.riffic33.heroes.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillLifeTap extends ActiveSkill {

    public SkillLifeTap(Heroes plugin) {
        super(plugin, "Lifetap");
        setUsage("/skill lifetap");
        setIdentifiers("skill lifetap");
        setTypes(SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("HealthLossPercentage", 10);
        node.set("Manaregain", 10);
        node.set("Threshold", 20);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int healthLoss = SkillConfigManager.getSetting(
        		hero.getHeroClass(),this, "HealthLossPercentage", 10);
        int manaGain = SkillConfigManager.getSetting(
        		hero.getHeroClass(), this,"Manaregain", 10);
        int thresHold = SkillConfigManager.getSetting(
        		hero.getHeroClass(),this, "Threshold", 20);
        
        
        String base = String.format("Convert %s health to %s mana.", healthLoss + "%", manaGain);
        StringBuilder description = new StringBuilder( base );     
        
        if (thresHold > 0) {
        	description.append(" Not available with less than ");
        	description.append(thresHold);
        	description.append("% health left.");
        }
        
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

        int pctLoss = SkillConfigManager.getSetting(hero.getHeroClass(), this,
                "HealthLossPercentage", 10);
        int manaGain = SkillConfigManager.getSetting(hero.getHeroClass(), this,
                "Manaregain", 10);
        int thresHold = SkillConfigManager.getSetting(hero.getHeroClass(),
                this, "Threshold", 20);

        if (hero.getPlayer().getHealth() < hero.getPlayer().getMaxHealth() * (thresHold / 100D)) {
            Messaging.send(player, "Health is too low to use life tap");
            return SkillResult.CANCELLED;
        }
        if (hero.getMana() >= hero.getMaxMana()) {
            Messaging.send(player, "Mana is already full");
            return SkillResult.CANCELLED;
        }

        double damage = (hero.getPlayer().getMaxHealth() * (pctLoss / 100D));
        damageEntity(player, player, damage, DamageCause.CUSTOM);
        int newMana = hero.getMana() + manaGain > hero.getMaxMana() ? hero
                .getMaxMana() : hero.getMana() + manaGain;
        hero.setMana(newMana);
        Messaging.send(player, "You lose $1 health. New mana: $2",
                new Object[] { damage + "", newMana + "" });
        return SkillResult.NORMAL;
    }
}