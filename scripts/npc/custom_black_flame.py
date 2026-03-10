def format_diff(label, old_val, new_val, percent=False):
    if old_val == new_val:
        return None
    delta = new_val - old_val
    suffix = "%" if percent else ""
    return "#b{}#k: #r{}{}#k -> #g{}{}#k (#d{:+d}{}#k)".format(label, old_val, suffix, new_val, suffix, delta, suffix)


changes = []

stat_rows = [
    ("STR", oldEquip.getfSTR(), equip.getfSTR(), False),
    ("DEX", oldEquip.getfDEX(), equip.getfDEX(), False),
    ("INT", oldEquip.getfINT(), equip.getfINT(), False),
    ("LUK", oldEquip.getfLUK(), equip.getfLUK(), False),
    ("ATT", oldEquip.getfATT(), equip.getfATT(), False),
    ("MATT", oldEquip.getfMATT(), equip.getfMATT(), False),
    ("DEF", oldEquip.getfDEF(), equip.getfDEF(), False),
    ("MaxHP", oldEquip.getfHP(), equip.getfHP(), False),
    ("MaxMP", oldEquip.getfMP(), equip.getfMP(), False),
    ("Speed", oldEquip.getfSpeed(), equip.getfSpeed(), False),
    ("Jump", oldEquip.getfJump(), equip.getfJump(), False),
    ("All Stat", oldEquip.getfAllStat(), equip.getfAllStat(), True),
    ("Boss Damage", oldEquip.getfBoss(), equip.getfBoss(), True),
    ("Damage", oldEquip.getfDamage(), equip.getfDamage(), True),
    ("Level Reduction", oldEquip.getfLevel(), equip.getfLevel(), False),
]

for label, old_val, new_val, percent in stat_rows:
    line = format_diff(label, old_val, new_val, percent)
    if line is not None:
        changes.append(line)

change_text = "\r\n".join(changes) if changes else "No visible flame stat changes were detected."

keep_new = sm.sendAskYesNo(
    "Your #bBlack Flame#k rolled new bonus stats for #i{}##t{}##k.\r\n\r\n{}\r\n\r\n#bYes#k: keep the new flame stats.\r\n#rNo#k: restore the previous flame stats.".format(
        equip.getItemId(),
        equip.getItemId(),
        change_text
    )
)

pending = chr.getPendingFlameInfo()
if pending is not None:
    if not keep_new:
        pending.revert()
    equip.updateToChar(chr)
    if "otherEquip" in locals() and otherEquip is not None:
        otherEquip.updateToChar(chr)
    chr.setPendingFlameInfo(None)

sm.dispose()
