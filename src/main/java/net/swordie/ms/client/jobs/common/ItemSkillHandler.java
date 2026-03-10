package net.swordie.ms.client.jobs.common;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Summon;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;

/**
 * Created on 09/07/2021.
 *
 * @author Asura
 */
public class ItemSkillHandler implements ICommonSkillHandler {


    // Item Skill
    public static final int DAWN_SHIELD = 80011247;
    public static final int DAWN_SHIELD_BUFF = 80011248;
    public static final int DIVINE_GUARDIAN = 80011249;
    public static final int DIVINE_SHIELD = 80011250;
    public static final int DIVINE_BRILLIANCE = 80011251;
    public static final int MONOLITH = 80011261;
    public static final int ELEMENTAL_SYLPH = 80001518;
    public static final int FLAME_SYLPH = 80001519;
    public static final int THUNDER_SYLPH = 80001520;
    public static final int ICE_SYLPH = 80001521;
    public static final int EARTH_SYLPH = 80001522;
    public static final int DARK_SYLPH = 80001523;
    public static final int HOLY_SYLPH = 80001524;
    public static final int SALAMANDER_SYLPH = 80001525;
    public static final int ELECTRON_SYLPH = 80001526;
    public static final int UNDINE_SYLPH = 80001527;
    public static final int GNOME_SYLPH = 80001528;
    public static final int DEVIL_SYLPH = 80001529;
    public static final int ANGEL_SYLPH = 80001530;
    public static final int ELEMENTAL_SYLPH_2 = 80001715;
    public static final int FLAME_SYLPH_2 = 80001716;
    public static final int THUNDER_SYLPH_2 = 80001717;
    public static final int ICE_SYLPH_2 = 80001718;
    public static final int EARTH_SYLPH_2 = 80001719;
    public static final int DARK_SYLPH_2 = 80001720;
    public static final int HOLY_SYLPH_2 = 80001721;
    public static final int SALAMANDER_SYLPH_2 = 80001722;
    public static final int ELECTRON_SYLPH_2 = 80001723;
    public static final int UNDINE_SYLPH_2 = 80001724;
    public static final int GNOME_SYLPH_2 = 80001725;
    public static final int DEVIL_SYLPH_2 = 80001726;
    public static final int ANGEL_SYLPH_2 = 80001727;
    public static final int WHITE_ANGELIC_BLESSING = 80000155;
    public static final int WHITE_ANGELIC_BLESSING_2 = 80001154;
    public static final int LIGHTNING_GOD_RING = 80001262;
    public static final int LIGHTNING_GOD_RING_2 = 80011178;
    public static final int GUARD_RING = 80011149;
    public static final int SUN_RING = 80010067;
    public static final int RAIN_RING = 80010068;
    public static final int RAINBOW_RING = 80010069;
    public static final int SNOW_RING = 80010070;
    public static final int LIGHTNING_RING = 80010071;
    public static final int WIND_RING = 80010072;


    private Char chr;

    public ItemSkillHandler(Char chr) {
        this.chr = chr;
    }

    @Override
    public void handleSkill(Char chr, TemporaryStatManager tsm, int skillId, int slv, InPacket inPacket, SkillUseInfo skillUseInfo) {
        Skill skill = SkillData.getSkillDeepCopyById(skillId);
        SkillInfo si = null;
        if (skill != null) {
            si = SkillData.getSkillInfoById(skillId);
        }
        Summon summon;
        Field field = chr.getField();

        switch (skillId) {
            case DAWN_SHIELD:
            case DAWN_SHIELD_BUFF:
                applyDawnShield(tsm, slv);
                break;
            case DIVINE_GUARDIAN:
            case DIVINE_SHIELD:
            case DIVINE_BRILLIANCE:
                applyGenericItemStatBuff(tsm, skillId, slv);
                break;
            case MONOLITH:
                summon = Summon.getSummonByNoCTS(chr, skillId, slv);
                field = chr.getField();
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummonAndRemoveOld(summon);
                break;
            case WHITE_ANGELIC_BLESSING:
            case WHITE_ANGELIC_BLESSING_2:
            case LIGHTNING_GOD_RING:
            case LIGHTNING_GOD_RING_2:
            case GUARD_RING:
            case SUN_RING:
            case RAIN_RING:
            case RAINBOW_RING:
            case SNOW_RING:
            case LIGHTNING_RING:
            case WIND_RING:
                summon = Summon.getSummonBy(chr, skillId, slv);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.Heal);
                summon.setFlyMob(true);
                field.spawnSummonAndRemoveOld(summon);
                break;
            case ELEMENTAL_SYLPH:
            case FLAME_SYLPH:
            case THUNDER_SYLPH:
            case ICE_SYLPH:
            case EARTH_SYLPH:
            case DARK_SYLPH:
            case HOLY_SYLPH:
            case SALAMANDER_SYLPH:
            case ELECTRON_SYLPH:
            case UNDINE_SYLPH:
            case GNOME_SYLPH:
            case DEVIL_SYLPH:
            case ANGEL_SYLPH:
            case ELEMENTAL_SYLPH_2:
            case FLAME_SYLPH_2:
            case THUNDER_SYLPH_2:
            case ICE_SYLPH_2:
            case EARTH_SYLPH_2:
            case DARK_SYLPH_2:
            case HOLY_SYLPH_2:
            case SALAMANDER_SYLPH_2:
            case ELECTRON_SYLPH_2:
            case UNDINE_SYLPH_2:
            case GNOME_SYLPH_2:
            case DEVIL_SYLPH_2:
            case ANGEL_SYLPH_2:
                summon = Summon.getSummonBy(chr, skillId, slv);
                field.spawnSummonAndRemoveOld(summon);
                break;
        }

        tsm.sendSetStatPacket();
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo) {

    }

    @Override
    public void handleHit(Client c, HitInfo hitInfo) {

    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, Option o) {
        if (cts == CharacterTemporaryStat.ProtectiveShield && o != null && o.rOption == DAWN_SHIELD_BUFF) {
            if (chr.hasSkill(DIVINE_GUARDIAN)) {
                applyGenericItemStatBuff(chr.getTemporaryStatManager(), DIVINE_GUARDIAN, chr.getSkillLevel(DIVINE_GUARDIAN));
                chr.getTemporaryStatManager().sendSetStatPacket();
            }
        }
    }

    @Override
    public void handleChangeHP(int curHP, int newHP) {

    }

    @Override
    public void handleChangeMP(int curMP, int newMP) {

    }

    private void applyDawnShield(TemporaryStatManager tsm, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(DAWN_SHIELD_BUFF);
        if (si == null) {
            return;
        }

        int tOpt = getPositiveSkillValue(si, slv, SkillStat.time);
        if (tOpt <= 0) {
            tOpt = 30;
        }

        int pdd = getPositiveSkillValue(si, slv, SkillStat.indiePdd, SkillStat.pdd, SkillStat.pddX);
        if (pdd <= 0) {
            pdd = 500;
        }
        tsm.putCharacterStatValue(CharacterTemporaryStat.IndieDEF, new Option(pdd, DAWN_SHIELD_BUFF, tOpt));

        int stance = getPositiveSkillValue(si, slv, SkillStat.indieStance, SkillStat.stanceProp);
        if (stance <= 0) {
            stance = 100;
        }
        tsm.putCharacterStatValue(CharacterTemporaryStat.IndieStance, new Option(stance, DAWN_SHIELD_BUFF, tOpt));

        int shieldAmount = getPositiveSkillValue(si, slv, SkillStat.damAbsorbShieldR, SkillStat.x, SkillStat.y);
        if (shieldAmount > 0 && shieldAmount <= 100) {
            shieldAmount = chr.getHPPerc(shieldAmount);
        }
        if (shieldAmount <= 0) {
            shieldAmount = chr.getHPPerc(20);
        }
        tsm.putCharacterStatValue(CharacterTemporaryStat.ProtectiveShield, new Option(shieldAmount, DAWN_SHIELD_BUFF, tOpt));
    }

    private void applyGenericItemStatBuff(TemporaryStatManager tsm, int skillId, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si == null) {
            return;
        }
        int tOpt = getPositiveSkillValue(si, slv, SkillStat.time);
        if (tOpt <= 0) {
            tOpt = 10;
        }

        putIfPositive(tsm, CharacterTemporaryStat.IndiePAD, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indiePad, SkillStat.pad));
        putIfPositive(tsm, CharacterTemporaryStat.IndieMAD, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indieMad, SkillStat.mad));
        putIfPositive(tsm, CharacterTemporaryStat.IndieDEF, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indiePdd, SkillStat.pdd, SkillStat.pddX));
        putIfPositive(tsm, CharacterTemporaryStat.IndieStance, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indieStance, SkillStat.stanceProp));
        putIfPositive(tsm, CharacterTemporaryStat.IndieDamR, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indieDamR, SkillStat.damR));
        putIfPositive(tsm, CharacterTemporaryStat.IndieIgnoreMobpdpR, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indieIgnoreMobpdpR));
        putIfPositive(tsm, CharacterTemporaryStat.IndieCr, skillId, tOpt, getPositiveSkillValue(si, slv, SkillStat.indieCr, SkillStat.cr));
    }

    private void putIfPositive(TemporaryStatManager tsm, CharacterTemporaryStat cts, int skillId, int tOpt, int value) {
        if (value > 0) {
            tsm.putCharacterStatValue(cts, new Option(value, skillId, tOpt));
        }
    }

    private int getPositiveSkillValue(SkillInfo si, int slv, SkillStat... stats) {
        for (SkillStat stat : stats) {
            int value = si.getValue(stat, slv);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }
}
