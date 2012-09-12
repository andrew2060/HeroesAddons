package dev.riffic33.heroes.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillLifeTap extends ActiveSkill {

    public SkillLifeTap(Heroes plugin) {
        super(plugin, "Lifetap");
        setUsage("/skill lifetap");
        setDescription("Convert %s health to %s mana.");
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
        int healthLoss = SkillConfigManager.getSetting(hero.getHeroClass(),
                this, "HealthLossPercentage", 10);
        int manaGain = SkillConfigManager.getSetting(hero.getHeroClass(), this,
                "Manaregain", 10);
        StringBuffer sb = new StringBuffer(String.format(getDescription(),
                healthLoss + "%", manaGain));
        int thresHold = SkillConfigManager.getSetting(hero.getHeroClass(),
                this, "Threshold", 20);
        if (thresHold > 0) {
            sb.append(" Not available with less than ");
            sb.append(thresHold);
            sb.append("% health left.");
        }
        double cdSec = SkillConfigManager.getUseSetting(hero, this,
                Setting.COOLDOWN, 45000, false) / 1000.0D;
        if (cdSec > 0.0D) {
            sb.append(" CD:");
            sb.append(Util.formatDouble(cdSec));
            sb.append("s");
        }
        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA,
                30, false);
        if (mana > 0) {
            sb.append(" M:");
            sb.append(mana);
        }
        return sb.toString();
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

        if (hero.getHealth() < hero.getMaxHealth() * (thresHold / 100D)) {
            Messaging.send(player, "Health is too low to use life tap");
            return SkillResult.CANCELLED;
        }
        if (hero.getMana() >= hero.getMaxMana()) {
            Messaging.send(player, "Mana is already full");
            return SkillResult.CANCELLED;
        }

        int damage = (int) (Math.floor(hero.getMaxHealth() * (pctLoss / 100D)));
        damageEntity(player, player, damage, DamageCause.CUSTOM);
        int newMana = hero.getMana() + manaGain > hero.getMaxMana() ? hero
                .getMaxMana() : hero.getMana() + manaGain;
        hero.setMana(newMana);
        Messaging.send(player, "You lose $1 health. New mana: $2",
                new Object[] { damage + "", newMana + "" });
        return SkillResult.NORMAL;
    }
}