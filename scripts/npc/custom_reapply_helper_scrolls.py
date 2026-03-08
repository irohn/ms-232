# Gets called after spell tracing to reapply Safety/Lucky day scrolls
# Binding "equip" has the current equip
from net.swordie.ms.client.character.items import EquipAttribute

if "oldEquip" in locals() and oldEquip is not None and oldEquip.hasAttribute(EquipAttribute.ReturnScroll):
    stat_getters = [
        ("STR", oldEquip.getiStr, equip.getiStr, True),
        ("DEX", oldEquip.getiDex, equip.getiDex, True),
        ("INT", oldEquip.getiInt, equip.getiInt, True),
        ("LUK", oldEquip.getiLuk, equip.getiLuk, True),
        ("MaxHP", oldEquip.getiMaxHp, equip.getiMaxHp, True),
        ("MaxMP", oldEquip.getiMaxMp, equip.getiMaxMp, True),
        ("ATT", oldEquip.getiPad, equip.getiPad, True),
        ("MATT", oldEquip.getiMad, equip.getiMad, True),
        ("DEF", oldEquip.getiPDD, equip.getiPDD, True),
        ("MDEF", oldEquip.getiMDD, equip.getiMDD, True),
        ("ACC", oldEquip.getiAcc, equip.getiAcc, True),
        ("EVA", oldEquip.getiEva, equip.getiEva, True),
        ("Speed", oldEquip.getiSpeed, equip.getiSpeed, True),
        ("Jump", oldEquip.getiJump, equip.getiJump, True),
    ]

    changes = []
    for label, old_getter, new_getter, show_delta in stat_getters:
        old_val = old_getter()
        new_val = new_getter()
        if old_val != new_val:
            if show_delta:
                delta = new_val - old_val
                changes.append("#b{}#k: #g{:+d}#k (#r{}#k -> #g{}#k)".format(label, delta, old_val, new_val))
            else:
                changes.append("#b{}#k: #r{}#k -> #g{}#k".format(label, old_val, new_val))

    change_text = "\r\n".join(changes) if changes else "No visible stat change was detected."

    oldEquip.removeAttribute(EquipAttribute.ReturnScroll)
    equip.removeAttribute(EquipAttribute.ReturnScroll)
    prompt = "A #bReturn Scroll#k was applied to this item.\r\n\r\n{}\r\n\r\nDo you want to keep the new scroll result?\r\n#rChoose No to restore the previous state.#k".format(change_text)
    if not sm.sendAskYesNo(prompt):
        equip.copyScrollStatsFrom(oldEquip)
        equip.copyAttributesFrom(oldEquip)
        if "otherEquip" in locals() and otherEquip is not None:
            otherEquip.copyScrollStatsFrom(equip)
            otherEquip.copyAttributesFrom(equip)
            otherEquip.updateToChar(chr)

LUCKY_DAY = 2530000

quant = sm.getQuantityOfItem(LUCKY_DAY)
if quant > 0 and not equip.hasAttribute(EquipAttribute.LuckyDay) and sm.sendAskYesNo("Do you want to reapply a #i{}##t{}#? (#b#c{}##k left)".format(LUCKY_DAY, LUCKY_DAY, LUCKY_DAY)):
    if quant > 0:
        equip.addAttribute(EquipAttribute.LuckyDay)
        sm.consumeItem(LUCKY_DAY)
    else:
        sm.sendNext("You don't have any.")

SAFETY_SCROLL_1 = 5064100  # Cash item from the CS
SAFETY_SCROLL_2 = 2532000  # Use item from Ari

chosenScroll = SAFETY_SCROLL_1 if sm.getQuantityOfItem(SAFETY_SCROLL_1) > sm.getQuantityOfItem(SAFETY_SCROLL_2) else SAFETY_SCROLL_2

quant = sm.getQuantityOfItem(chosenScroll)

if quant > 0 and not equip.hasAttribute(EquipAttribute.UpgradeCountProtection) and sm.sendAskYesNo("Do you want to reapply a #i{}##t{}#? (#b#c{}##k left)".format(chosenScroll, chosenScroll, chosenScroll)):
    if quant > 0:
        equip.addAttribute(EquipAttribute.UpgradeCountProtection)
        sm.consumeItem(chosenScroll)
    else:
        sm.sendNext("You don't have any.")

equip.updateToChar(chr)
