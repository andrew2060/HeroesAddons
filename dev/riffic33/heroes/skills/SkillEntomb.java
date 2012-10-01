package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

/**
 * duration must be over 10.000, otherwise damage doesn't work,<br>
 * don't know why ... PeriodicExpirableEffect make also no damage :(
 * 
 * @author riffic33
 * @author IDragonfire
 */
public class SkillEntomb extends TargettedSkill {

    public SkillEntomb(Heroes plugin) {
        super(plugin, "Entomb");
        setUsage("/skill entomb");
        setIdentifiers("skill entomb", "skill Entomb");
        setTypes(SkillType.SILENCABLE, SkillType.LIGHT, SkillType.DEBUFF);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 0);
        node.set("LevelMultiplier", 0.5);
        node.set(Setting.DURATION.node(), 12000);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int bDmg = SkillConfigManager.getUseSetting(
        		hero, this,"BaseTickDamage", 0, false);
        float bMulti = SkillConfigManager.getUseSetting(
        		hero, this,"LevelMultiplier", 0, false);
        long duration = SkillConfigManager.getUseSetting(
        		hero, this, Setting.DURATION, 12000, false);
        int damage = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti * hero.getLevel());
        String newDmg = damage > 0 ? "Deals " + damage + " damage" : "";
        	
        String base = String.format( "Encase your target in glass for %s seconds.", duration / 1000D );
        StringBuilder description = new StringBuilder( base + newDmg );
    	
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
        if (player == target) {
            return SkillResult.INVALID_TARGET;
        }
        int bDmg = SkillConfigManager.getUseSetting(hero, this,
                "BaseTickDamage", 0, false);
        float bMulti = (float) SkillConfigManager.getUseSetting(hero, this,
                "LevelMultiplier", 0.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this,
                Setting.DURATION, 12000, false);
        int damage = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti
                * hero.getLevel());

        EntombEffect entombEffect = new EntombEffect(this, player, duration,
                damage);
        CharacterTemplate targetTemplate;

        if (target instanceof Player) {
            targetTemplate = this.plugin.getCharacterManager().getHero(
                    (Player) target);
        } else {
            targetTemplate = this.plugin.getCharacterManager().getMonster(
                    target);
        }
        targetTemplate.addEffect(entombEffect);

        return SkillResult.NORMAL;
    }

    public class EntombEffect extends PeriodicExpirableEffect {

        private HashSet<Block> blocks;
        private final String applyText = "$1 has been entombed";
        private final String expireText = "Entomb removed from $1";
        private Location loc;
        private Player usedBy;
        private SkillListener listener;
        private long damage;

        public EntombEffect(Skill skill, Player usedBy, long duration,
                long damage) {
            super(skill, "EntombEffect", 100, duration);
            this.types.add(EffectType.DISABLE);
            this.types.add(EffectType.STUN);
            this.types.add(EffectType.LIGHT);
            this.usedBy = usedBy;
            this.damage = damage;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            hero.addEffect(new SilenceEffect(getSkill(), getDuration()));
            entomb(hero.getPlayer(), player.getName());
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            LivingEntity entity = monster.getEntity();
            entomb(entity, entity.getType().toString());
        }

        private void entomb(LivingEntity entity, String name) {
            broadcast(entity.getLocation(), this.applyText, name);
            Location pLoc = entity.getLocation();
            Location pBlockLoc = pLoc.getBlock().getLocation();
            Location tpLoc = new Location(pLoc.getWorld(),
                    pBlockLoc.getX() + 0.5D, pBlockLoc.getY(),
                    pBlockLoc.getZ() + 0.5D);
            tpLoc.setYaw(pLoc.getYaw());
            tpLoc.setPitch(pLoc.getPitch());
            entity.teleport(tpLoc);
            this.loc = tpLoc;
            this.blocks = placeGlassBlock(entity);
        }

        private HashSet<Block> placeGlassBlock(LivingEntity target) {
            HashSet<Block> blocks = new HashSet<Block>(20);
            Block glsLoc = target.getLocation().getBlock();
            for (int y = 0; y < 2; y++) {
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        if (glsLoc.getRelative(x, y, z).isEmpty()) {
                            Block iBlock = glsLoc.getRelative(x, y, z);
                            iBlock.setType(Material.GLASS);
                            blocks.add(iBlock);
                        }
                    }
                }
            }
            this.listener = new SkillListener(blocks);
            Bukkit.getServer().getPluginManager().registerEvents(this.listener,
                    SkillEntomb.this.plugin);
            return blocks;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            removeBlocks(player, player.getName());
            if (this.damage > 0) {
                Skill.damageEntity(hero.getPlayer(), this.usedBy,
                        (int) this.damage, DamageCause.MAGIC, false);
            }
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            LivingEntity entity = monster.getEntity();
            removeBlocks(entity, entity.getType().toString());
            if (this.damage > 0) {
                Skill.damageEntity(monster.getEntity(), this.usedBy,
                        (int) this.damage, DamageCause.MAGIC, false);
            }
        }

        public void removeBlocks(LivingEntity entity, String name) {
            Iterator<Block> glsIter = this.blocks.iterator();
            while (glsIter.hasNext()) {
                Block bChange = glsIter.next();
                if (bChange.getType() == Material.GLASS) {
                    bChange.setType(Material.AIR);
                }
            }
            HandlerList.unregisterAll(this.listener);
            broadcast(entity.getLocation(), this.expireText, name);
        }

        @Override
        public void tickHero(Hero hero) {
            tpBack(hero.getPlayer());
        }

        @Override
        public void tickMonster(Monster monster) {
            tpBack(monster.getEntity());
        }

        public void tpBack(LivingEntity entity) {
            try {
                Location location = entity.getLocation();
                if (location.getX() != this.loc.getX()
                        || location.getY() != this.loc.getY()
                        || location.getZ() != this.loc.getZ()) {
                    this.loc.setYaw(location.getYaw());
                    this.loc.setPitch(location.getPitch());
                    entity.teleport(this.loc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class SkillListener implements Listener {

        private Set<Block> blocks;

        public SkillListener(Set<Block> blocks) {
            this.blocks = blocks;
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (this.blocks.contains(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }
}
