# Maple Admin's Heartfelt Gift
# 2x 3x EXP Coupon
from net.swordie.ms.enums import InvType

box = 2436226
item_id = 2450163
quantity = 2

if sm.hasItem(box) and sm.getEmptyInventorySlots(InvType.CONSUME) >= 1:
    sm.consumeItem(box)
    sm.giveItem(item_id, quantity)
    sm.setSpeakerID(9201238)
    sm.sendSayOkay("Got\r\n#i{0}# #z{0}# x{1}".format(item_id, quantity))
