package net.swordie.ms.handlers.item;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.packet.ZeroPool;
import net.swordie.ms.connection.packet.field.FieldPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static net.swordie.ms.enums.ChatType.Mob;
import static net.swordie.ms.enums.ChatType.SystemNotice;
import static net.swordie.ms.enums.EquipBaseStat.*;
import static net.swordie.ms.enums.InvType.*;

public class ItemUpgradeHandler {

    private static final Logger log = LogManager.getLogger(ItemUpgradeHandler.class);
    private static final int CHAOS_ROLL_SCALE = 1_000_000;
    private static final int[] NORMAL_CHAOS_VALUES = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5};
    private static final int[] NORMAL_CHAOS_WEIGHTS = {49_400, 29_700, 36_500, 80_000, 137_000, 183_800, 193_100, 158_700, 102_100, 19_800, 9_900};
    private static final int[] INCREDIBLE_COG_VALUES = {0, 1, 2, 3, 4, 6};
    private static final int[] INCREDIBLE_COG_WEIGHTS = {183_827, 330_081, 238_669, 138_661, 49_438, 59_324};
    // Based on the normal chaos distribution, extended to -6..6 with a slight positive bias.
    private static final int[] MIRACULOUS_CHAOS_VALUES = {-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6};
    private static final int[] MIRACULOUS_CHAOS_WEIGHTS = {12_000, 48_116, 28_928, 35_551, 77_920, 133_438, 179_021, 188_079, 154_574, 99_445, 19_285, 9_643, 14_000};

    private static int rollWeightedStat(int low, int mid, int high) {
        int roll = Util.getRandom(99);
        if (roll < 50) {
            return low;
        }
        if (roll < 90) {
            return mid;
        }
        return high;
    }

    private static int rollTwoWeightedStat(int low, int high, int lowChance) {
        return Util.getRandom(99) < lowChance ? low : high;
    }

    private static int rollWeightedValue(int[] values, int[] weights) {
        int roll = Util.getRandom(CHAOS_ROLL_SCALE - 1);
        int cur = 0;
        for (int i = 0; i < values.length; i++) {
            cur += weights[i];
            if (roll < cur) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }

    private static boolean isMiraculousChaosScroll(int scrollID) {
        return scrollID == ItemConstants.MIRACULOUS_CHAOS_SCROLL_50
                || scrollID == ItemConstants.MIRACULOUS_CHAOS_SCROLL_60;
    }

    private static boolean isIncredibleChaosOfGoodnessScroll(int scrollID) {
        return scrollID == ItemConstants.INCREDIBLE_CHAOS_SCROLL_OF_GOODNESS_20
                || scrollID == ItemConstants.INCREDIBLE_CHAOS_SCROLL_OF_GOODNESS_40
                || scrollID == ItemConstants.INCREDIBLE_CHAOS_SCROLL_OF_GOODNESS_50
                || scrollID == ItemConstants.INCREDIBLE_CHAOS_SCROLL_OF_GOODNESS_60;
    }

    private static int getSetStarForceScrollTarget(int scrollID) {
        return switch (scrollID) {
            case 2049374, 2049375, 2049370, 2049381, 2644017 -> 12;
            case 2049378, 2049379, 2644001, 2049372, 2049398 -> 15;
            case 2049393, 2049394, 2644000, 2644002, 2644004, 2049371 -> 17;
            case 2049392, 2644016, 2049376 -> 20;
            case 2049373 -> 10;
            case 2049391 -> 7;
            case 2049395 -> 5;
            default -> 0;
        };
    }

    private static int getSetStarForceScrollMaxReqLevel(int scrollID) {
        return switch (scrollID) {
            case 2049374, 2049381, 2049378, 2049393 -> 150;
            case 2644017, 2049375, 2049379, 2644001, 2049398,
                 2049394, 2644000, 2644002, 2644004, 2049392, 2644016 -> 160;
            default -> 0;
        };
    }

    private static boolean requiresUntradableSetStarForceScroll(int scrollID) {
        return scrollID == 2644017;
    }

    private static int rollChaosStat(int scrollID, boolean noNegative, EquipBaseStat ebs, boolean maximize) {
        int amount;
        if (maximize) {
            if (isMiraculousChaosScroll(scrollID)) {
                amount = 6;
            } else if (isIncredibleChaosOfGoodnessScroll(scrollID)) {
                amount = 6;
            } else {
                amount = ItemConstants.RAND_CHAOS_MAX;
            }
        } else if (isMiraculousChaosScroll(scrollID)) {
            amount = rollWeightedValue(MIRACULOUS_CHAOS_VALUES, MIRACULOUS_CHAOS_WEIGHTS);
        } else if (isIncredibleChaosOfGoodnessScroll(scrollID)) {
            amount = rollWeightedValue(INCREDIBLE_COG_VALUES, INCREDIBLE_COG_WEIGHTS);
        } else if (!noNegative) {
            amount = rollWeightedValue(NORMAL_CHAOS_VALUES, NORMAL_CHAOS_WEIGHTS);
        } else {
            amount = Util.getRandom(ItemConstants.RAND_CHAOS_MAX);
        }
        if (ebs == iMaxHP || ebs == iMaxMP) {
            amount *= 10;
        }
        return amount;
    }

    private static int rollWeightedStat(int low, int mid, int high, boolean maximize) {
        return maximize ? high : rollWeightedStat(low, mid, high);
    }

    private static int rollTwoWeightedStat(int low, int high, int lowChance, boolean maximize) {
        return maximize ? high : rollTwoWeightedStat(low, high, lowChance);
    }

    private static void applyCustomXScrollStats(Equip equip, int scrollID, boolean maximize) {
        switch (scrollID) {
            case 2615031:
            case 2615053:
            case 2616061:
            case 2048817:
            case 2048829:
                equip.addStat(iPAD, rollWeightedStat(5, 6, 7, maximize));
                return;
            case 2048818:
            case 2048819:
                if (isMagicAttackPetEquipXScroll(scrollID)) {
                    equip.addStat(iMAD, rollWeightedStat(5, 6, 7, maximize));
                } else {
                    equip.addStat(iPAD, rollWeightedStat(5, 6, 7, maximize));
                }
                return;
            case 2615032:
            case 2616062:
                equip.addStat(iMAD, rollWeightedStat(5, 6, 7, maximize));
                return;
            case 2047409:
                equip.addStat(iPAD, rollTwoWeightedStat(4, 5, 50, maximize));
                return;
            case 2047410:
                equip.addStat(iMAD, rollTwoWeightedStat(4, 5, 50, maximize));
                return;
            case 2047405:
            case 2047407:
            case 2048836:
            case 2048838:
            case 2046856:
            case 2047402:
            case 2048094:
            case 2048804:
                equip.addStat(iPAD, rollTwoWeightedStat(4, 5, 85, maximize));
                return;
            case 2047406:
            case 2046408:
            case 2048837:
            case 2048839:
            case 2046857:
            case 2047403:
            case 2048095:
            case 2048805:
                equip.addStat(iMAD, rollTwoWeightedStat(4, 5, 85, maximize));
                return;
            case 2613050:
            case 2612061:
                equip.addStat(iPAD, rollWeightedStat(10, 11, 12, maximize));
                return;
            case 2613051:
            case 2612062:
                equip.addStat(iMAD, rollWeightedStat(10, 11, 12, maximize));
                return;
            case 2046991:
            case 2640024:
            case 2047844:
                equip.addStat(iPAD, rollWeightedStat(9, 10, 11, maximize));
                return;
            case 2046992:
            case 2040025:
                equip.addStat(iMAD, rollWeightedStat(9, 10, 11, maximize));
                return;
            case 2046829:
                equip.addStat(iPAD, rollWeightedStat(2, 3, 4, maximize));
                return;
            default:
                return;
        }
    }

    private static boolean isMagicAttackPetEquipXScroll(int scrollID) {
        var itemName = StringData.getItemStringById(scrollID);
        if (itemName == null) {
            return false;
        }
        var upperName = itemName.toUpperCase();
        return upperName.contains("PET EQUIP") && upperName.contains("X")
                && (upperName.contains("MAGIC ATT") || upperName.contains("M. ATT"));
    }


    @Handler(op = InHeader.USER_EX_ITEM_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserExItemUpgradeItemUseRequest(Client c, InPacket inPacket) {
        handleRebirthFlameUse(c, inPacket, false);
    }

    @Handler(op = InHeader.USER_KARMA_CONSUME_ITEM_USE_REQUEST)
    public static void handleUserKarmaConsumeItemUseRequest(Client c, InPacket inPacket) {
        handleRebirthFlameUse(c, inPacket, true);
    }

    private static void handleRebirthFlameUse(Client c, InPacket inPacket, boolean blackFlame) {
        inPacket.decodeInt(); //tick
        var usePosition = inPacket.decodeShort();
        var eqpPosition = inPacket.decodeShort();
        var enchantSkill = inPacket.decodeByte() != 0;

        Char chr = c.getChr();
        if (chr.getPendingFlameInfo() != null) {
            chr.chatMessage(SystemNotice, "Finish your current black flame choice first.");
            chr.dispose();
            return;
        }
        Item flame = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(usePosition);
        var invType = eqpPosition < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(eqpPosition);

        if (flame == null || equip == null || !ItemConstants.canEquipHaveFlame(equip)) {
            chr.chatMessage(SystemNotice, "Could not find flame or equip.");
            chr.dispose();
            return;
        }

        if (!ItemConstants.isRebirthFlame(flame.getItemId())) {
            chr.chatMessage(SystemNotice, "This item is not a rebirth flame.");
            chr.dispose();
            return;
        }

        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(flame.getItemId()).getScrollStats();
        if (vals.size() > 0) {
            int reqEquipLevelMax = vals.getOrDefault(ScrollStat.reqEquipLevelMax, 250);

            if (equip.getReqLevel() + equip.getiIncReq() > reqEquipLevelMax) {
                c.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("Equipment level does not meet scroll requirements.")));
                chr.dispose();
                return;
            }

            boolean success = chr.isAdminInvincible() || Util.succeedProp(vals.getOrDefault(ScrollStat.success, 100));
            Equip oldEquip = blackFlame && success ? equip.deepCopy() : null;
            Equip otherEquip = null;

            if (success) {
                boolean eternalFlame = vals.getOrDefault(ScrollStat.createType, 6) >= 7;
                equip.randomizeFlameStats(eternalFlame); // Generate high stats if it's an eternal/RED flame only.

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    int otherEquipPos = Math.abs(eqpPosition) == 10 ? 11 : 10;
                    otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                    if (otherEquip != null) {
                        otherEquip.copyFlameStatsFrom(equip);
                        otherEquip.updateToChar(chr);
                    }

                    chr.write(ZeroPool.egoEquipComplete(true, true));
                }
            }

            c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, flame.getItemId(), equip.getItemId(), false));
            equip.updateToChar(chr);
            chr.consumeItem(flame);
            if (blackFlame && success) {
                chr.setPendingFlameInfo(new PendingFlameInfo(equip, oldEquip, otherEquip));
                ItemHandlerModule.startBlackFlameHelperScript(chr, equip, oldEquip, otherEquip);
                return;
            }
            chr.write(WvsContext.rebirthFlameResult(flame.getItemId(), equip.getBagIndexWithEquipped(), 0));
        }

        chr.dispose();
    }

    @Handler(op = InHeader.USER_MEMORIAL_CUBE_OPTION_REQUEST)
    public static void handleUserMemorialCubeOptionRequest(Char chr, InPacket inPacket) {
        MemorialCubeInfo mci = chr.getMemorialCubeInfo();
        if (mci == null) {
            return;
        }

        inPacket.decodeInt(); // tick
        boolean chooseBefore = inPacket.decodeByte() == 7;
        mci.applyPotential(chooseBefore);
        mci.getEquip().updateToChar(chr);
        chr.setMemorialCubeInfo(null);
    }

    @Handler(op = InHeader.GOLD_HAMMER_REQUEST)
    public static void handleGoldHammerRequest(Char chr, InPacket inPacket) {
        if (chr.getClient().getWorld().isReboot()) {
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 1, 0));
            chr.getOffenseManager().addOffense(String.format("Character %d attempted to hammer in reboot world.", chr.getId()));
            return;
        }

        inPacket.decodeInt(); // tick
        int iPos = inPacket.decodeInt(); // hammer slot
        int hammerID = inPacket.decodeInt(); // hammer item id
        int eInvType = inPacket.decodeInt(); // use hammer? useless though
        int ePos = inPacket.decodeInt(); // equip slot

        InvType invType = InvType.getInvTypeByVal(eInvType);
        if (invType.equals(EQUIP) && ePos < 0) {
            invType = EQUIPPED;
        }

        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot((short) ePos);
        Item hammer = chr.getInventoryByType(CONSUME).getItemBySlot((short) iPos);
        short maxHammers = ItemConstants.MAX_HAMMER_SLOTS;

        if (equip != null) {
            var defaultEquip = ItemData.getEquipInfoById(equip.getItemId());
            if (defaultEquip.isHasIUCMax()) {
                maxHammers = defaultEquip.getIucMax();
            }
        }

        if (equip == null || !ItemConstants.canEquipGoldHammer(equip) ||
                hammer == null || !ItemConstants.isGoldHammer(hammer) || hammerID != hammer.getItemId()) {
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 1, 0));
            chr.getOffenseManager().addOffense(String.format("Character %d tried to use hammer (id %d) on an invalid equip (id %d)",
                    chr.getId(), hammer == null ? 0 : hammer.getItemId(), equip == null ? 0 : equip.getItemId()));
            return;
        }

        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(hammer.getItemId()).getScrollStats();

        if (vals.size() > 0) {
            if (equip.getIuc() >= maxHammers) {
                chr.getOffenseManager().addOffense(String.format("Character %d tried to use hammer (id %d) an invalid equip (id %d)",
                        chr.getId(), hammerID, equip.getItemId()));
                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 2, 0));
                return;
            }

            boolean success = Util.succeedProp(vals.getOrDefault(ScrollStat.success, 100));

            if (success) {
                equip.addStat(iuc, 1); // +1 hammer used
                equip.addStat(tuc, 1); // +1 upgrades available
                equip.updateToChar(chr);

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                    Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                    otherEquip.copySecondaryStatsFrom(equip);
                    otherEquip.updateToChar(chr);

                    chr.write(ZeroPool.egoEquipComplete(true, true));
                }

                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Success, 0, equip.getIuc()));
            } else {
                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Fail, 1, equip.getIuc()));
            }

            chr.consumeItem(hammer.getItemId(), 1);
        }
    }

    @Handler(op = InHeader.GOLD_HAMMER_COMPLETE)
    public static void handleGoldHammerComplete(Char chr, InPacket inPacket) {
        int returnResult = inPacket.decodeInt();
        int result = inPacket.decodeInt();
        if (returnResult == GoldHammerResult.Success.getVal() || returnResult == GoldHammerResult.Fail.getVal()) {
            //I think its ok to just send back the result given.
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Done, result, 0));
        } else {
            chr.getOffenseManager().addOffense(String.format("Character %d have invalid gold hammer complete returnResult %d",
                    chr.getId(), returnResult));
        }
    }

    @Handler(op = InHeader.USER_UPGRADE_ASSIST_ITEM_USE_REQUEST)
    public static void handleUserUpgradeAssistItemUseRequest(Client c, InPacket inPacket) {

        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to scroll in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort(); //Use Position
        var invType = InvType.getInvTypeByVal(inPacket.decodeShort());
        short ePos = inPacket.decodeShort(); //Eqp Position
        byte bEnchantSkill = inPacket.decodeByte(); //no clue what this means exactly
//        short idk = inPacket.decodeShort(); //No clue what this is, stayed  00 00  throughout different tests
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        invType = ePos < 0 ? EQUIPPED : invType;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            return;
        }
        int scrollID = scroll.getItemId();
        EquipAttribute equipAttribute = null;
        switch (scrollID) {
            case 2532000: // Safety Scroll
            case 2532001: // Pet Safety Scroll
            case 2532002: // Safety Scroll
            case 2532003: // Safety Scroll
            case 2532004: // Pet Safety Scroll
            case 2532005: // Safety Scroll
                equipAttribute = EquipAttribute.UpgradeCountProtection;
                break;
            case 2530000: // Lucky Day
            case 2530002: // Lucky Day
            case 2530003: // Pet Lucky Day
            case 2530004: // Lucky Day
            case 2530006: // Pet Lucky Day
                equipAttribute = EquipAttribute.LuckyDay;
                break;
            case 2531000: // Protection Scroll
            case 2531001:
            case 2531004:
            case 2531005:
                equipAttribute = EquipAttribute.ProtectionScroll;
                break;
            default:
                if (ItemConstants.isReturnScroll(scrollID)) {
                    equipAttribute = EquipAttribute.ReturnScroll;
                }
                break;
        }
        if (equipAttribute == null) {
            chr.chatMessage(SystemNotice, "That scroll cannot be applied.");
            return;
        }
        if (equip.hasAttribute(equipAttribute)) {
            chr.chatMessage(SystemNotice, "That item already has this scroll applied.");
            return;
        }
        equip.addAttribute(equipAttribute);
        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), true, false, scrollID, equip.getItemId(), false));

        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
            int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
            otherEquip.copyAttributesFrom(equip);
            otherEquip.updateToChar(chr);

            chr.write(ZeroPool.egoEquipComplete(true, true));
        }

        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_HYPER_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserHyperUpgradeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        var usePos = inPacket.decodeShort();
        var equipPos = inPacket.decodeShort();

        var scroll = chr.getConsumeInventory().getItemBySlot(usePos);
        var equip = equipPos < 0
                ? chr.getEquippedInventory().getItemBySlot(Math.abs(equipPos))
                : chr.getEquipInventory().getItemBySlot(equipPos);

        if (scroll == null || !(equip instanceof Equip)) {
            chr.chatMessage("Could not find the scroll or the equip.");
            chr.dispose();
            return;
        }

        int scrollId = scroll.getItemId();
        int targetStars = getSetStarForceScrollTarget(scrollId);
        if (targetStars > 0) {
            Equip eq = (Equip) equip;
            int maxReqLevel = getSetStarForceScrollMaxReqLevel(scrollId);
            if (requiresUntradableSetStarForceScroll(scrollId)) {
                boolean untradable = eq.isTradeBlock()
                        || eq.isEquipTradeBlock()
                        || eq.hasAttribute(EquipAttribute.Untradable);
                if (!untradable) {
                    chr.chatMessage("This scroll can only be used on untradable equipment.");
                    chr.dispose();
                    return;
                }
            }
            if (eq.getBaseStat(tuc) > 0) {
                chr.chatMessage("This scroll can only be used on equipment with no remaining upgrades.");
                chr.dispose();
                return;
            }
            if (eq.getInfo().isSuperiorEqp()) {
                chr.chatMessage("This scroll cannot be used on superior equipment.");
                chr.dispose();
                return;
            }
            if (maxReqLevel > 0 && eq.getReqLevel() + eq.getiIncReq() > maxReqLevel) {
                chr.chatMessage("Equipment level does not meet scroll requirements.");
                chr.dispose();
                return;
            }
            if (eq.getChuc() > targetStars) {
                chr.chatMessage("This item already has more than the target Star Force.");
                chr.dispose();
                return;
            }
            if (GameConstants.getMaxStars(eq) < targetStars) {
                chr.chatMessage("This item cannot reach the target Star Force.");
                chr.dispose();
                return;
            }

            eq.setChuc((short) targetStars);
            eq.updateToChar(chr);

            if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(eq.getItemId())) {
                int otherEquipPos = Math.abs(equipPos) == 10 ? 11 : 10;
                Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                otherEquip.copySecondaryStatsFrom(eq);
                otherEquip.updateToChar(chr);
                chr.write(ZeroPool.egoEquipComplete(true, true));
            }

            chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), true, false, scroll.getItemId(), eq.getItemId(), false));
            chr.consumeItem(scroll);
            return;
        }

        if (scrollId == 2644006 || scrollId == 2644007) {
            Equip eq = (Equip) equip;
            if (eq.getBaseStat(tuc) > 0) {
                chr.chatMessage("This scroll can only be used on equipment with no remaining upgrades.");
                chr.dispose();
                return;
            }
            if (eq.getReqLevel() + eq.getiIncReq() > 200) {
                chr.chatMessage("Equipment level does not meet scroll requirements.");
                chr.dispose();
                return;
            }
            int newStars = eq.getChuc() + 1;
            if (newStars > 23) {
                chr.chatMessage("This item cannot exceed 23 Star Force with this scroll.");
                chr.dispose();
                return;
            }
            if (GameConstants.getMaxStars(eq) < newStars) {
                chr.chatMessage("This item cannot reach the target Star Force.");
                chr.dispose();
                return;
            }

            boolean success = chr.isAdminInvincible() || Util.succeedProp(ItemData.getItemInfoByID(scrollId).getScrollStats().getOrDefault(ScrollStat.success, 100));
            if (success) {
                eq.setChuc((short) newStars);
                eq.updateToChar(chr);

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(eq.getItemId())) {
                    int otherEquipPos = Math.abs(equipPos) == 10 ? 11 : 10;
                    Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                    otherEquip.copySecondaryStatsFrom(eq);
                    otherEquip.updateToChar(chr);
                    chr.write(ZeroPool.egoEquipComplete(true, true));
                }
            } else if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(eq.getItemId())) {
                chr.write(ZeroPool.egoEquipComplete(false, true));
            }

            chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollId, eq.getItemId(), false));
            chr.consumeItem(scroll);
            return;
        }

        boolean advanced = false;
        boolean safeguard = false;

        switch (scroll.getItemId()) {
            case 2049301: // Normal EE
            case 2049307:
                break;
            case 2049023: // AEE
            case 2049300:
            case 2049303:
            case 2049306:
            case 2049323:
                advanced = true;
                break;
            case 2049325: // Noboom AEE
                advanced = true;
                safeguard = true;
                break;
        }

        boolean success = ItemHandlerModule.enhanceItem(chr, (Equip) equip, advanced, safeguard);
        if (!success) {
            chr.dispose();
        } else {
            chr.consumeItem(scroll);
        }
    }

    @Handler(op = InHeader.USER_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to scroll in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort(); //Use Position
        var invType = InvType.getInvTypeByVal(inPacket.decodeShort());
        short ePos = inPacket.decodeShort(); //Eqp Position
        byte bEnchantSkill = inPacket.decodeByte(); //no clue what this means exactly
        short idk = inPacket.decodeShort(); //No clue what this is, stayed  00 00  throughout different tests

        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        int scrollID = scroll.getItemId();
        var ii = ItemData.getItemInfoByID(scrollID);

        invType = ePos < 0 ? EQUIPPED : invType;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);

        if (equip == null || equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            chr.dispose();
            return;
        }

        if (ItemConstants.isSymbol(equip.getItemId()) || !ii.canBeUsedOnItem(equip)) {
            chr.chatMessage("Invalid item to use this item on.");
            chr.dispose();
            return;
        }

        boolean success = true;
        boolean boom = false;
        boolean hadReturnScroll = equip.hasAttribute(EquipAttribute.ReturnScroll);
        Equip prevEquip = hadReturnScroll ? equip.deepCopy() : null;
        Map<ScrollStat, Integer> vals = ii.getScrollStats();
        if (vals.size() > 0) {
            boolean isSet12StarForceScroll = scrollID == 2644017;
            boolean recover = vals.getOrDefault(ScrollStat.recover, 0) != 0;
            boolean reset = vals.getOrDefault(ScrollStat.reset, 0) + vals.getOrDefault(ScrollStat.perfectReset, 0) != 0;
            if (isSet12StarForceScroll) {
                boolean untradable = equip.isTradeBlock()
                        || equip.isEquipTradeBlock()
                        || equip.hasAttribute(EquipAttribute.Untradable);
                if (!untradable) {
                    chr.chatMessage("This scroll can only be used on untradable equipment.");
                    chr.dispose();
                    return;
                }
                if (equip.getBaseStat(tuc) > 0) {
                    chr.chatMessage("This scroll can only be used on equipment with no remaining upgrades.");
                    chr.dispose();
                    return;
                }
                if (equip.getInfo().isSuperiorEqp()) {
                    chr.chatMessage("This scroll cannot be used on superior equipment.");
                    chr.dispose();
                    return;
                }
                if (equip.getChuc() > 12) {
                    chr.chatMessage("This item already has more than 12 Star Force.");
                    chr.dispose();
                    return;
                }
                if (GameConstants.getMaxStars(equip) < 12) {
                    chr.chatMessage("This item cannot reach 12 Star Force.");
                    chr.dispose();
                    return;
                }
            } else if (equip.getBaseStat(tuc) <= 0 && (!recover && !reset)) {
                chr.chatMessage("This item cannot be scrolled.");
                chr.dispose();
                return;
            }
            boolean useTuc = !recover && !reset && !isSet12StarForceScroll;
            int successChance = vals.getOrDefault(ScrollStat.success, 100)
                    + chr.getAvatarData().getCharacterStat().getExtraScrollChance(equip);
            int curse = vals.getOrDefault(ScrollStat.cursed, 0);

            success = chr.isAdminInvincible() || Util.succeedProp(successChance);
            if (success) {
                if (isSet12StarForceScroll) {
                    equip.setChuc((short) 12, false);
                } else {
                    boolean chaos = vals.containsKey(ScrollStat.randStat);
                    if (chaos) {
                        boolean noNegative = vals.containsKey(ScrollStat.noNegative);
                        Map<EquipBaseStat, Integer> chaosRolls = new java.util.EnumMap<>(EquipBaseStat.class);
                        boolean hasChaosTarget;
                        boolean hasNonZeroChange;
                        do {
                            chaosRolls.clear();
                            hasChaosTarget = false;
                            hasNonZeroChange = false;
                            for (EquipBaseStat ebs : ScrollStat.getRandStats()) {
                                int cur = (int) equip.getBaseStat(ebs);
                                if (cur == 0) {
                                    continue;
                                }
                                hasChaosTarget = true;
                                int randStat = rollChaosStat(scrollID, noNegative, ebs, chr.isAdminInvincible());
                                chaosRolls.put(ebs, randStat);
                                if (randStat != 0) {
                                    hasNonZeroChange = true;
                                }
                            }
                        } while (hasChaosTarget && !hasNonZeroChange);

                        for (Map.Entry<EquipBaseStat, Integer> entry : chaosRolls.entrySet()) {
                            int randStat = entry.getValue();
                            equip.addStat(entry.getKey(), randStat);
                        }
                    }
                    if (recover) {
                        Equip fullTucEquip = ItemData.getEquipDeepCopy(equip.getItemId(), false);
                        int maxTuc = fullTucEquip.getTuc() + equip.getIuc();
                        if (equip.getTuc() + equip.getCuc() >= maxTuc) {
                            chr.chatMessage("This item has no open slots to recover.");
                        } else {
                            equip.addStat(tuc, 1);
                        }
                    } else if (reset) {
                        equip.resetStats();

                        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                            int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                            otherEquip.resetStats();
                        }

                    } else {
                        for (Map.Entry<ScrollStat, Integer> entry : vals.entrySet()) {
                            ScrollStat ss = entry.getKey();
                            int val = entry.getValue();
                            if (ss.getEquipStat() != null) {
                                equip.addStat(ss.getEquipStat(), val);
                            }
                        }
                        applyCustomXScrollStats(equip, scrollID, chr.isAdminInvincible());
                    }
                }
                if (useTuc) {
                    equip.addStat(tuc, -1);
                    equip.addStat(cuc, 1);
                }

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    chr.write(ZeroPool.egoEquipComplete(true, true));
                }
            } else {
                if (curse > 0) {
                    boom = Util.succeedProp(curse);
                    if (boom && !equip.hasAttribute(EquipAttribute.ProtectionScroll)) {
                        chr.consumeItem(equip);
                    } else {
                        boom = false;
                    }
                }
                if (useTuc && !equip.hasAttribute(EquipAttribute.UpgradeCountProtection)) {
                    equip.addStat(tuc, -1);
                }

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    chr.write(ZeroPool.egoEquipComplete(false, true));
                }
            }
            equip.removeAttribute(EquipAttribute.ProtectionScroll);
            equip.removeAttribute(EquipAttribute.LuckyDay);
            if (hadReturnScroll) {
                equip.removeAttribute(EquipAttribute.ReturnScroll);
            }
            if (useTuc) {
                equip.removeAttribute(EquipAttribute.UpgradeCountProtection);
            }
            c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), boom));
            if (!boom) {
                equip.recalcEnchantmentStats();

                Equip otherEquip = null;
                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                    otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                    otherEquip.copySecondaryStatsFrom(equip);
                }

                if (otherEquip != null && hadReturnScroll) {
                    otherEquip.removeAttribute(EquipAttribute.ReturnScroll);
                }

                equip.updateToChar(chr);
                if (otherEquip != null) {
                    otherEquip.updateToChar(chr);
                }
                if (hadReturnScroll) {
                    ItemHandlerModule.startPostScrollHelperScript(chr, equip, prevEquip, otherEquip, success);
                }
            }
            chr.consumeItem(scroll);
        } else {
            chr.chatMessage("Could not find scroll data.");
            chr.dispose();
        }
    }

    @Handler(op = InHeader.USER_ITEM_OPTION_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserItemOptionUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        byte bEnchantSkill = inPacket.decodeByte(); // bool or byte?
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            chr.dispose();
            return;
        } else if (!ItemConstants.canEquipHavePotential(equip)) {
            chr.getOffenseManager().addOffense(String.format("Character %d tried to add potential an ineligible item (id %d)", chr.getId(), equip.getItemId()));
            chr.dispose();
            return;
        }
        int scrollID = scroll.getItemId();
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
        int chance = vals.getOrDefault(ScrollStat.success, 100);
        int curse = vals.getOrDefault(ScrollStat.cursed, 0);
        boolean success = chr.isAdminInvincible() || Util.succeedProp(chance);
        if (success) {
            short val;
            int thirdLineChance = ItemConstants.THIRD_LINE_CHANCE;
            switch (scrollID / 10) {
                case 204940: // Rare Pot
                case 204941:
                case 204942:
                case 204943:
                case 204944:
                case 204945:
                case 204946:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204970: // Epic pot
                case 204971:
                    val = ItemGrade.HiddenEpic.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204974: // Unique Pot
                case 204975:
                case 204976:
                case 204979:
                    val = ItemGrade.HiddenUnique.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204978: // Legendary Pot
                    val = ItemGrade.HiddenLegendary.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;

                default:
                    chr.chatMessage(Mob, "Unhandled scroll " + scrollID);
                    chr.dispose();
                    log.error("Unhandled scroll " + scrollID);
                    return;
            }

            if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                otherEquip.copyItemOptionsFrom(equip);
                otherEquip.updateToChar(chr);

                chr.write(ZeroPool.egoEquipComplete(true, true));
            }
        }

        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), false));
        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_ADDITIONAL_SLOT_EXTEND_ITEM_USE_REQUEST)
    public static void handleUserAdditionalSlotExtendItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Item equipItem = chr.getInventoryByType(invType).getItemBySlot(ePos);

        if (item == null || equipItem == null) {
            chr.chatMessage("Could not find either the use item or the equip.");
            return;
        }

        int itemID = item.getItemId();
        Equip equip = (Equip) equipItem;

        if (((Equip) equipItem).getOptionBonus(2) != 0) {
            chr.chatMessage("This item already has 3 bonus potentials.");
            chr.dispose();
            return;
        }

        int successChance = ItemData.getItemInfoByID(itemID).getScrollStats().getOrDefault(ScrollStat.success, 100);
        boolean success = chr.isAdminInvincible() || Util.succeedProp(successChance);
        if (success) {
            switch (itemID / 100) {
                case 20483: // Equipment bonus stamps
                    equip.setOption(2, equip.getRandomOption(true, 2), true);
                    break;
                default:
                    log.error("Unhandled slot extend item " + itemID);
                    chr.chatMessage("Unhandled slot extend item " + itemID);
                    return;
            }
            equip.updateToChar(chr);

            if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                otherEquip.copyItemOptionsFrom(equip);
                otherEquip.updateToChar(chr);

                chr.write(ZeroPool.egoEquipComplete(true, true));
            }
        }
        chr.consumeItem(item);
        chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, itemID, equip.getItemId(), false));
    }

    @Handler(op = InHeader.USER_ITEM_SLOT_EXTEND_ITEM_USE_REQUEST)
    public static void handleUserItemSlotExtendItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Item equipItem = chr.getInventoryByType(invType).getItemBySlot(ePos);

        if (item == null || equipItem == null) {
            chr.chatMessage("Could not find either the use item or the equip.");
            chr.dispose();
            return;
        }

        int itemID = item.getItemId();
        Equip equip = (Equip) equipItem;

        if (((Equip) equipItem).getOptionBase(2) != 0) {
            chr.chatMessage("This item already has 3 potentials.");
            chr.dispose();
            return;
        }

        int successChance = ItemData.getItemInfoByID(itemID).getScrollStats().getOrDefault(ScrollStat.success, 100);
        boolean success = chr.isAdminInvincible() || Util.succeedProp(successChance);
        if (success) {
            switch (itemID / 100) {
                case 20495: // Equipment stamps
                    equip.setOption(2, equip.getRandomOption(false, 2), false);
                    break;
                default:
                    log.error("Unhandled slot extend item " + itemID);
                    chr.chatMessage("Unhandled slot extend item " + itemID);
                    return;
            }
            equip.updateToChar(chr);

            if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                otherEquip.copyItemOptionsFrom(equip);
                otherEquip.updateToChar(chr);

                chr.write(ZeroPool.egoEquipComplete(true, true));
            }
        }
        chr.consumeItem(item);
        chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, itemID, equip.getItemId(), false));
    }

    @Handler(op = InHeader.USER_ADDITIONAL_OPT_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserAdditionalOptUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to use bonus potential in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        byte bEnchantSkill = inPacket.decodeByte();
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            return;
        }
        int scrollID = scroll.getItemId();
        boolean success;
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
        int chance = vals.getOrDefault(ScrollStat.success, 100);
        int curse = vals.getOrDefault(ScrollStat.cursed, 0);
        success = chr.isAdminInvincible() || Util.succeedProp(chance);
        if (success) {
            short val;
            int thirdLineChance = ItemConstants.THIRD_LINE_CHANCE;
            switch (scrollID) {
                case 2048305: // Bonus Pot
                case 2048308:
                case 2048309:
                case 2048310:
                case 2048311:
                case 2048313:
                case 2048314:
                case 2048316:
                case 2048329:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBonus(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(true);
                    }
                    break;
                case 2048306: // Special Bonus Pot
                case 2048307:
                case 2048315:
                case 2048331:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBonus(val, 100);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(true);
                    }
                    break;
                default:
                    chr.chatMessage(Mob, "Unhandled scroll " + scrollID);
                    break;
            }

            if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                otherEquip.copyItemOptionsFrom(equip);
                otherEquip.updateToChar(chr);

                chr.write(ZeroPool.egoEquipComplete(true, true));
            }
        }
        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), false));
        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_ITEM_RELEASE_REQUEST)
    public static void handleUserItemReleaseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos); // old system with magnifying glasses
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (equip == null) {
            chr.chatMessage(SystemNotice, "Could not find equip.");
            return;
        }
        boolean base = equip.getOptionBase(0) < 0;
        boolean bonus = equip.getOptionBonus(0) < 0;
        if (base && bonus) {
            equip.releaseOptions(true);
            equip.releaseOptions(false);
        } else {
            equip.releaseOptions(bonus);
        }

        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
            int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
            otherEquip.copyItemOptionsFrom(equip);
            otherEquip.updateToChar(chr);

            chr.write(ZeroPool.egoEquipComplete(true, true));
        }

        c.write(FieldPacket.showItemReleaseEffect(chr.getId(), ePos, bonus));
        if (invType.equals(EQUIPPED)) {
            chr.getInventoryByType(invType).recalcBaseStats(chr);
        }
        equip.updateToChar(chr);
    }
}
