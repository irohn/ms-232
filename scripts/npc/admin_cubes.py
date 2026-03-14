CUBE_IDS = [
    5062005,
    5062006,
    5062009,
    5062010,
    5062090,
    5062500,
    5062024,
    5062503,
    5062021,
    2711004,
    2711003,
]

menu = "Select a cube to receive #b100x#k:\r\n\r\n"
for i in range(len(CUBE_IDS)):
    item_id = CUBE_IDS[i]
    menu += "#L{}##v{}# #z{}##l\r\n".format(i, item_id, item_id)

selection = sm.sendNext(menu)

if selection < 0 or selection >= len(CUBE_IDS):
    sm.sendSayOkay("Invalid selection.")
else:
    item_id = CUBE_IDS[selection]
    quantity = 100
    if not sm.canHold(item_id, quantity):
        sm.sendSayOkay("Please make room in your inventory first.")
    else:
        sm.giveItem(item_id, quantity)
        sm.sendSayOkay(
            "You received #b{}x#k #v{}# #z{}##.".format(quantity, item_id, item_id)
        )
